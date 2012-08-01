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
package org.hupo.psi.mi.psicquic.ws.utils;

import org.apache.commons.io.IOUtils;
import org.hupo.psi.mi.psicquic.model.PsicquicSearchResults;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrServer;
import org.hupo.psi.mi.psicquic.ws.SolrBasedPsicquicService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

/**
 * Streams results in MITAB format.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class PsicquicStreamingOutput implements StreamingOutput {

    private PsicquicSolrServer psicquicSolrServer;
    private boolean gzip;
    private String query;
    private int first;
    private int maxRows;
    private String returnType;
    private String [] queryFilters;

    public PsicquicStreamingOutput(PsicquicSolrServer psicquicService, String query, int first, int maxRows, String returnType, String[] queryFilters){
        this(psicquicService, query, first, maxRows, returnType, queryFilters, false);
    }

    public PsicquicStreamingOutput(PsicquicSolrServer psicquicService, String query, int first, int maxRows, String returnType, String[] queryFilter, boolean gzip) {
        this.psicquicSolrServer = psicquicService;
        this.gzip = gzip;
        this.query = query;
        this.first = first;
        this.maxRows = maxRows;
        this.returnType = returnType;
        this.queryFilters = queryFilter;
    }

    public void write(OutputStream outputStream) throws IOException, WebApplicationException {

        OutputStream os;

        if (isGzip()) {
            os = new GZIPOutputStream(outputStream);
        } else {
            os = outputStream;
        }

        PrintWriter out = new PrintWriter(os);

        int totalResults = -1;
        int max = first + this.maxRows;
        int blockSize = Math.min(SolrBasedPsicquicService.BLOCKSIZE_MAX, max);
        int firstResult = first;

        do {

            if (totalResults > 0 && max < firstResult+blockSize) {
                blockSize = max - firstResult;
            }

            try {
                PsicquicSearchResults results = psicquicSolrServer.searchWithFilters(query, firstResult, blockSize, returnType, queryFilters);
                InputStream mitabStream = results.getMitab();

                try {
                    if (mitabStream != null){
                        IOUtils.copy(results.getMitab(), out);
                    }
                }
                finally {
                    mitabStream.close();
                }

                out.flush();

                totalResults = (int) results.getNumberResults();

                firstResult = firstResult + blockSize;

            } catch (Exception e) {
                throw new WebApplicationException(e);
            }

        } while (firstResult < totalResults && firstResult < max);

        out.close();

        // close the gzip outputStream
        if (isGzip()){
            os.close();
        }        
    }

    public boolean isGzip() {
        return gzip;
    }

    public void setGzip(boolean gzip) {
        this.gzip = gzip;
    }
}
