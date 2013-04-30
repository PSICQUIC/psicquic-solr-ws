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
package org.hupo.psi.mi.psicquic.ws;

import org.hupo.psi.mi.psicquic.model.server.SolrHomeBuilder;

import java.io.File;

/**
 * Class to set up solr server
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 */
public class SetupSolrServer {

    public static void main(String[] args) throws Exception {
        System.out.println("////////////////////////////");
        System.out.println("Setting up SOLR server...");

        String solrWorkDirName = "target/solr";

        if (args.length > 0){
            solrWorkDirName = args[0];
        }

        SolrHomeBuilder solrHomeBuilder = new SolrHomeBuilder();

        File solrWorkingDir = new File(solrWorkDirName);

        solrHomeBuilder.install(solrWorkingDir);

        System.out.println("Home: "+solrHomeBuilder.getSolrHomeDir());
        System.out.println("SOLR War: "+solrHomeBuilder.getSolrWar());

        System.out.println("////////////////////////////");

    }

}
