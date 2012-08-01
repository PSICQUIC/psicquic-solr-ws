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
package org.hupo.psi.mi.psicquic.ws;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.hupo.psi.mi.psicquic.DbRef;
import org.hupo.psi.mi.psicquic.PsicquicService;
import org.hupo.psi.mi.psicquic.QueryResponse;
import org.hupo.psi.mi.psicquic.RequestInfo;
import org.hupo.psi.mi.psicquic.indexing.batch.SolrMitabIndexer;
import org.hupo.psi.mi.psicquic.model.PsicquicSolrServer;
import org.hupo.psi.mi.psicquic.model.server.SolrJettyRunner;
import org.hupo.psi.mi.psicquic.ws.config.PsicquicConfig;
import org.junit.*;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * IntactPsicquicService Tester.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: SolrBasedPsicquicServiceTest.java 13961 2010-02-01 15:34:42Z brunoaranda $
 */
public class SolrBasedPsicquicServiceTest {

    private static PsicquicService service;

    private static SolrJettyRunner solrJettyRunner;

    @Before
    public void setupSolrPsicquicService() throws Exception {

        // Start a jetty server to host the solr index
        solrJettyRunner = new SolrJettyRunner();
        solrJettyRunner.start();

        // index data to be hosted by PSICQUIC
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"/META-INF/beans.spring.test.xml", "classpath*:/META-INF/psicquic-spring.xml",
        "/META-INF/psicquic-indexing-spring-test.xml"});

        SolrMitabIndexer indexer = (SolrMitabIndexer)context.getBean("solrMitabIndexer");
        indexer.startJob("mitabIndexNegativeJob");

        SolrServer solrServer = solrJettyRunner.getSolrServer();
        Assert.assertEquals(4L, solrServer.query(new SolrQuery("*:*")).getResults().getNumFound());

        PsicquicConfig config = (PsicquicConfig)context.getBean("testPsicquicConfig");
        config.setSolrUrl(solrJettyRunner.getSolrUrl());

	    service = (PsicquicService) context.getBean("solrBasedPsicquicService");
    }

    @After
    public void after() throws Exception {

//        solrJettyRunner.join(); // keep the server running ...

        solrJettyRunner.stop();
        solrJettyRunner = null;
        service = null;
    }

    @Test
    public void testGetByInteractor() throws Exception {
        RequestInfo info = new RequestInfo();
        info.setResultType( PsicquicSolrServer.RETURN_TYPE_MITAB25 );
        info.setBlockSize(50);

        // search for idA or idB
        DbRef dbRef = new DbRef();
        dbRef.setId("P07228");
        dbRef.setDbAc("uniprotkb");

        final QueryResponse response = service.getByInteractor(dbRef, info);

        Assert.assertEquals(2, response.getResultInfo().getTotalResults());
        Assert.assertEquals(2, response.getResultSet().getMitab().split("\n").length);

        // search for altidA or altidB
        DbRef dbRef2 = new DbRef();
        dbRef2.setId("EBI-5606437");
        dbRef2.setDbAc("intact");

        final QueryResponse response2 = service.getByInteractor(dbRef2, info);

        Assert.assertEquals(2, response2.getResultInfo().getTotalResults());
        Assert.assertEquals(2, response2.getResultSet().getMitab().split("\n").length);

        // search for aliasA or aliasB
        DbRef dbRef3 = new DbRef();
        dbRef3.setId("RGD-receptor");
        dbRef3.setDbAc("uniprotkb");

        final QueryResponse response3 = service.getByInteractor(dbRef3, info);

        Assert.assertEquals(2, response3.getResultInfo().getTotalResults());
        Assert.assertEquals(2, response3.getResultSet().getMitab().split("\n").length);

        // serch for db only
        DbRef dbRef4 = new DbRef();
        dbRef4.setDbAc("uniprotkb");

        final QueryResponse response4 = service.getByInteractor(dbRef4, info);

        Assert.assertEquals(4, response4.getResultInfo().getTotalResults());
        Assert.assertEquals(4, response4.getResultSet().getMitab().split("\n").length);

        // serch for id only
        DbRef dbRef5 = new DbRef();
        dbRef5.setId("RGD-receptor");

        final QueryResponse response5 = service.getByInteractor(dbRef5, info);

        Assert.assertEquals(2, response5.getResultInfo().getTotalResults());
        Assert.assertEquals(2, response5.getResultSet().getMitab().split("\n").length);
    }

    /*@Test
    public void testGetByInteraction() throws Exception {
        RequestInfo info = new RequestInfo();
        info.setResultType( IntactPsicquicService.RETURN_TYPE_MITAB25 );
        info.setBlockSize(50);

        DbRef dbRef = new DbRef();
        dbRef.setId("DGI-337977");

        final QueryResponse response = service.getByInteraction(dbRef, info);

        Assert.assertEquals(1, response.getResultInfo().getTotalResults());
    }

    @Test
    public void testGetByInteractorList_operandOR() throws Exception {
        RequestInfo info = new RequestInfo();
        info.setResultType( IntactPsicquicService.RETURN_TYPE_MITAB25 );
        info.setBlockSize(50);

        DbRef dbRef1 = new DbRef();
        dbRef1.setId("MDR1");
        DbRef dbRef2 = new DbRef();
        dbRef2.setId("FMS");

        final QueryResponse response = service.getByInteractorList(Arrays.asList(dbRef1, dbRef2), info, "OR");

        Assert.assertEquals(2, response.getResultInfo().getTotalResults());

    }

    @Test
    public void testGetByInteractorList_operandAND() throws Exception {
        RequestInfo info = new RequestInfo();
        info.setResultType( IntactPsicquicService.RETURN_TYPE_MITAB25 );
        info.setBlockSize(50);

        DbRef dbRef1 = new DbRef();
        dbRef1.setId("Imatinib");
        DbRef dbRef2 = new DbRef();
        dbRef2.setId("FMS");

        final QueryResponse response = service.getByInteractorList(Arrays.asList(dbRef1, dbRef2), info, "AND");

        Assert.assertEquals(1, response.getResultInfo().getTotalResults());

    }

    @Test
    public void testGetByInteractionList() throws Exception {
        RequestInfo info = new RequestInfo();
        info.setResultType( IntactPsicquicService.RETURN_TYPE_MITAB25 );
        info.setBlockSize(50);

        DbRef dbRef1 = new DbRef();
        dbRef1.setId("DGI-337977");
        DbRef dbRef2 = new DbRef();
        dbRef2.setId("DGI-337968");

        final QueryResponse response = service.getByInteractionList(Arrays.asList(dbRef1, dbRef2), info);

        Assert.assertEquals(2, response.getResultInfo().getTotalResults());
    }

    @Test
    public void testGetByQuery() throws Exception {
        RequestInfo info = new RequestInfo();
        info.setResultType( IntactPsicquicService.RETURN_TYPE_MITAB25 );
        info.setBlockSize(50);

        final QueryResponse response = service.getByQuery("imatinib", info);

        Assert.assertEquals(11, response.getResultInfo().getTotalResults());
        Assert.assertEquals(11, response.getResultSet().getMitab().split("\n").length);
    }

    @Test
    public void testGetVersion() {
        Assert.assertEquals("TEST.VERSION", service.getVersion());
    } */
}