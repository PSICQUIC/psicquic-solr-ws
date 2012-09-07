#!/bin/bash

if [ $# == 1 ];
then
      SOLR_WORKDIR=$1;
      echo "SOLR working directory (where we have solr-home and the index and solr war file): ${SOLR_WORKDIR}"
      mvn clean -Pstart-jetty-solr jetty:run -Dmaven.test.skip -Dsolr.workdir=${SOLR_WORKDIR}
else
      echo "SOLR server working directory not provided, use default (current directory)"
      mvn clean -Pstart-jetty-solr jetty:run -Dmaven.test.skip
fi