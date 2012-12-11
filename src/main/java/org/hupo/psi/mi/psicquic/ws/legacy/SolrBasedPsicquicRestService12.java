package org.hupo.psi.mi.psicquic.ws.legacy;

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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.hupo.psi.mi.psicquic.NotSupportedMethodException;
import org.hupo.psi.mi.psicquic.NotSupportedTypeException;
import org.hupo.psi.mi.psicquic.PsicquicServiceException;
import org.hupo.psi.mi.psicquic.model.PsicquicSearchResults;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrException;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrServer;
import org.hupo.psi.mi.psicquic.ws.PsicquicRestService;
import org.hupo.psi.mi.psicquic.ws.SolrBasedPsicquicService;
import org.hupo.psi.mi.psicquic.ws.config.PsicquicConfig;
import org.hupo.psi.mi.psicquic.ws.utils.PsicquicConverterUtils;
import org.hupo.psi.mi.psicquic.ws.utils.PsicquicStreamingOutput;
import org.hupo.psi.mi.psicquic.ws.utils.XgmmlStreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.calimocho.solr.converter.SolrFieldName;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml254.jaxb.EntrySet;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * rest service for the version 1.2 of REST which was based on LUCENE
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15/06/12</pre>
 */
@Controller
public class SolrBasedPsicquicRestService12 implements PsicquicRestService {
    public static final String RETURN_TYPE_MITAB25_BIN = "tab25-bin";

    private final Logger logger = LoggerFactory.getLogger(SolrBasedPsicquicRestService12.class);

    public static final String RETURN_TYPE_XML25 = "xml25";
    public static final String RETURN_TYPE_MITAB25 = "tab25";
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

    protected PsicquicSolrServer psicquicSolrServer;

    @Autowired
    private PsicquicConfig config;

    public static final List<String> SUPPORTED_REST_RETURN_TYPES = Arrays.asList(
            RETURN_TYPE_XML25,
            RETURN_TYPE_MITAB25,
            RETURN_TYPE_BIOPAX,
            RETURN_TYPE_BIOPAX_L2,
            RETURN_TYPE_BIOPAX_L3,
            RETURN_TYPE_XGMML,
            RETURN_TYPE_RDF_XML,
            RETURN_TYPE_RDF_XML_ABBREV,
            RETURN_TYPE_RDF_N3,
            RETURN_TYPE_RDF_TURTLE,
            RETURN_TYPE_MITAB25_BIN,
            RETURN_TYPE_COUNT);

    public SolrBasedPsicquicRestService12() {
        if (psicquicSolrServer == null) {
            HttpSolrServer solrServer = new HttpSolrServer(config.getSolrUrl(), createHttpClient());

            solrServer.setConnectionTimeout(SolrBasedPsicquicService.connectionTimeOut);
            solrServer.setSoTimeout(SolrBasedPsicquicService.soTimeOut);
            solrServer.setAllowCompression(SolrBasedPsicquicService.allowCompression);

            psicquicSolrServer = new PsicquicSolrServer(solrServer);
        }
    }

    public Response getByInteractor(String interactorAc, String db, String format, String firstResult, String maxResults, String compressed) throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        String query = SolrFieldName.identifier.toString()+":"+createQueryValue(interactorAc, db);
        return getByQuery(query, format, firstResult, maxResults, compressed);
    }

    public Response getByInteraction(String interactionAc, String db, String format, String firstResult, String maxResults, String compressed) throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        String query = SolrFieldName.interaction_id.toString()+":"+createQueryValue(interactionAc, db);
        return getByQuery(query, format, firstResult, maxResults, compressed);
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

    @Override
    public Response getByQuery(String query, String format,
                             String firstResultStr,
                             String maxResultsStr,
                             String compressed) throws PsicquicServiceException,
            NotSupportedMethodException,
            NotSupportedTypeException {

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

        try {
            if (RETURN_TYPE_XML25.equalsIgnoreCase(format)) {
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

                return prepareResponse(Response.status(200).type(MediaType.APPLICATION_XML), new GenericEntity<EntrySet>(entrySet){}, psicquicResults.getNumberResults()).build();
            } else if ((format.toLowerCase().startsWith("rdf") && format.length() > 5) || format.toLowerCase().startsWith("biopax")
                    || format.toLowerCase().startsWith("biopax-L3") || format.toLowerCase().startsWith("biopax-L2")) {
                PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, firstResult, Math.min(maxResults, SolrBasedPsicquicService.BLOCKSIZE_MAX), PsicquicSolrServer.RETURN_TYPE_MITAB25, config.getQueryFilter());

                InputStream rdf = psicquicResults.createRDFOrBiopax(format);
                String mediaType = (format.contains("xml") || format.toLowerCase().startsWith("biopax"))? MediaType.APPLICATION_XML : MediaType.TEXT_PLAIN;

                return prepareResponse(Response.status(200).type(mediaType), new GenericEntity<InputStream>(rdf){}, psicquicResults.getNumberResults()).build();

            } else {
                long count = 0;

                if (RETURN_TYPE_COUNT.equalsIgnoreCase(format)) {
                    PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, 0, 0, PsicquicSolrServer.RETURN_TYPE_COUNT, config.getQueryFilter());

                    return prepareResponse(Response.status(200).type(MediaType.TEXT_PLAIN), new GenericEntity<Long>(psicquicResults.getNumberResults()){}, psicquicResults.getNumberResults()).build();
                } else if (RETURN_TYPE_XGMML.equalsIgnoreCase(format)) {
                    PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, firstResult, Math.min(maxResults, MAX_XGMML_INTERACTIONS), PsicquicSolrServer.RETURN_TYPE_MITAB25, config.getQueryFilter());

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

                    XgmmlStreamingOutput xgmml = new XgmmlStreamingOutput(this.psicquicSolrServer, query, firstResult, Math.min(maxResults, MAX_XGMML_INTERACTIONS), PsicquicSolrServer.RETURN_TYPE_MITAB25, new String[]{config.getQueryFilter()}, (int) count);

                    Response resp = prepareResponse(Response.status(200).type(MediaType.APPLICATION_XML).header("Content-Disposition", "attachment; filename="+name),
                            new GenericEntity<XgmmlStreamingOutput>(xgmml){}, count)
                            .build();

                    return resp;
                } else if (RETURN_TYPE_MITAB25.equalsIgnoreCase(format) || format == null) {
                    PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, 0, 0, PsicquicSolrServer.RETURN_TYPE_COUNT, config.getQueryFilter());

                    PsicquicStreamingOutput psicquicStreaming = new PsicquicStreamingOutput(psicquicSolrServer, query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_MITAB25, new String[]{config.getQueryFilter()});
                    return prepareResponse(Response.status(200).type(MediaType.TEXT_PLAIN), new GenericEntity<PsicquicStreamingOutput>(psicquicStreaming){},
                            psicquicResults.getNumberResults()).build();
                } else {
                    return formatNotSupportedResponse(format);
                }
            }
        } catch (Throwable e) {
            throw new PsicquicServiceException("Problem creating output", e);
        }


    }

    protected Response formatNotSupportedResponse(String format) {
        return Response.status(406).type(MediaType.TEXT_PLAIN).entity(new GenericEntity<String>("Format not supported: " + format) {
        }).build();
    }

    protected Response.ResponseBuilder prepareResponse(Response.ResponseBuilder responseBuilder, Object entity, long totalCount) throws IOException {
        responseBuilder.entity(entity);

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

    @Override
    public Object getSupportedFormats() throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        return Response.status(200)
                .type(MediaType.TEXT_PLAIN)
                .entity(new GenericEntity<String>(StringUtils.join(SUPPORTED_REST_RETURN_TYPES, "\n")){}).build();
    }

    public Object getProperty(String propertyName) {
        final String val = config.getProperties().get(propertyName);

        if (val == null) {
            return Response.status(404)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(new GenericEntity<String>("Property not found: " + propertyName) {
                    }).build();
        }

        return Response.status(200)
                .type(MediaType.TEXT_PLAIN)
                .entity(new GenericEntity<String>(val) {
                }).build();
    }

    public Response getProperties() {
        StringBuilder sb = new StringBuilder(256);

        for (Map.Entry entry : config.getProperties().entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }

        return Response.status(200)
                .type(MediaType.TEXT_PLAIN)
                .entity(new GenericEntity<String>(sb.toString()) {
                }).build();
    }

    public String getVersion() {
        return config.getVersion();
    }

    protected HttpClient createHttpClient() {
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
