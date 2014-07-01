#!/bin/bash

if [ $# == 2 ];
then
      MITAB_FILE=$1;
      SOLR_WORKDIR=$2;
      echo "MITAB file: ${MITAB_FILE}"
      echo "SOLR server work directory (solr-home and war file): ${SOLR_WORKDIR}"
      mvn clean install -PcreateIndex -DmitabFile=${MITAB_FILE} -Dsolr.workdir=${SOLR_WORKDIR} -Dmaven.test.skip
elif [ $# == 1 ];
then
      MITAB_FILE=$1;
      echo "MITAB file: ${MITAB_FILE}"
      echo "SOLR server work directory not provided, use default (current directory)"
      mvn clean install -PcreateIndex -DmitabFile=${MITAB_FILE} -Dmaven.test.skip
else
      echo ""
      echo "ERROR: wrong number of parameters ($#)."
      echo "usage: MITAB_FILE (SOLR_WORKDIR)"
      echo "usage: MITAB_FILE: the name of the MITAB file to index. Can be 2.5, 2.6 or 2.7"
      echo "usage: SOLR_WORKDIR: the directory where will be solr-home and solr war file. (By default, if nothing is given, it is : currentDirectory)"
      echo ""
      exit 1
fi