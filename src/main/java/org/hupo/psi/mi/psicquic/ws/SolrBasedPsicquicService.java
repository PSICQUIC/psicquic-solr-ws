/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hupo.psi.mi.psicquic.ws;

import org.apache.cxf.feature.Features;
import org.apache.lucene.search.BooleanQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.hupo.psi.mi.psicquic.*;
import org.hupo.psi.mi.psicquic.ws.config.PsicquicConfig;
import org.hupo.psi.mi.psicquic.ws.model.PsicquicSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.calimocho.solr.converter.SolrFieldName;

import java.util.*;

/**
 * This web service is based on a PSIMITAB lucene's directory to search and return the results.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: IndexBasedPsicquicService.java 1048 2012-06-01 14:54:23Z mdumousseau@yahoo.com $
 */
@Controller
@Features(features = { "org.apache.cxf.transport.common.gzip.GZIPFeature" })
public class SolrBasedPsicquicService implements PsicquicService {

    private final Logger logger = LoggerFactory.getLogger(SolrBasedPsicquicService.class);

    // settings SOLRServer 
    private static int maxTotalConnections = 128;
    private static int defaultMaxConnectionsPerHost = 32;
    private static int connectionTimeOut = 100000;
    private static int soTimeOut = 100000;
    private static boolean allowCompression = true;

    public static final String RETURN_TYPE_XML25 = "psi-mi/xml25";
    public static final String RETURN_TYPE_MITAB25 = "psi-mi/tab25";
    public static final String RETURN_TYPE_MITAB26 = "psi-mi/tab26";
    public static final String RETURN_TYPE_MITAB27 = "psi-mi/tab27";
    public static final String RETURN_TYPE_COUNT = "count";

    public static final String NEW_LINE = System.getProperty("line.separator");

    public static final int BLOCKSIZE_MAX = 200;
    private static final String RETURN_TYPE_DEFAULT = RETURN_TYPE_MITAB25;

    public static final List<String> SUPPORTED_SOAP_RETURN_TYPES = Arrays.asList(RETURN_TYPE_XML25,
            RETURN_TYPE_MITAB25, RETURN_TYPE_MITAB26, RETURN_TYPE_MITAB27, RETURN_TYPE_COUNT);


    @Autowired
    private PsicquicConfig config;

    private PsicquicSolrServer psicquicSolrServer;

    public SolrBasedPsicquicService() {
        BooleanQuery.setMaxClauseCount(200*1000);
    }

    public PsicquicSolrServer getPsicquicSolrServer() {
        if (psicquicSolrServer == null) {
            HttpSolrServer solrServer = new HttpSolrServer(config.getSolrUrl());

            solrServer.setMaxTotalConnections(maxTotalConnections);
            solrServer.setDefaultMaxConnectionsPerHost(defaultMaxConnectionsPerHost);
            solrServer.setConnectionTimeout(connectionTimeOut);
            solrServer.setSoTimeout(soTimeOut);
            solrServer.setAllowCompression(allowCompression);

            psicquicSolrServer = new PsicquicSolrServer(solrServer);
        }

        return psicquicSolrServer;

    }

    public QueryResponse getByInteractor(DbRef dbRef, RequestInfo requestInfo) throws NotSupportedMethodException, NotSupportedTypeException, PsicquicServiceException {
        String query = createQuery(SolrFieldName.identifier.toString(), dbRef);

        return getByQuery(query, requestInfo);
    }



    public QueryResponse getByInteraction(DbRef dbRef, RequestInfo requestInfo) throws NotSupportedMethodException, NotSupportedTypeException, PsicquicServiceException {
        String query = createQuery(SolrFieldName.interaction_id.toString(), dbRef);

        return getByQuery(query, requestInfo);
    }

    public QueryResponse getByInteractorList(List<DbRef> dbRefs, RequestInfo requestInfo, String operand) throws NotSupportedMethodException, NotSupportedTypeException, PsicquicServiceException {
        String query = createQuery(SolrFieldName.identifier.toString(), dbRefs, operand);

        return getByQuery(query, requestInfo);
    }

    public QueryResponse getByInteractionList(List<DbRef> dbRefs, RequestInfo requestInfo) throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        String query = createQuery(SolrFieldName.interaction_id.toString(), dbRefs, "OR");

        return getByQuery(query, requestInfo);
    }

    private String createQuery(String fieldName, DbRef dbRef) {
        return createQuery(fieldName, Collections.singleton(dbRef), null);
    }

    private String createQuery(String fieldName, Collection<DbRef> dbRefs, String operand) {
        StringBuilder sb = new StringBuilder(dbRefs.size() * 64);
        sb.append(fieldName).append(":(");

        for (Iterator<DbRef> dbRefIterator = dbRefs.iterator(); dbRefIterator.hasNext();) {
            DbRef dbRef = dbRefIterator.next();

            sb.append(createQuery(dbRef));

            if (dbRefIterator.hasNext()) {
                sb.append(" ").append(operand).append(" ");
            }
        }
        
        sb.append(")");

        return sb.toString();
    }

    private String createQuery(DbRef dbRef) {
        String db = dbRef.getDbAc();
        String id = dbRef.getId();
        
        return "("+((db == null || db.length() == 0)? "\""+id+"\"" : "\""+db+"\" AND \""+id+"\"")+")";
    }

    public QueryResponse getByQuery(String query, RequestInfo requestInfo) throws NotSupportedMethodException, NotSupportedTypeException, PsicquicServiceException {
        final int blockSize = Math.min(requestInfo.getBlockSize(), BLOCKSIZE_MAX);

        // apply any filter
        if (config.getQueryFilter() != null && !config.getQueryFilter().isEmpty()) {
            if ("*".equals(query) || query.trim().isEmpty()) {
                query = config.getQueryFilter();
            } else {
                query = query + " "+config.getQueryFilter();
                query = query.trim();
            }
        }

        final String resultType = requestInfo.getResultType();

        if (resultType != null && !getSupportedReturnTypes().contains(resultType)) {
            throw new NotSupportedTypeException("Not supported return type: "+resultType+" - Supported types are: "+getSupportedReturnTypes());
        }

        logger.debug("Searching: {} ({}/{})", new Object[] {query, requestInfo.getFirstResult(), blockSize});

        PsicquicSolrServer solrServer = getPsicquicSolrServer();


        // preparing the response
        QueryResponse queryResponse = solrServer.search(query, requestInfo.getFirstResult(), blockSize, requestInfo.getResultType());

        return queryResponse;
    }

    public String getVersion() {
        return config.getVersion();
    }

    public List<String> getSupportedReturnTypes()  {
        return SUPPORTED_SOAP_RETURN_TYPES;
    }

    public List<String> getSupportedDbAcs() {
        return Collections.EMPTY_LIST;
    }

    public String getProperty(String propertyName) {
        return config.getProperties().get(propertyName);
    }

    public List<Property> getProperties() {
        List<Property> properties = new ArrayList<Property>();

        for (Map.Entry<String,String> entry : config.getProperties().entrySet()) {
            Property prop = new Property();
            prop.setKey(entry.getKey());
            prop.setValue(entry.getValue());
            properties.add(prop);
        }

        return properties;
    }

    public static int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public static void setMaxTotalConnections(int maxTotalConnections) {
        SolrBasedPsicquicService.maxTotalConnections = maxTotalConnections;
    }

    public static int getDefaultMaxConnectionsPerHost() {
        return defaultMaxConnectionsPerHost;
    }

    public static void setDefaultMaxConnectionsPerHost(int defaultMaxConnectionsPerHost) {
        SolrBasedPsicquicService.defaultMaxConnectionsPerHost = defaultMaxConnectionsPerHost;
    }

    public static int getConnectionTimeOut() {
        return connectionTimeOut;
    }

    public static void setConnectionTimeOut(int connectionTimeOut) {
        SolrBasedPsicquicService.connectionTimeOut = connectionTimeOut;
    }

    public static int getSoTimeOut() {
        return soTimeOut;
    }

    public static void setSoTimeOut(int soTimeOut) {
        SolrBasedPsicquicService.soTimeOut = soTimeOut;
    }

    public static boolean isAllowCompression() {
        return allowCompression;
    }

    public static void setAllowCompression(boolean allowCompression) {
        SolrBasedPsicquicService.allowCompression = allowCompression;
    }
}

