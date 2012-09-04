#!/bin/bash

if [ $# == 3 ];
then
      MITAB_FILE=$1;
      SOLR_URL=$3;
      INDEXING_ID=$2;
      echo "MITAB file: ${MITAB_FILE}"
      echo "SOLR server URL: ${SOLR_URL}"
      echo "Indexing job ID: ${INDEXING_ID}"
      mvn clean -PrestartIndexWithSolrRunning -DmitabFile=${MITAB_FILE} -DsolrUrl=${SOLR_URL} -Dindexing.id=${INDEXING_ID} -Dmaven.test.skip
elif [ $# == 2 ];
then
      MITAB_FILE=$1;
      INDEXING_ID=$2;
      echo "MITAB file: ${MITAB_FILE}"
      echo "SOLR server URL not provided, use default (http://localhost:9090/solr/)"
      echo "Indexing job ID: ${INDEXING_ID}"
      mvn clean -PrestartIndexWithSolrRunning -DmitabFile=${MITAB_FILE} -DsolrUrl="http://localhost:9090/solr/" -Dindexing.id=${INDEXING_ID} -Dmaven.test.skip
else
      echo ""
      echo "ERROR: wrong number of parameters ($#)."
      echo "usage: MITAB_FILE INDEXING_ID SOLR_URL"
      echo "usage: MITAB_FILE: the name of the MITAB file to index. Can be 2.5, 2.6 or 2.7"
      echo "usage: SOLR_URL: the solr server URL. (By default, if nothing is given, it is : http://localhost:9090/solr/)"
      echo ""
      exit 1
fi
