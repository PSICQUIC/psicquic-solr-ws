/**
 * Copyright 2011 The European Bioinformatics Institute, and others.
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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class CompressedStreamingOutput implements StreamingOutput {
    
    private InputStream is;
    
    public CompressedStreamingOutput(InputStream is) {
        this.is = is;
    }

    
    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try{
            final GZIPOutputStream gzipOs = new GZIPOutputStream(baos);

            try{
                IOUtils.copy(is, gzipOs);

                output.write(baos.toByteArray());
            }
            finally {
                gzipOs.close();
            }
        }
        finally {
            baos.close();
            // close inputStream
            is.close();
        }
    }
}
