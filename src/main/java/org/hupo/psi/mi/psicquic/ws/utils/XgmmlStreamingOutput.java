package org.hupo.psi.mi.psicquic.ws.utils;

import org.hupo.psi.calimocho.tab.util.MitabDocumentDefinitionFactory;
import org.hupo.psi.calimocho.xgmml.XgmmlStreamingGrapBuilder;
import org.hupo.psi.mi.psicquic.model.PsicquicSearchResults;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrServer;
import org.hupo.psi.mi.psicquic.ws.SolrBasedPsicquicService;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Streaming output for XGMML
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/12</pre>
 */

public class XgmmlStreamingOutput extends PsicquicStreamingOutput {
    private int totalResults = -1;

    public XgmmlStreamingOutput(PsicquicSolrServer psicquicService, String query, int first, int maxRows, String returnType, String[] queryFilters, int total) {
        super(psicquicService, query, first, maxRows, returnType, queryFilters);
        this.totalResults = total;
    }

    public XgmmlStreamingOutput(PsicquicSolrServer psicquicService, String query, int first, int maxRows, String returnType, String[] queryFilter, int total, boolean gzip) {
        super(psicquicService, query, first, maxRows, returnType, queryFilter, gzip);
        this.totalResults = total;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException, WebApplicationException {

        OutputStream os;

        if (isGzip()) {
            os = new GZIPOutputStream(outputStream);
        } else {
            os = outputStream;
        }

        int max = first + this.maxRows;
        int blockSize = Math.min(SolrBasedPsicquicService.BLOCKSIZE_MAX, max);
        int firstResult = first;

        XgmmlStreamingGrapBuilder graphBuilder = null;
        try {
            graphBuilder = new XgmmlStreamingGrapBuilder("PSICQUIC", "Generated from MITAB 2.5", "http://psicquic.googlecode.com");

            graphBuilder.open(os, totalResults);

            do {
                if (totalResults > 0 && max < firstResult+blockSize) {
                    blockSize = max - firstResult;
                }

                try {
                    PsicquicSearchResults results = psicquicSolrServer.searchWithFilters(query, firstResult, blockSize, returnType, queryFilters);
                    InputStream mitabStream = results.getMitab();

                    try {
                        if (mitabStream != null){
                            graphBuilder.writeNodesAndEdgesFromMitab(mitabStream, MitabDocumentDefinitionFactory.mitab25());
                        }
                    }
                    finally {
                        mitabStream.close();
                    }

                    totalResults = (int) results.getNumberResults();

                    firstResult = firstResult + blockSize;

                } catch (Exception e) {
                    throw new WebApplicationException(e);
                }

            } while (firstResult < totalResults && firstResult < max);
        } catch (JAXBException e) {
            throw new WebApplicationException(e);
        } catch (XMLStreamException e) {
            throw new WebApplicationException(e);
        }
        finally {
            if (graphBuilder != null){
                try {
                    graphBuilder.close();
                } catch (XMLStreamException e) {
                    throw new WebApplicationException(e);
                }
            }
        }
    }

}
