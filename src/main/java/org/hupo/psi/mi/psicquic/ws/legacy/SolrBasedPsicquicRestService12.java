package org.hupo.psi.mi.psicquic.ws.legacy;

import org.apache.commons.lang.StringUtils;
import org.hupo.psi.calimocho.io.DocumentConverter;
import org.hupo.psi.calimocho.model.DocumentDefinition;
import org.hupo.psi.calimocho.tab.util.MitabDocumentDefinitionFactory;
import org.hupo.psi.calimocho.xgmml.XGMMLDocumentDefinition;
import org.hupo.psi.mi.psicquic.NotSupportedMethodException;
import org.hupo.psi.mi.psicquic.NotSupportedTypeException;
import org.hupo.psi.mi.psicquic.PsicquicServiceException;
import org.hupo.psi.mi.psicquic.ws.SolrBasedPsicquicRestService;
import org.hupo.psi.mi.psicquic.ws.utils.PsicquicStreamingOutput;
import org.hupo.psi.mi.rdf.PsimiRdfConverter;
import psidev.psi.mi.xml254.jaxb.EntrySet;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
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
                final EntrySet entrySet = getByQueryXml(query, firstResult, maxResults);
                int count = 0;

                if (entrySet != null && !entrySet.getEntries().isEmpty()){
                    count = entrySet.getEntries().iterator().next().getInteractionList().getInteractions().size();
                }

                return prepareResponse(Response.status(200).type(MediaType.APPLICATION_XML), entrySet, count, isCompressed).build();
            } else if ((format.toLowerCase().startsWith("rdf") && format.length() > 5) || format.toLowerCase().startsWith("biopax")
                    || format.toLowerCase().startsWith("biopax-L3") || format.toLowerCase().startsWith("biopax-L2")) {
                String rdfFormat = getRdfFormatName(format);
                String mediaType = (format.contains("xml") || format.toLowerCase().startsWith("biopax"))? MediaType.APPLICATION_XML : MediaType.TEXT_PLAIN;

                psidev.psi.mi.xml.model.EntrySet entrySet = createEntrySet(query, firstResult, maxResults);
                StringWriter sw = new StringWriter();
                int count = 0;

                // only convert when having some results
                if (entrySet != null && !entrySet.getEntries().isEmpty()){
                    PsimiRdfConverter rdfConverter = new PsimiRdfConverter();
                    try {
                        rdfConverter.convert(entrySet, rdfFormat , sw);
                    } catch (Exception e) {
                        return formatNotSupportedResponse(format);
                    }

                    if (!entrySet.getEntries().isEmpty()){
                        count = entrySet.getEntries().iterator().next().getInteractions().size();
                    }
                }

                Response resp = prepareResponse(Response.status(200).type(mediaType), sw.toString(), count, isCompressed).build();

                // close writer
                sw.close();

                return resp;

            } else {
                int count = count(query);

                if (RETURN_TYPE_COUNT.equalsIgnoreCase(format)) {
                    return count(query);
                } else if (RETURN_TYPE_XGMML.equalsIgnoreCase(format)) {
                    PsicquicStreamingOutput result = new PsicquicStreamingOutput(psicquicService, query, firstResult, Math.min(MAX_XGMML_INTERACTIONS, maxResults), SolrBasedPsicquicRestService.RETURN_TYPE_MITAB25);

                    count = result.countResults();

                    ByteArrayOutputStream mitabOs = new ByteArrayOutputStream();
                    result.write(mitabOs);

                    boolean tooManyResults = false;

                    if (count > MAX_XGMML_INTERACTIONS) {
                        tooManyResults = true;
                    }

                    DocumentDefinition mitabDefinition = MitabDocumentDefinitionFactory.mitab25();
                    DocumentDefinition xgmmlDefinition = new XGMMLDocumentDefinition("PSICQUIC", "Query: "+query+((tooManyResults? " / MORE THAN "+MAX_XGMML_INTERACTIONS+" RESULTS WERE RETURNED. FILE LIMITED TO THE FIRST "+MAX_XGMML_INTERACTIONS : "")), "http://psicquic.googlecode.com");

                    Reader mitabReader = new StringReader(mitabOs.toString());
                    Writer xgmmlWriter = new StringWriter();

                    DocumentConverter converter = new DocumentConverter(mitabDefinition, xgmmlDefinition);
                    converter.convert(mitabReader, xgmmlWriter);

                    // close mitabOs now
                    mitabOs.close();
                    // close mitab reader now
                    mitabReader.close();

                    Response resp = prepareResponse(Response.status(200).type("application/xgmml"),
                            xgmmlWriter.toString(), count, isCompressed)
                            .build();

                    // close stringWriter now
                    xgmmlWriter.close();

                    return resp;
                } else if (RETURN_TYPE_MITAB25.equalsIgnoreCase(format) || format == null) {
                    PsicquicStreamingOutput result = new PsicquicStreamingOutput(psicquicService, query, firstResult, maxResults, isCompressed, SolrBasedPsicquicRestService.RETURN_TYPE_MITAB25);
                    return prepareResponse(Response.status(200).type(MediaType.TEXT_PLAIN), result,
                            result.countResults(), isCompressed).build();
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
