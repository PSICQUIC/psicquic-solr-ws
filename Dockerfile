FROM tomcat:9-jdk11

ADD /war_files/intact-psicquic-ws.war "/usr/local/tomcat/webapps/Tools#webservices#psicquic#intact.war"
ADD /war_files/uniprot-psicquic-ws.war "/usr/local/tomcat/webapps/Tools#webservices#psicquic#uniprot.war"
ADD /war_files/mbinfo-psicquic-ws.war "/usr/local/tomcat/webapps/Tools#webservices#psicquic#mbinfo.war"
ADD /war_files/mpidb-psicquic-ws.war "/usr/local/tomcat/webapps/Tools#webservices#psicquic#mpidb.war"
ADD /war_files/mint-psicquic-ws.war "/usr/local/tomcat/webapps/Tools#webservices#psicquic#mint.war"
ADD /war_files/bhf-ucl-psicquic-ws.war "/usr/local/tomcat/webapps/psicquic#Tools#webservices#bhf-ucl.war"
ADD /war_files/hpidb-psicquic-ws.war "/usr/local/tomcat/webapps/Tools#webservices#psicquic#hpidb.war"
ADD /war_files/imex-psicquic-ws.war "/usr/local/tomcat/webapps/Tools#webservices#psicquic#imex.war"
RUN cp -r webapps.dist/ROOT webapps/
RUN cp -r webapps.dist/manager webapps/

CMD ["catalina.sh", "run"]