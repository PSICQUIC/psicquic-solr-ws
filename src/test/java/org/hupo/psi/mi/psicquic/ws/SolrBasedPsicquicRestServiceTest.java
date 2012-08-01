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

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: SolrBasedPsicquicRestServiceTest.java 17838 2012-01-30 14:23:55Z brunoaranda $
 */
public class SolrBasedPsicquicRestServiceTest {

    /*private static PsicquicRestService service;

    private static SolrJettyRunner solrJettyRunner;

    @BeforeClass
    public static void setupSolrPsicquicService() throws Exception {

        // Start a jetty server to host the solr index
        solrJettyRunner = new SolrJettyRunner();
        solrJettyRunner.setPort( 19876 );
        solrJettyRunner.start();

        IntactSolrIndexer indexer = new IntactSolrIndexer(solrJettyRunner.getSolrUrl( CoreNames.CORE_PUB ),
                                                          solrJettyRunner.getSolrUrl( CoreNames.CORE_ONTOLOGY_PUB ));
        indexer.indexMitab( SolrBasedPsicquicServiceTest.class.getResourceAsStream("/META-INF/imatinib.mitab.txt"), true );
        indexer.indexMitab( SolrBasedPsicquicServiceTest.class.getResourceAsStream("/META-INF/400.mitab.txt"), true );

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"/META-INF/beans.spring.test.xml"});
        PsicquicConfig config = (PsicquicConfig)context.getBean("testPsicquicConfig");
        config.setSolrServerUrl( solrJettyRunner.getSolrUrl( CoreNames.CORE_PUB ) );

	    service = (IntactPsicquicRestService) context.getBean("intactPsicquicRestService");
    }

    @AfterClass
    public static void after() throws Exception {

//        solrJettyRunner.join(); // keep the server running ...

        solrJettyRunner.stop();
        solrJettyRunner = null;
        service = null;
    }

    @Test
    public void testGetByQuery() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("imatinib", "tab25", "0", "200", "n");

        PsicquicStreamingOutput pso = (PsicquicStreamingOutput) response.getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(11, baos.toString().split("\n").length);
    }

    @Test
    public void testGetByQuery_maxResults() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("imatinib", "tab25", "0", "3", "n");

        PsicquicStreamingOutput pso = (PsicquicStreamingOutput) response.getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(3, baos.toString().split("\n").length);
    }

    @Test
    public void testGetByQuery_maxResults_above200() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("*", "tab25", "0", "305", "n");

        PsicquicStreamingOutput pso = (PsicquicStreamingOutput) response.getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(305, baos.toString().split("\n").length);
    }

    @Test
    public void testGetByQuery_maxResults_above400() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("*", "tab25", "0", "405", "n");

        PsicquicStreamingOutput pso = (PsicquicStreamingOutput) response.getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(405, baos.toString().split("\n").length);
    }

    @Test
    public void testGetByQuery_firstResult_above200() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("*", "tab25", "150", "255", "n");

        PsicquicStreamingOutput pso = (PsicquicStreamingOutput) response.getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(255, baos.toString().split("\n").length);
    }

    @Test
    public void testGetByQuery_firstResult_above200_max() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("*", "tab25", "250", "500", "n");

        PsicquicStreamingOutput pso = (PsicquicStreamingOutput) response.getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(160, baos.toString().split("\n").length);
    }

    @Test
    public void testGetByQuery_maxResults_nolimit() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("imatinib", "tab25", "0", String.valueOf(Integer.MAX_VALUE), "n");

        PsicquicStreamingOutput pso = (PsicquicStreamingOutput) response.getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        Assert.assertEquals(11, baos.toString().split("\n").length);
    }
    
    @Test
    public void testGetByQuery_bin() throws Exception {
        ResponseImpl response = (ResponseImpl) service.getByQuery("imatinib", "tab25-bin", "0", "200", "n");

        PsicquicStreamingOutput pso = (PsicquicStreamingOutput) response.getEntity();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pso.write(baos);

        // gunzip the output
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        GZIPInputStream gzipInputStream = new GZIPInputStream(bais);

        ByteArrayOutputStream mitabOut = new ByteArrayOutputStream();

        byte[] buf = new byte[1024];
        int len;
        while ((len = gzipInputStream.read(buf)) > 0)
            mitabOut.write(buf, 0, len);

        gzipInputStream.close();
        mitabOut.close();

        Assert.assertEquals(11, mitabOut.toString().split("\n").length);
    } */
}
