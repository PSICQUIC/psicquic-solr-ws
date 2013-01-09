package org.hupo.psi.mi.psicquic.ws.utils;

import com.google.common.primitives.Ints;
import org.apache.commons.io.IOUtils;
import org.hupo.psi.mi.psicquic.*;
import org.hupo.psi.mi.psicquic.model.PsicquicSearchResults;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrServer;
import org.hupo.psi.mi.psicquic.ws.SolrBasedPsicquicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import psidev.psi.mi.tab.PsimiTabException;
import psidev.psi.mi.tab.converter.tab2xml.XmlConversionException;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml254.jaxb.EntrySet;

import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * This class will stream results in MITAB and create PSICQUIC queryResponse
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>09/10/12</pre>
 */

public class StreamingQueryResponse {

    private final Logger logger = LoggerFactory.getLogger(StreamingQueryResponse.class);

    protected PsicquicSolrServer psicquicSolrServer;
    protected String query;
    protected int first;
    protected int maxRows;
    protected String returnType;
    protected String [] queryFilters;

    public StreamingQueryResponse(PsicquicSolrServer psicquicService, String query, int first, int maxRows, String returnType, String[] queryFilters){
        this.psicquicSolrServer = psicquicService;
        this.query = query;
        this.first = first;
        this.maxRows = maxRows;
        this.returnType = returnType;
        this.queryFilters = queryFilters;
    }

    public QueryResponse executeStreamingQuery(RequestInfo requestInfo) throws IOException, WebApplicationException, ConverterException, XmlConversionException, NotSupportedTypeException, IllegalAccessException {

        if (requestInfo == null){
            return null;
        }

        StringWriter mitabWriter = new StringWriter();

        int totalResults = -1;
        int blockSize = Math.min(SolrBasedPsicquicService.BLOCKSIZE_MAX, requestInfo.getBlockSize());
        int firstResult = first;

        do {

            try {
                PsicquicSearchResults results = psicquicSolrServer.searchWithFilters(query, firstResult, Math.min(blockSize, maxRows+first-firstResult), returnType, queryFilters);
                InputStream mitabStream = results.getMitab();

                try {
                    if (mitabStream != null){

                        IOUtils.copy(mitabStream, mitabWriter);
                    }
                }
                finally {
                    if (mitabStream != null){
                        mitabStream.close();
                    }
                }

                totalResults = Ints.checkedCast(results.getNumberResults());

                firstResult += blockSize;

            } catch (Exception e) {
                throw new WebApplicationException(e);
            }

        } while (firstResult < totalResults  && firstResult < maxRows+first);

        // preparing the response
        QueryResponse queryResponse = new QueryResponse();
        ResultInfo resultInfo = new ResultInfo();
        resultInfo.setBlockSize(requestInfo.getBlockSize());
        resultInfo.setFirstResult(requestInfo.getFirstResult());
        resultInfo.setResultType(requestInfo.getResultType());
        resultInfo.setTotalResults(totalResults);
        queryResponse.setResultInfo(resultInfo);

        ResultSet resultSet = createResultSet(query, totalResults, requestInfo, mitabWriter);
        queryResponse.setResultSet(resultSet);

        return queryResponse;
    }

    private ResultSet createResultSet(String query, int totalResults, RequestInfo requestInfo, StringWriter results) throws NotSupportedTypeException, ConverterException, IOException, XmlConversionException, IllegalAccessException {
        ResultSet resultSet = new ResultSet();

        String resultType = requestInfo.getResultType() != null ? requestInfo.getResultType() : PsicquicSolrServer.RETURN_TYPE_DEFAULT;

        if (PsicquicSolrServer.RETURN_TYPE_MITAB25.equals(resultType) || PsicquicSolrServer.RETURN_TYPE_MITAB26.equals(resultType) || PsicquicSolrServer.RETURN_TYPE_MITAB27.equals(resultType)) {
            if (logger.isDebugEnabled()) logger.debug("Creating PSI-MI TAB");

            if (results != null){
                String mitab = results.toString();

                resultSet.setMitab(mitab);
            }
        } else if (PsicquicSolrServer.RETURN_TYPE_XML25.equals(resultType)) {
            if (logger.isDebugEnabled()) logger.debug("Creating PSI-MI XML");

            EntrySet jEntrySet = null;
            try {
                jEntrySet = PsicquicConverterUtils.createEntrySetFromInputStream(query, totalResults, results);
            } catch (PsimiTabException e) {
                jEntrySet = new EntrySet();
                logger.error("Impossible to convert to xml", e);
            }
            resultSet.setEntrySet(jEntrySet);

        } else if (PsicquicSolrServer.RETURN_TYPE_COUNT.equals(resultType)) {
            if (logger.isDebugEnabled()) logger.debug("Count query");
            // nothing to be done here
        } else {
            throw new NotSupportedTypeException("Not supported return type: "+resultType+" - Supported types are: "+SolrBasedPsicquicService.SUPPORTED_SOAP_RETURN_TYPES);
        }

        return resultSet;
    }
}
