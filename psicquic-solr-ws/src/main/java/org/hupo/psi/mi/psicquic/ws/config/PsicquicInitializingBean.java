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
package org.hupo.psi.mi.psicquic.ws.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Psicquic Initializing Bean.
 *
 * @author MarineDumousseau (marine@ebi.ac.uk)
 * @version $Id: PsicquicInitializingBean.java 17796 2012-01-24 17:55:09Z brunoaranda $
 */
@Controller
public class PsicquicInitializingBean implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(PsicquicInitializingBean.class);

    private static final String STATS_DIR_ENV = "psicquic.stats.dir";

    @Autowired
    private PsicquicConfig config;

    //@Autowired
    //private StatsConsumer statsConsumer;

    public void afterPropertiesSet() throws Exception {
        // proxy set
        if (config.getProxyHost() != null && config.getProxyHost().length() > 0) {
            if (logger.isInfoEnabled()) logger.info("Using proxy host: "+config.getProxyHost());
            System.setProperty("http.proxyHost", config.getProxyHost());
        }
        if (config.getProxyPort() != null && config.getProxyPort().length() > 0) {
            if (logger.isInfoEnabled()) logger.info("Using proxy port: "+config.getProxyPort());
            System.setProperty("http.proxyPort", config.getProxyPort());
        }

        // stats consumer
        logger.info("Initializing consumer");
        //statsConsumer.start();
    }
}
