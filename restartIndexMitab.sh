#!/bin/bash

if [ $# == 3 ];
then
      MITAB_FILE=$1;
      SOLR_WORKDIR=$3;
      INDEXING_ID=$2;
      echo "MITAB file: ${MITAB_FILE}"
      echo "SOLR server URL: ${SOLR_WORKDIR}"
      echo "Indexing job ID: ${INDEXING_ID}"
      mvn clean -PrestartIndex -DmitabFile=${MITAB_FILE} -Dsolr.workdir=${SOLR_WORKDIR} -Dindexing.id=${INDEXING_ID} -Dmaven.test.skip
elif [ $# == 2 ];
then
      MITAB_FILE=$1;
      INDEXING_ID=$2;
      echo "MITAB file: ${MITAB_FILE}"
      echo "SOLR server working directory not provided, use default (current directory)"
      echo "Indexing job ID: ${INDEXING_ID}"
      mvn clean -PrestartIndex -DmitabFile=${MITAB_FILE} -Dindexing.id=${INDEXING_ID} -Dmaven.test.skip
else
      echo ""
      echo "ERROR: wrong number of parameters ($#)."
      echo "usage: MITAB_FILE INDEXING_ID SOLR_URL"
      echo "usage: MITAB_FILE: the name of the MITAB file to index. Can be 2.5, 2.6 or 2.7"
      echo "usage: SOLR_WORKDIR: the directory where will be solr-home and solr war file. (By default, if nothing is given, it is : currentDirectory)"
      echo ""
      exit 1
fi
