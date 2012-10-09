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

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.hupo.psi.mi.psicquic.NotSupportedMethodException;
import org.hupo.psi.mi.psicquic.NotSupportedTypeException;
import org.hupo.psi.mi.psicquic.PsicquicServiceException;
import org.hupo.psi.mi.psicquic.model.PsicquicSearchResults;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrServer;
import org.hupo.psi.mi.psicquic.ws.config.PsicquicConfig;
import org.hupo.psi.mi.psicquic.ws.utils.CompressedStreamingOutput;
import org.hupo.psi.mi.psicquic.ws.utils.PsicquicConverterUtils;
import org.hupo.psi.mi.psicquic.ws.utils.PsicquicStreamingOutput;
import org.hupo.psi.mi.psicquic.ws.utils.XgmmlStreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.calimocho.solr.converter.SolrFieldName;
import psidev.psi.mi.xml.io.impl.PsimiXmlWriter254;
import psidev.psi.mi.xml254.jaxb.EntrySet;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * This web service is based on a PSIMITAB SOLR index to search and return the results.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: IntactPsicquicService.java 12873 2009-03-18 02:51:31Z baranda $
 */
@Controller
public class SolrBasedPsicquicRestService implements PsicquicRestService {

    private final Logger logger = LoggerFactory.getLogger(SolrBasedPsicquicRestService.class);

    public static final String RETURN_TYPE_XML25 = "xml25";
    public static final String RETURN_TYPE_MITAB25 = "tab25";
    public static final String RETURN_TYPE_MITAB25_BIN = "tab25-bin";
    public static final String RETURN_TYPE_MITAB26 = "tab26";
    public static final String RETURN_TYPE_MITAB26_BIN = "tab26-bin";
    public static final String RETURN_TYPE_MITAB27 = "tab27";
    public static final String RETURN_TYPE_MITAB27_BIN = "tab27-bin";
    public static final String RETURN_TYPE_BIOPAX = "biopax";
    public static final String RETURN_TYPE_BIOPAX_L2 = "biopax-L2";
    public static final String RETURN_TYPE_BIOPAX_L3 = "biopax-L3";
    public static final String RETURN_TYPE_XGMML = "xgmml";
    public static final String RETURN_TYPE_RDF_XML = "rdf-xml";
    public static final String RETURN_TYPE_RDF_XML_ABBREV = "rdf-xml-abbrev";
    public static final String RETURN_TYPE_RDF_N3 = "rdf-n3";
    public static final String RETURN_TYPE_RDF_TURTLE = "rdf-turtle";
    public static final String RETURN_TYPE_COUNT = "count";
    protected static final int MAX_XGMML_INTERACTIONS = 5000;
    protected static final int MAX_INTERACTIONS_PER_QUERY = 500;

    @Autowired
    protected PsicquicConfig config;

    protected PsicquicSolrServer psicquicSolrServer;

    public static final List<String> SUPPORTED_REST_RETURN_TYPES = Arrays.asList(
            RETURN_TYPE_XML25,
            RETURN_TYPE_MITAB25,
			RETURN_TYPE_MITAB25_BIN,
			RETURN_TYPE_MITAB26,
			RETURN_TYPE_MITAB26_BIN,
			RETURN_TYPE_MITAB27,
			RETURN_TYPE_MITAB27_BIN,
			RETURN_TYPE_BIOPAX,
            RETURN_TYPE_XGMML,
            RETURN_TYPE_RDF_XML,
            RETURN_TYPE_RDF_XML_ABBREV,
            RETURN_TYPE_RDF_N3,
            RETURN_TYPE_RDF_TURTLE,
            RETURN_TYPE_COUNT,
            RETURN_TYPE_BIOPAX_L2,
            RETURN_TYPE_BIOPAX_L3);

    public SolrBasedPsicquicRestService() {
    }

    public PsicquicSolrServer getPsicquicSolrServer() {
        if (psicquicSolrServer == null) {
            HttpSolrServer solrServer = new HttpSolrServer(config.getSolrUrl(), createHttpClient());

            solrServer.setConnectionTimeout(SolrBasedPsicquicService.connectionTimeOut);
            solrServer.setSoTimeout(SolrBasedPsicquicService.soTimeOut);
            solrServer.setAllowCompression(SolrBasedPsicquicService.allowCompression);

            psicquicSolrServer = new PsicquicSolrServer(solrServer);
        }

        return psicquicSolrServer;

    }

    protected void logQueryIfConfigured(String query){
        org.apache.log4j.Logger logger = this.config.getQueryLogger();

        if (logger != null && query != null){
            logger.info("query: " + query);
        }
    }
    public Object getByInteractor(String interactorAc, String db, String format, String firstResult, String maxResults, String compressed) throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        String query = SolrFieldName.identifier.toString()+":"+createQueryValue(interactorAc, db);
        return getByQuery(query, format, firstResult, maxResults, compressed);
    }

    public Object getByInteraction(String interactionAc, String db, String format, String firstResult, String maxResults, String compressed) throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        String query = SolrFieldName.interaction_id.toString()+":"+createQueryValue(interactionAc, db);
        return getByQuery(query, format, firstResult, maxResults, compressed);
    }

    public Object getByQuery(String query, String format,
                                                 String firstResultStr,
                                                 String maxResultsStr,
                                                 String compressed) throws PsicquicServiceException,
                                                                 NotSupportedMethodException,
                                                                 NotSupportedTypeException {
        if (query == null) throw new NullPointerException("Null query");

        logQueryIfConfigured(query);

        PsicquicSolrServer psicquicSolrServer = getPsicquicSolrServer();

        boolean isCompressed = ("y".equalsIgnoreCase(compressed) || "true".equalsIgnoreCase(compressed));

        int firstResult;
        int maxResults;

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

        format = format.toLowerCase();

        // if using mitab25-bin, set to mitab and compressed=y
        if (RETURN_TYPE_MITAB25_BIN.equalsIgnoreCase(format)) {
            format = RETURN_TYPE_MITAB25;
            isCompressed = true;
        }
        else if (RETURN_TYPE_MITAB26_BIN.equalsIgnoreCase(format)) {
            format = RETURN_TYPE_MITAB26;
            isCompressed = true;
        }
        else if (RETURN_TYPE_MITAB27_BIN.equalsIgnoreCase(format)) {
            format = RETURN_TYPE_MITAB27;
            isCompressed = true;
        }

        try {
            if (RETURN_TYPE_XML25.equalsIgnoreCase(format)) {
                PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_XML25, config.getQueryFilter());

                final EntrySet entrySet = PsicquicConverterUtils.extractJaxbEntrySetFromPsicquicResults(psicquicResults, query, maxResults);
                long count = psicquicResults.getNumberResults();

                return prepareResponse(Response.status(200).type(MediaType.APPLICATION_XML), entrySet, count, isCompressed).build();
            } else if ((format.toLowerCase().startsWith("rdf") && format.length() > 5) || format.toLowerCase().startsWith("biopax")
                    || format.toLowerCase().startsWith("biopax-L3") || format.toLowerCase().startsWith("biopax-L2")) {
                PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_MITAB25, config.getQueryFilter());

                InputStream rdf = psicquicResults.createRDFOrBiopax(format);
                String mediaType = (format.contains("xml") || format.toLowerCase().startsWith("biopax"))? MediaType.APPLICATION_XML : MediaType.TEXT_PLAIN;

                return prepareResponse(Response.status(200).type(mediaType), rdf, psicquicResults.getNumberResults(), isCompressed).build();

            } else {
                long count = 0;

                if (RETURN_TYPE_COUNT.equalsIgnoreCase(format)) {
                    PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, 0, 0, PsicquicSolrServer.RETURN_TYPE_COUNT, config.getQueryFilter());

                    return psicquicResults.getNumberResults();
                } else if (RETURN_TYPE_XGMML.equalsIgnoreCase(format)) {
                    PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, 0, 0, PsicquicSolrServer.RETURN_TYPE_COUNT, config.getQueryFilter());

                    count = psicquicResults.getNumberResults();

                    String fixedQuery = query;
                    if (fixedQuery.contains("&")){
                        fixedQuery = query.substring(0, query.indexOf("&"));
                    }
                    fixedQuery = fixedQuery.replaceAll("q=", "");
                    fixedQuery = fixedQuery.replaceAll(":","_");
                    fixedQuery = fixedQuery.replaceAll(" ","_");
                    fixedQuery = fixedQuery.replaceAll("\\(","");
                    fixedQuery = fixedQuery.replaceAll("\\)","");

                    String name = fixedQuery.substring(0, Math.min(10, fixedQuery.length())) + ".xgmml";

                    XgmmlStreamingOutput xgmml = new XgmmlStreamingOutput(this.psicquicSolrServer, query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_MITAB27, new String[]{config.getQueryFilter()}, (int) count, isCompressed);

                    Response resp = prepareResponse(Response.status(200).type("application/xgmml").header("Content-Disposition", "attachment; filename="+name),
                            xgmml, count, isCompressed)
                            .build();

                    return resp;
                } else if (RETURN_TYPE_MITAB25.equalsIgnoreCase(format) || format == null) {
                    PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, 0, 0, PsicquicSolrServer.RETURN_TYPE_COUNT, config.getQueryFilter());

                    PsicquicStreamingOutput psicquicStreaming = new PsicquicStreamingOutput(psicquicSolrServer, query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_MITAB25, new String[]{config.getQueryFilter()}, isCompressed);
                    return prepareResponse(Response.status(200).type(MediaType.TEXT_PLAIN), psicquicStreaming,
                           psicquicResults.getNumberResults(), isCompressed).build();
                }
                else if (RETURN_TYPE_MITAB26.equalsIgnoreCase(format)) {
                    PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_COUNT, config.getQueryFilter());

                    PsicquicStreamingOutput psicquicStreaming = new PsicquicStreamingOutput(psicquicSolrServer, query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_MITAB26, new String[]{config.getQueryFilter()}, isCompressed);
                    return prepareResponse(Response.status(200).type(MediaType.TEXT_PLAIN), psicquicStreaming,
                            psicquicResults.getNumberResults(), isCompressed).build();
                }
                else if (RETURN_TYPE_MITAB27.equalsIgnoreCase(format)) {
                    PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, 0, 0, PsicquicSolrServer.RETURN_TYPE_COUNT, config.getQueryFilter());

                    PsicquicStreamingOutput psicquicStreaming = new PsicquicStreamingOutput(psicquicSolrServer, query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_MITAB27, new String[]{config.getQueryFilter()}, isCompressed);
                    return prepareResponse(Response.status(200).type(MediaType.TEXT_PLAIN), psicquicStreaming,
                            psicquicResults.getNumberResults(), isCompressed).build();
                }else {
                    return formatNotSupportedResponse(format);
                }
            }
        } catch (Throwable e) {
            throw new PsicquicServiceException("Problem creating output", e);
        }


    }

    protected Response formatNotSupportedResponse(String format) {
        return Response.status(406).type(MediaType.TEXT_PLAIN).entity("Format not supported: "+format).build();
    }

    protected Response.ResponseBuilder prepareResponse(Response.ResponseBuilder responseBuilder, Object entity, long totalCount, boolean compressed) throws IOException {
        if (compressed) {
            if (entity instanceof InputStream) {
                CompressedStreamingOutput streamingOutput = new CompressedStreamingOutput((InputStream)entity);
                responseBuilder.entity(streamingOutput);
            } else if (entity instanceof String) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(((String)entity).getBytes());
                CompressedStreamingOutput streamingOutput = new CompressedStreamingOutput(inputStream);
                responseBuilder.entity(streamingOutput);
            } else if (entity instanceof EntrySet) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                PsimiXmlWriter254 xmlWriter254 = new PsimiXmlWriter254();
                try {
                    xmlWriter254.marshall((EntrySet)entity, baos);

                    ByteArrayInputStream inputStream = new ByteArrayInputStream(baos.toByteArray());

                    CompressedStreamingOutput streamingOutput = new CompressedStreamingOutput(inputStream);
                    responseBuilder.entity(streamingOutput);
                } catch (Throwable e) {
                    throw new IOException("Problem marshalling XML", e);
                }
                finally {
                    baos.close();
                }

            } else {
                responseBuilder.entity(entity);
            }

            responseBuilder.header("Content-Encoding", "gzip");
        } else {
            responseBuilder.entity(entity);
        }

        prepareHeaders(responseBuilder).header("X-PSICQUIC-Count", String.valueOf(totalCount));
        

        return responseBuilder;
    }
    
    public Response.ResponseBuilder prepareHeaders(Response.ResponseBuilder responseBuilder) {
        responseBuilder.header("X-PSICQUIC-Impl", config.getImplementationName());
        responseBuilder.header("X-PSICQUIC-Impl-Version", config.getVersion());
        responseBuilder.header("X-PSICQUIC-Spec-Version", config.getRestSpecVersion());
        responseBuilder.header("X-PSICQUIC-Supports-Compression", Boolean.TRUE);
        responseBuilder.header("X-PSICQUIC-Supports-Formats", StringUtils.join(SUPPORTED_REST_RETURN_TYPES, ", "));

        return responseBuilder;
    }

    public Object getSupportedFormats() throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        return Response.status(200)
                .type(MediaType.TEXT_PLAIN)
                .entity(StringUtils.join(SUPPORTED_REST_RETURN_TYPES, "\n")).build();
    }

    public Object getProperty(String propertyName) {
        final String val = config.getProperties().get(propertyName);

        if (val == null) {
            return Response.status(404)
                .type(MediaType.TEXT_PLAIN)
                .entity("Property not found: "+propertyName).build();
        }

         return Response.status(200)
                .type(MediaType.TEXT_PLAIN)
                .entity(val).build();
    }

    public Object getProperties() {
        StringBuilder sb = new StringBuilder(256);

        for (Map.Entry entry : config.getProperties().entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }

        return Response.status(200)
                .type(MediaType.TEXT_PLAIN)
                .entity(sb.toString()).build();
    }

    public String getVersion() {
        return config.getVersion();
    }

    private String createQueryValue(String interactorAc, String db) {
        StringBuilder sb = new StringBuilder(256);
        if (db != null && db.length() > 0) {
            sb.append(db);

            if (interactorAc != null && interactorAc.length() > 0){
                sb.append(':').append(interactorAc);
            }
        }
        else {
            sb.append(interactorAc);
        }
        return sb.toString();
    }

    private HttpClient createHttpClient() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory
                .getSocketFactory()));
        schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory
                .getSocketFactory()));

        PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);
        cm.setMaxTotal(SolrBasedPsicquicService.maxTotalConnections);
        cm.setDefaultMaxPerRoute(SolrBasedPsicquicService.defaultMaxConnectionsPerHost);

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

}
