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

import com.google.common.primitives.Ints;
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

/**
 * Streams results in MITAB format.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class PsicquicStreamingOutput implements StreamingOutput {

    protected PsicquicSolrServer psicquicSolrServer;
    protected String query;
    protected int first;
    protected int maxRows;
    protected String returnType;
    protected String [] queryFilters;

    public PsicquicStreamingOutput(PsicquicSolrServer psicquicService, String query, int first, int maxRows, String returnType, String[] queryFilter) {
        this.psicquicSolrServer = psicquicService;
        this.query = query;
        this.first = first;
        this.maxRows = maxRows;
        this.returnType = returnType;
        this.queryFilters = queryFilter;
    }

    public void write(OutputStream outputStream) throws IOException, WebApplicationException {

        PrintWriter out = new PrintWriter(outputStream);

        int totalResults = -1;
        int blockSize = Math.min(SolrBasedPsicquicService.BLOCKSIZE_MAX, maxRows);
        int firstResult = first;

        do {

            try {
                PsicquicSearchResults results = psicquicSolrServer.searchWithFilters(query, firstResult, blockSize, returnType, queryFilters);
                InputStream mitabStream = results.getMitab();

                try {
                    if (mitabStream != null){
                        IOUtils.copy(mitabStream, out);
                    }
                }
                finally {
                    if (mitabStream != null){
                        mitabStream.close();
                    }
                }

                out.flush();

                totalResults = Ints.checkedCast(results.getNumberResults());

                firstResult += blockSize;

            } catch (Exception e) {
                throw new WebApplicationException(e);
            }

        } while (firstResult < totalResults && firstResult < maxRows);

        out.close();
    }
}
