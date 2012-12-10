/**
 * Copyright 2009 The European Bioinformatics Institute, and others.
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

import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.hupo.psi.mi.psicquic.indexing.batch.SolrMitabIndexer;
import org.hupo.psi.mi.psicquic.model.server.SolrJettyRunner;
import org.hupo.psi.mi.psicquic.ws.config.PsicquicConfig;
import org.hupo.psi.mi.psicquic.ws.utils.PsicquicStreamingOutput;
import org.hupo.psi.mi.psicquic.ws.utils.XgmmlStreamingOutput;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import psidev.psi.mi.xml254.jaxb.EntrySet;

import javax.ws.rs.core.GenericEntity;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: SolrBasedPsicquicRestServiceTest.java 17838 2012-01-30 14:23:55Z brunoaranda $
 */
public class SolrBasedPsicquicRestServiceTest {

    private static PsicquicRestService service;

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

        HttpSolrServer solrServer = solrJettyRunner.getSolrServer();
        Assert.assertEquals(4L, solrServer.query(new SolrQuery("*:*")).getResults().getNumFound());

        PsicquicConfig config = (PsicquicConfig)context.getBean("testPsicquicConfig");
        config.setSolrUrl(solrJettyRunner.getSolrUrl());

	    service = (SolrBasedPsicquicRestService) context.getBean("solrBasedPsicquicRestService");
    }

    @After
    public void after() throws Exception {

        solrJettyRunner.stop();
        solrJettyRunner = null;
        service = null;
    }

    @Test
    public void testGetByInteractor() throws Exception {

        // search for idA or idB
        ResponseImpl response = (ResponseImpl) service.getByInteractor("P07228", "uniprotkb", "tab25", "0", "50", "n");

        PsicquicStreamingOutput pso = ((GenericEntity<PsicquicStreamingOutput>) response.getEntity()).getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(1, baos.toString().split("\n").length);

        // search for altidA or altidB
        ResponseImpl response2 = (ResponseImpl) service.getByInteractor("EBI-5606437", "intact", "tab25", "0", "50", "n");

        PsicquicStreamingOutput pso2 = ((GenericEntity<PsicquicStreamingOutput>) response2.getEntity()).getEntity();

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        pso2.write(baos2);

        Assert.assertEquals(1, baos2.toString().split("\n").length);

        // search for aliasA or aliasB
        ResponseImpl response3 = (ResponseImpl) service.getByInteractor("RGD-receptor", "uniprotkb", "tab25", "0", "50", "n");

        PsicquicStreamingOutput pso3 = ((GenericEntity<PsicquicStreamingOutput>) response3.getEntity()).getEntity();

        ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
        pso3.write(baos3);

        Assert.assertEquals(1, baos3.toString().split("\n").length);

        // serch for db only
        ResponseImpl response4 = (ResponseImpl) service.getByInteractor(null, "uniprotkb", "tab25", "0", "50", "n");

        PsicquicStreamingOutput pso4 = ((GenericEntity<PsicquicStreamingOutput>) response4.getEntity()).getEntity();

        ByteArrayOutputStream baos4 = new ByteArrayOutputStream();
        pso4.write(baos4);

        Assert.assertEquals(2, baos4.toString().split("\n").length);

        // serch for id only
        ResponseImpl response5 = (ResponseImpl) service.getByInteractor("RGD-receptor", null, "tab25", "0", "50", "n");

        PsicquicStreamingOutput pso5 = ((GenericEntity<PsicquicStreamingOutput>) response5.getEntity()).getEntity();

        ByteArrayOutputStream baos5 = new ByteArrayOutputStream();
        pso5.write(baos5);

        Assert.assertEquals(1, baos5.toString().split("\n").length);
    }

    @Test
    public void testGetByInteraction() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByInteraction("EBI-5630468", null, "tab25", "0", "50", "n");

        PsicquicStreamingOutput pso = ((GenericEntity<PsicquicStreamingOutput>) response.getEntity()).getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(1, baos.toString().split("\n").length);

        // search for db only
        ResponseImpl response2 = (ResponseImpl) service.getByInteraction(null, "intact", "tab25", "0", "50", "n");

        PsicquicStreamingOutput pso2 = ((GenericEntity<PsicquicStreamingOutput>) response2.getEntity()).getEntity();

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        pso2.write(baos2);

        Assert.assertEquals(2, baos2.toString().split("\n").length);

        // search for db:id only
        ResponseImpl response3 = (ResponseImpl) service.getByInteraction("EBI-5630468", "intact", "tab25", "0", "50", "n");

        PsicquicStreamingOutput pso3 = ((GenericEntity<PsicquicStreamingOutput>) response3.getEntity()).getEntity();

        ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
        pso3.write(baos3);

        Assert.assertEquals(1, baos3.toString().split("\n").length);
    }

    @Test
    public void testGetByQuery_count() throws Exception {
        Long response = ((GenericEntity<Long>) ((ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "count", "0", "200", "n")).getEntity()).getEntity();

        Assert.assertEquals(new Long(2), response);
    }

    @Test
    public void testGetByQuery_tab25() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "tab25", "0", "200", "n");

        PsicquicStreamingOutput pso = ((GenericEntity<PsicquicStreamingOutput>) response.getEntity()).getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(1, baos.toString().split("\n").length);
    }

    @Test
    public void testGetByQuery_tab26() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "tab26", "0", "200", "n");

        PsicquicStreamingOutput pso = ((GenericEntity<PsicquicStreamingOutput>) response.getEntity()).getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(2, baos.toString().split("\n").length);
    }

    @Test
    public void testGetByQuery_tab27() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "tab27", "0", "200", "n");

        PsicquicStreamingOutput pso = ((GenericEntity<PsicquicStreamingOutput>) response.getEntity()).getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(2, baos.toString().split("\n").length);
    }

    @Test
    public void testGetByQuery_xml25() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "xml25", "0", "200", "n");

        psidev.psi.mi.xml254.jaxb.EntrySet entrySet = ((GenericEntity<EntrySet>) response.getEntity()).getEntity();

        Assert.assertNotNull(entrySet);
        Assert.assertEquals(2, entrySet.getEntries().iterator().next().getInteractionList().getInteractions().size());
    }

    @Test
    public void testGetByQuery_xgmml() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "xgmml", "0", "200", "n");

        XgmmlStreamingOutput pso = ((GenericEntity<XgmmlStreamingOutput>) response.getEntity()).getEntity();

        Assert.assertNotNull(pso);
    }

    @Test
    public void testGetByQuery_biopax() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "biopax", "0", "200", "n");

        InputStream pso = ((GenericEntity<InputStream>) response.getEntity()).getEntity();

        Assert.assertNotNull(pso);

        ResponseImpl response2 = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "biopax-L2", "0", "200", "n");

        InputStream pso2 = ((GenericEntity<InputStream>) response2.getEntity()).getEntity();

        Assert.assertNotNull(pso2);

        ResponseImpl response3 = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "biopax-L3", "0", "200", "n");

        InputStream pso3 = ((GenericEntity<InputStream>) response3.getEntity()).getEntity();

        Assert.assertNotNull(pso3);
    }

    @Test
    public void testGetByQuery_rdf() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "rdf", "0", "200", "n");

        String pso = ((GenericEntity<String>) response.getEntity()).getEntity();

        Assert.assertNotNull(pso);
        Assert.assertEquals("Format not supported: rdf", pso);

        ResponseImpl response2 = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "rdf-xml", "0", "200", "n");

        InputStream pso2 = ((GenericEntity<InputStream>) response2.getEntity()).getEntity();

        Assert.assertNotNull(pso2);

        ResponseImpl response3 = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "rdf-xml-abbrev", "0", "200", "n");

        InputStream pso3 = ((GenericEntity<InputStream>) response3.getEntity()).getEntity();

        Assert.assertNotNull(pso3);

        ResponseImpl response4 = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "rdf-n3", "0", "200", "n");

        InputStream pso4 = ((GenericEntity<InputStream>) response4.getEntity()).getEntity();

        Assert.assertNotNull(pso4);

        ResponseImpl response5 = (ResponseImpl) service.getByQuery("pmethod:\"western blot\" AND negative:(true OR false)", "rdf-turtle", "0", "200", "n");

        InputStream pso5 = ((GenericEntity<InputStream>) response5.getEntity()).getEntity();

        Assert.assertNotNull(pso5);
    }
}
