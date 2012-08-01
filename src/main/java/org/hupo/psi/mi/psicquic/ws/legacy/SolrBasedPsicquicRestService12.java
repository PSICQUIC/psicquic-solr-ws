package org.hupo.psi.mi.psicquic.ws.legacy;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.hupo.psi.mi.psicquic.NotSupportedMethodException;
import org.hupo.psi.mi.psicquic.NotSupportedTypeException;
import org.hupo.psi.mi.psicquic.PsicquicServiceException;
import org.hupo.psi.mi.psicquic.model.PsicquicSearchResults;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrException;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrServer;
import org.hupo.psi.mi.psicquic.ws.SolrBasedPsicquicRestService;
import org.hupo.psi.mi.psicquic.ws.SolrBasedPsicquicService;
import org.hupo.psi.mi.psicquic.ws.utils.PsicquicConverterUtils;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml254.jaxb.EntrySet;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * rest service for the version 1.2 of REST which was based on LUCENE
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15/06/12</pre>
 */

public class SolrBasedPsicquicRestService12 extends SolrBasedPsicquicRestService {

    public static final List<String> SUPPORTED_REST_RETURN_TYPES = Arrays.asList(
            RETURN_TYPE_XML25,
            RETURN_TYPE_MITAB25,
            RETURN_TYPE_BIOPAX,
            RETURN_TYPE_XGMML,
            RETURN_TYPE_RDF_XML,
            RETURN_TYPE_RDF_XML_ABBREV,
            RETURN_TYPE_RDF_N3,
            RETURN_TYPE_RDF_TURTLE,
            RETURN_TYPE_COUNT);

    public SolrBasedPsicquicRestService12() {
    }

    @Override
    public Object getByQuery(String query, String format,
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
                    psicquicResults = psicquicSolrServer.search(query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_XML25, config.getQueryFilter());
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

                return entrySet;
            } else if ((format.toLowerCase().startsWith("rdf") && format.length() > 5) || format.toLowerCase().startsWith("biopax")
                    || format.toLowerCase().startsWith("biopax-L3") || format.toLowerCase().startsWith("biopax-L2")) {
                PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, firstResult, maxResults, format, config.getQueryFilter());

                InputStream rdf = psicquicResults.createRDFOrBiopax(format);
                String mediaType = (format.contains("xml") || format.toLowerCase().startsWith("biopax"))? MediaType.APPLICATION_XML : MediaType.TEXT_PLAIN;

                return prepareResponse(Response.status(200).type(mediaType), rdf, psicquicResults.getNumberResults(), isCompressed).build();

            } else {
                long count = 0;

                if (RETURN_TYPE_COUNT.equalsIgnoreCase(format)) {
                    PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, 0, 0, PsicquicSolrServer.RETURN_TYPE_COUNT, config.getQueryFilter());

                    return psicquicResults.getNumberResults();
                } else if (RETURN_TYPE_XGMML.equalsIgnoreCase(format)) {
                    PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, firstResult, Math.min(maxResults, MAX_XGMML_INTERACTIONS), PsicquicSolrServer.RETURN_TYPE_MITAB25, config.getQueryFilter());

                    count = psicquicResults.getNumberResults();

                    InputStream xgmml = psicquicResults.createXGMML();
                    Response resp = prepareResponse(Response.status(200).type("application/xgmml"),
                            xgmml, count, isCompressed)
                            .build();

                    return resp;
                } else if (RETURN_TYPE_MITAB25.equalsIgnoreCase(format) || format == null) {
                    PsicquicSearchResults psicquicResults = psicquicSolrServer.search(query, firstResult, maxResults, PsicquicSolrServer.RETURN_TYPE_MITAB25, config.getQueryFilter());

                    InputStream mitab = psicquicResults.getMitab();
                    if (mitab != null){
                        return prepareResponse(Response.status(200).type(MediaType.TEXT_PLAIN), mitab,
                                psicquicResults.getNumberResults(), isCompressed).build();
                    }
                    else {
                        return prepareResponse(Response.status(200).type(MediaType.TEXT_PLAIN), "",
                                psicquicResults.getNumberResults(), isCompressed).build();
                    }
                } else {
                    return formatNotSupportedResponse(format);
                }
            }
        } catch (Throwable e) {
            throw new PsicquicServiceException("Problem creating output", e);
        }


    }

    @Override
    public Object getSupportedFormats() throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        return Response.status(200)
                .type(MediaType.TEXT_PLAIN)
                .entity(StringUtils.join(SUPPORTED_REST_RETURN_TYPES, "\n")).build();
    }
}
