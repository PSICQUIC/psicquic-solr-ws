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
package org.hupo.psi.mi.psicquic.ws.legacy;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.hupo.psi.mi.psicquic.NotSupportedMethodException;
import org.hupo.psi.mi.psicquic.NotSupportedTypeException;
import org.hupo.psi.mi.psicquic.PsicquicServiceException;
import org.hupo.psi.mi.psicquic.indexing.batch.model.SolrInteraction;
import org.hupo.psi.mi.psicquic.model.PsicquicSearchResults;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrException;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrServer;
import org.hupo.psi.mi.psicquic.ws.SolrBasedPsicquicRestService;
import org.hupo.psi.mi.psicquic.ws.SolrBasedPsicquicService;
import org.hupo.psi.mi.psicquic.ws.config.PsicquicConfig;
import org.hupo.psi.mi.psicquic.ws.utils.CompressedStreamingOutput;
import org.hupo.psi.mi.psicquic.ws.utils.PsicquicConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.data.solr.server.support.MulticoreSolrClientFactory;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.calimocho.solr.converter.SolrFieldName;
import psidev.psi.mi.tab.PsimiTabException;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml254.jaxb.EntrySet;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * This web service is based on a PSIMITAB SOLR index to search and return the results.
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id: IntactPsicquicService.java 12873 2009-03-18 02:51:31Z baranda $
 */
@Controller
public class SolrBasedPsicquicRestService10 implements PsicquicRestService10 {
    private final Logger logger = LoggerFactory.getLogger(SolrBasedPsicquicRestService10.class);

    @Autowired
    private PsicquicConfig config;

    protected PsicquicSolrServer psicquicSolrServer;

    public static final String RETURN_TYPE_MITAB25_BIN = "tab25-bin";

    public PsicquicSolrServer getPsicquicSolrServer() {
        if (psicquicSolrServer == null) {
            if (config.getSolrTemplate() != null) {
                psicquicSolrServer = new PsicquicSolrServer(config.getSolrTemplate(), SolrInteraction.INTERACTIONS_CORE_NAME);
            } else {
                HttpSolrClient solrClient = new HttpSolrClient(config.getSolrUrl(), createHttpClient());
                SolrClientFactory solrClientFactory = new MulticoreSolrClientFactory(solrClient);
                SolrOperations solrTemplate = new SolrTemplate(solrClientFactory);
                psicquicSolrServer = new PsicquicSolrServer(solrTemplate, SolrInteraction.INTERACTIONS_CORE_NAME);
            }
        }

        return psicquicSolrServer;

    }

    protected void logQueryIfConfigured(String query){
        org.apache.log4j.Logger logger = this.config.getQueryLogger();

        if (logger != null && query != null){
            logger.info("query: " + query);
        }
    }

    public Response getByInteractor(String interactorAc, String db, String format, String firstResult, String maxResults) throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        String query = SolrFieldName.id+":"+createQueryValue(interactorAc, db)+ " OR "+SolrFieldName.alias+":"+createQueryValue(interactorAc, db);
        return getByQuery(query, format, firstResult, maxResults);
    }

    public Response getByInteraction(String interactionAc, String db, String format, String firstResult, String maxResults) throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        String query = SolrFieldName.interaction_id+":"+createQueryValue(interactionAc, db);
        return getByQuery(query, format, firstResult, maxResults);
    }

    public Response getByQuery(String query, String format,
                             String firstResultStr,
                             String maxResultsStr) throws PsicquicServiceException,
            NotSupportedMethodException,
            NotSupportedTypeException {
        int firstResult;
        int maxResults;

        logQueryIfConfigured(query);

        PsicquicSolrServer psicquicSolrServer = getPsicquicSolrServer();

        try {
            firstResult =Integer.parseInt(firstResultStr);
        } catch (NumberFormatException e) {
            throw new PsicquicServiceException("firstResult parameter is not a number: "+firstResultStr);
        }

        try {
            if (maxResultsStr == null) {
                maxResults = Integer.MAX_VALUE;
            } else {
                maxResults = Integer.parseInt(maxResultsStr);
            }
        } catch (NumberFormatException e) {
            throw new PsicquicServiceException("maxResults parameter is not a number: "+maxResultsStr);
        }

        if (strippedMime(SolrBasedPsicquicRestService.RETURN_TYPE_XML25).equals(format)) {
            PsicquicSearchResults psicquicResults = null;
            EntrySet entrySet = null;
            try {
                psicquicResults = psicquicSolrServer.search(query, firstResult, Math.min(maxResults, SolrBasedPsicquicService.BLOCKSIZE_MAX), PsicquicSolrServer.RETURN_TYPE_XML25, config.getQueryFilter());
                entrySet = PsicquicConverterUtils.extractJaxbEntrySetFromPsicquicResults(psicquicResults, query, maxResults, SolrBasedPsicquicService.BLOCKSIZE_MAX);
            } catch (PsicquicSolrException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            } catch (SolrServerException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            } catch (XmlConversionException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            } catch (IllegalAccessException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            } catch (ConverterException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            } catch (IOException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            }
            catch (PsimiTabException e){
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            }

            return Response.status(200).type(MediaType.APPLICATION_XML).entity(new GenericEntity<EntrySet>(entrySet) {
            }).build();
        } else if (SolrBasedPsicquicRestService.RETURN_TYPE_COUNT.equals(format)) {
            PsicquicSearchResults psicquicResults = null;
            try {
                psicquicResults = psicquicSolrServer.search(query, 0, 0, PsicquicSolrServer.RETURN_TYPE_COUNT, config.getQueryFilter());
            } catch (PsicquicSolrException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            } catch (SolrServerException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            }

            return Response.status(200).type(MediaType.TEXT_PLAIN).entity(new GenericEntity<Long>(psicquicResults.getNumberResults()){}).build();
        } else if (strippedMime(RETURN_TYPE_MITAB25_BIN).equals(format)) {
            PsicquicSearchResults psicquicResults = null;
            try {
                psicquicResults = psicquicSolrServer.search(query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_MITAB25, config.getQueryFilter());
            } catch (PsicquicSolrException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            } catch (SolrServerException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            }

            InputStream mitab = psicquicResults.getMitab();
            if (mitab != null){
                CompressedStreamingOutput streamingOutput = new CompressedStreamingOutput(mitab);
                return Response.status(200).type("application/x-gzip").entity(new GenericEntity<CompressedStreamingOutput>(streamingOutput) {
                }).build();
            }
            else {
                return Response.status(200).type("application/x-gzip").entity(new GenericEntity<String>("") {
                }).build();
            }
        } else if (strippedMime(SolrBasedPsicquicRestService.RETURN_TYPE_MITAB25).equals(format) || format == null) {
            PsicquicSearchResults psicquicResults = null;
            try {
                psicquicResults = psicquicSolrServer.search(query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_MITAB25, config.getQueryFilter());
            } catch (PsicquicSolrException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            } catch (SolrServerException e) {
                throw new PsicquicServiceException("Problem executing the query " + query, e);
            }

            InputStream mitab = psicquicResults.getMitab();
            if (mitab != null){
                return Response.status(200).type(MediaType.TEXT_PLAIN).entity(new GenericEntity<InputStream>(mitab){}).build();
            }
            else {
                return Response.status(200).type(MediaType.TEXT_PLAIN).entity(new GenericEntity<String>(""){}).build();
            }
        } else {
            return Response.status(406).type(MediaType.TEXT_PLAIN).entity(new GenericEntity<String>("Format not supported") {
            }).build();
        }


    }

    public Response getSupportedFormats() throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        List<String> formats = new ArrayList<String>(SolrBasedPsicquicService.SUPPORTED_SOAP_RETURN_TYPES.size()+1);
        formats.add(strippedMime(RETURN_TYPE_MITAB25_BIN));

        for (String mime : SolrBasedPsicquicService.SUPPORTED_SOAP_RETURN_TYPES) {
            formats.add(strippedMime(mime));
        }

        return Response.status(200)
                .type(MediaType.TEXT_PLAIN)
                .entity(new GenericEntity<String>(StringUtils.join(formats, "\n")){}).build();
    }

    public String getVersion() {
        return config.getVersion();
    }

    private String createQueryValue(String interactorAc, String db) {
        StringBuilder sb = new StringBuilder(256);
        if (db.length() > 0) sb.append('"').append(db).append(':');
        sb.append(interactorAc);
        if (db.length() > 0) sb.append('"');

        return sb.toString();
    }

    private String strippedMime(String mimeType) {
        if (mimeType.indexOf("/") > -1) {
            return mimeType.substring(mimeType.indexOf("/")+1);
        } else {
            return mimeType;
        }
    }

    private HttpClient createHttpClient() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(SolrBasedPsicquicService.maxTotalConnections);
        cm.setDefaultMaxPerRoute(SolrBasedPsicquicService.defaultMaxConnectionsPerHost);

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setConnectionManager(cm);

        String proxyHost = config.getProxyHost();
        String proxyPort = config.getProxyPort();
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder.setConnectTimeout(SolrBasedPsicquicService.connectionTimeOut);
        requestBuilder.setSocketTimeout(SolrBasedPsicquicService.soTimeOut);
        if (isValueSet(proxyHost) && !proxyHost.trim().isEmpty() &&
                isValueSet(proxyPort) && !proxyPort.trim().isEmpty()) {
            try {
                HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
                requestBuilder.setProxy(proxy);
            } catch (Exception e){
                logger.error("Impossible to create proxy host:"+proxyHost+", port:"+proxyPort,e);
            }
        }
        builder.setDefaultRequestConfig(requestBuilder.build());

        return builder.build();
    }
    private boolean isValueSet(String value) {
        return value != null && !value.startsWith("$");
    }
}