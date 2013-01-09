package org.hupo.psi.mi.psicquic.ws.utils;

import com.google.common.primitives.Ints;
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

    @Override
    public void write(OutputStream outputStream) throws IOException, WebApplicationException {

        int blockSize = Math.min(SolrBasedPsicquicService.BLOCKSIZE_MAX, maxRows);
        int firstResult = first;

        XgmmlStreamingGrapBuilder graphBuilder = null;
        try {
            graphBuilder = new XgmmlStreamingGrapBuilder("PSICQUIC", "Generated from MITAB 2.5", "http://psicquic.googlecode.com");

            graphBuilder.open(outputStream, totalResults);

            do {

                try {
                    PsicquicSearchResults results = psicquicSolrServer.searchWithFilters(query, firstResult, Math.min(blockSize, maxRows+first-firstResult), returnType, queryFilters);
                    InputStream mitabStream = results.getMitab();

                    try {
                        if (mitabStream != null){
                            graphBuilder.writeNodesAndEdgesFromMitab(mitabStream, MitabDocumentDefinitionFactory.mitab27());
                        }
                    }
                    finally {
                        mitabStream.close();
                    }

                    totalResults = Ints.checkedCast(results.getNumberResults());

                    firstResult += blockSize;

                } catch (Exception e) {
                    throw new WebApplicationException(e);
                }

            } while (firstResult < totalResults  && firstResult < maxRows+first);
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
