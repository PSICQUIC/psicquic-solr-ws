FROM tomcat:9-jdk11

ADD /war_files/intact-psicquic-ws.war "/usr/local/tomcat/webapps/psicquic#intact.war"
ADD /war_files/uniprot-psicquic-ws.war "/usr/local/tomcat/webapps/psicquic#uniprot.war"
ADD /war_files/mbinfo-psicquic-ws.war "/usr/local/tomcat/webapps/psicquic#mbinfo.war"
ADD /war_files/mpidb-psicquic-ws.war "/usr/local/tomcat/webapps/psicquic#mpidb.war"
ADD /war_files/mint-psicquic-ws.war "/usr/local/tomcat/webapps/psicquic#mint.war"
ADD /war_files/bhf-ucl-psicquic-ws.war "/usr/local/tomcat/webapps/psicquic#bhf-ucl.war"
ADD /war_files/hpidb-psicquic-ws.war "/usr/local/tomcat/webapps/psicquic#hpidb.war"
ADD /war_files/imex-psicquic-ws.war "/usr/local/tomcat/webapps/psicquic#imex.war"
RUN cp -r webapps.dist/ROOT webapps/
RUN cp -r webapps.dist/manager webapps/

# COPY custom conf files (server.xml, tomcat-users.xml, etc.)
ADD /config/tomcat-users.xml "/usr/local/tomcat/conf/tomcat-users.xml"
ADD /config/manager-context.xml "/usr/local/tomcat/webapps/manager/META-INF/context.xml"

CMD ["catalina.sh", "run"]