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
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.hupo.psi.mi.psicquic.*;
import org.hupo.psi.mi.psicquic.model.PsicquicSearchResults;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrException;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrServer;
import org.hupo.psi.mi.psicquic.ws.config.PsicquicConfig;
import org.hupo.psi.mi.psicquic.ws.utils.StreamingQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.calimocho.solr.converter.SolrFieldName;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.xml.converter.ConverterException;

import java.io.IOException;
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
    public static int maxTotalConnections = 128;
    public static int defaultMaxConnectionsPerHost = 24;
    public static int connectionTimeOut = 20000;
    public static int soTimeOut = 20000;
    public static boolean allowCompression = true;

    public static final int BLOCKSIZE_MAX = 500;

    public static final List<String> SUPPORTED_SOAP_RETURN_TYPES = Arrays.asList(PsicquicSolrServer.RETURN_TYPE_XML25,
            PsicquicSolrServer.RETURN_TYPE_MITAB25, PsicquicSolrServer.RETURN_TYPE_MITAB26, PsicquicSolrServer.RETURN_TYPE_MITAB27, PsicquicSolrServer.RETURN_TYPE_COUNT);


    @Autowired
    private PsicquicConfig config;

    private PsicquicSolrServer psicquicSolrServer;

    public SolrBasedPsicquicService() {
    }

    public PsicquicSolrServer getPsicquicSolrServer() {
        if (psicquicSolrServer == null) {
            HttpSolrServer solrServer = new HttpSolrServer(config.getSolrUrl(), createHttpClient());

            solrServer.setConnectionTimeout(connectionTimeOut);
            solrServer.setSoTimeout(soTimeOut);
            solrServer.setAllowCompression(allowCompression);

            psicquicSolrServer = new PsicquicSolrServer(solrServer);
        }

        return psicquicSolrServer;

    }

    private void logQueryIfConfigured(String query){
        org.apache.log4j.Logger logger = this.config.getQueryLogger();

        if (logger != null && query != null){
            logger.info("query: " + query);
        }
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

        if (id != null){
            return "("+((db == null || db.length() == 0)? "\""+id+"\"" : "\""+db+":"+id+"\"")+")";
        }
        else {
            return "("+db+")";
        }
    }

    public QueryResponse getByQuery(String query, RequestInfo requestInfo) throws NotSupportedMethodException, NotSupportedTypeException, PsicquicServiceException {

        final String resultType = requestInfo.getResultType();

        if (resultType != null && !getSupportedReturnTypes().contains(resultType)) {
            throw new NotSupportedTypeException("Not supported return type: "+resultType+" - Supported types are: "+getSupportedReturnTypes());
        }

        logger.debug("Searching: {} ({}/{})", new Object[] {query, requestInfo.getFirstResult(), requestInfo.getBlockSize()});
        logQueryIfConfigured(query);

        // preparing the response
        QueryResponse queryResponse = null;
        try {
            queryResponse = executeQuery(query, requestInfo);
        } catch (PsicquicSolrException e) {
            throw new PsicquicServiceException("Problem executing the query " + query, e);
        } catch (SolrServerException e) {
            throw new PsicquicServiceException("Problem executing the query" + query, e);
        } catch (XmlConversionException e) {
            throw new PsicquicServiceException("Problem converting XML for the query " +query, e);
        } catch (IllegalAccessException e) {
            throw new PsicquicServiceException("Problem retrieving results in PSICQUIC for the query "  +query, e);
        } catch (ConverterException e) {
            throw new PsicquicServiceException("Problem retrieving results in PSICQUIC for the query "  +query, e);
        } catch (IOException e) {
            throw new PsicquicServiceException("Problem retrieving results in PSICQUIC for the query "  +query, e);
        }

        return queryResponse;
    }

    private QueryResponse executeQuery(String query, RequestInfo requestInfo) throws SolrServerException, PsicquicServiceException, NotSupportedTypeException, PsicquicSolrException, ConverterException, IOException, XmlConversionException, IllegalAccessException {
        PsicquicSolrServer solrServer = getPsicquicSolrServer();

        String resultType = requestInfo.getResultType() != null ? requestInfo.getResultType() : PsicquicSolrServer.RETURN_TYPE_DEFAULT;
        int maxResults = requestInfo.getBlockSize();

        // in case of xml, we cannot give more than 500 results
        if (resultType.equals(PsicquicSolrServer.RETURN_TYPE_XML25) && maxResults > BLOCKSIZE_MAX){

            // check that total number of results is less than 500, otherwise throw an Exception
            PsicquicSearchResults results = psicquicSolrServer.searchWithFilters(query, 0, 0, PsicquicSolrServer.RETURN_TYPE_COUNT, new String[]{config.getQueryFilter()});

            long total = results.getNumberResults();
            // check that remaining number of results is less than 500. If not, throw an exception
            if (total - requestInfo.getFirstResult() > BLOCKSIZE_MAX){
                PsicquicFault fault = new PsicquicFault();
                fault.setCode(400);
                fault.setMessage("Too many results to export in one single XML Entry.");
                throw new PsicquicServiceException("Too many results to return in XML. Please use a more specific search or reduce the RequestInfo.blockSize to 500 and use pagination.", fault);
            }
        }

        StreamingQueryResponse psicquicStreaming = new StreamingQueryResponse(solrServer, query, requestInfo.getFirstResult(), maxResults, resultType, new String[]{config.getQueryFilter()});

        return psicquicStreaming.executeStreamingQuery(requestInfo);
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

    private HttpClient createHttpClient() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory
                .getSocketFactory()));
        schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory
                .getSocketFactory()));

        PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);
        cm.setMaxTotal(maxTotalConnections);
        cm.setDefaultMaxPerRoute(defaultMaxConnectionsPerHost);

        HttpClient httpClient = new DefaultHttpClient(cm);

        String proxyHost = config.getProxyHost();
        String proxyPort = config.getProxyPort();

        if (isValueSet(proxyHost) && proxyHost.trim().length() > 0 &&
                isValueSet(proxyPort) && proxyPort.trim().length() > 0) {
            try{
                HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
                httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }
            catch (Exception e){
                logger.error("Impossible to create proxy host:"+proxyHost+", port:"+proxyPort,e);
            }
        }

        return httpClient;
    }

    private boolean isValueSet(String value) {
        return value != null && !value.startsWith("$");
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

