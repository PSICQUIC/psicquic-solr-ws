package org.hupo.psi.mi.psicquic.ws.indexing;

import org.hupo.psi.mi.psicquic.indexing.batch.SolrMitabIndexer;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Mitab indexer
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>04/09/12</pre>
 */

public class MitabIndexer {

    public static void main(String [] args) throws JobInstanceAlreadyCompleteException, JobParametersInvalidException, NoSuchJobExecutionException, NoSuchJobException, JobRestartException, NoSuchJobInstanceException, JobExecutionAlreadyRunningException {
        // loads the spring context defining beans and jobs
        ApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] {"classpath*:/META-INF/psicquic-spring.xml", "classpath*:/META-INF/jobs/psicquic-indexing-spring.xml"});

        SolrMitabIndexer rm = (SolrMitabIndexer)
                context.getBean("solrMitabIndexer");

        if ( args.length == 1){
            rm.setIndexingId(args[0]);
            rm.resumeIndexing();
        }
        else {
            rm.startIndexing();
        }
    }
}
