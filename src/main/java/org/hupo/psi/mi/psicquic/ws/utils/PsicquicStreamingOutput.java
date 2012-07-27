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

import org.hupo.psi.mi.psicquic.model.PsicquicSearchResults;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
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

    private PsicquicSearchResults psicquicSearchResults;
    private boolean gzip;

    public PsicquicStreamingOutput(PsicquicSearchResults psicquicService){
        this(psicquicService, false);
    }

    public PsicquicStreamingOutput(PsicquicSearchResults psicquicService, boolean gzip) {
        this.psicquicSearchResults = psicquicService;
        this.gzip = gzip;

    }

    public long countResults() throws IOException {

        if (this.psicquicSearchResults == null){
           return 0;
        }
        return this.psicquicSearchResults.getNumberResults();
    }

    public void write(OutputStream outputStream) throws IOException, WebApplicationException {
        OutputStream os;

        if (isGzip()) {
            os = new GZIPOutputStream(outputStream);
        } else {
            os = outputStream;
        }

        PrintWriter out = new PrintWriter(os);

        try{
            out.write(psicquicSearchResults.getMitab());

            out.flush();
        }
        finally {
            out.close();
            if (isGzip()){
                os.close();
            }
        }        
    }

    public boolean isGzip() {
        return gzip;
    }

    public void setGzip(boolean gzip) {
        this.gzip = gzip;
    }
}
