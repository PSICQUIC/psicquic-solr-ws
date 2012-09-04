package org.hupo.psi.mi.psicquic.ws.indexing;

import org.hupo.psi.mi.psicquic.indexing.batch.SolrMitabIndexer;
import org.hupo.psi.mi.psicquic.model.server.SolrJettyRunner;
import org.hupo.psi.mi.psicquic.ws.SetupSolrServer;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>04/09/12</pre>
 */

public class MitabIndexerAndJettyRunner {

    private String workingDir = "target/solr";

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public static void main(String [] args) throws JobInstanceAlreadyCompleteException, JobParametersInvalidException, NoSuchJobExecutionException, NoSuchJobException, JobRestartException, NoSuchJobInstanceException, JobExecutionAlreadyRunningException {
        // loads the spring context defining beans and jobs
        ApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] {"classpath*:/META-INF/psicquic-spring.xml", "classpath*:/META-INF/jobs/psicquic-indexing-spring.xml", "/META-INF/beans.spring.xml"});

        try {
            MitabIndexerAndJettyRunner runner = (MitabIndexerAndJettyRunner)
                    context.getBean("mitabIndexerAndJettyRunner");

            SetupSolrServer.main(new String[]{runner.getWorkingDir()});

            SolrJettyRunner solrJettyRunner = new SolrJettyRunner(new File(runner.getWorkingDir()));
            solrJettyRunner.start();

            SolrMitabIndexer rm = (SolrMitabIndexer)
                    context.getBean("solrMitabIndexer");

            if ( args.length == 1){
                rm.setIndexingId(args[0]);
                rm.resumeIndexing();
            }
            else {
                rm.startIndexing();
            }

            solrJettyRunner.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
