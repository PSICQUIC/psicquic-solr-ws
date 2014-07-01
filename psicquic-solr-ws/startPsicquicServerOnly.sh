#!/bin/bash

if [ $# == 1 ];
then
      SOLR_URL=$1;
      echo "SOLR server url: ${SOLR_URL}"
      mvn clean jetty:run -Dmaven.test.skip -DsolrUrl=${SOLR_URL}
else
      echo "SOLR server url not provided, use default http://localhost:9090/solr"
      mvn clean jetty:run -Dmaven.test.skip
fi
