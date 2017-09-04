#!/bin/bash

ARTIFACTS=/guestAuth/app/rest/builds/project:WF,buildType:WF_Nightly,count:1/artifacts/children/
NEO4J_PORT=17687
REST_ENDPOINT=https://ci.wildfly.org
ROOT=$PWD
VERSION=0.2.0
WILDFLY_PORT=19990
XPATH='string(//file[not(contains(@name, "-src"))]/content/@href)'

log () {
  echo
  echo
  echo
  echo "-------------------------------------------------------------------------------"
  echo "$1"
  echo "-------------------------------------------------------------------------------"
}

log "Download WildFly"
rm -rf target
mkdir -p target
cd target
curl --output wf.xml ${REST_ENDPOINT}${ARTIFACTS}
curl --output wildfly.zip ${REST_ENDPOINT}`xmllint --xpath "${XPATH}" wf.xml`
cd ${ROOT}

log "Build and start WildFly and Neo4j docker images"
mvn docker:build
mvn docker:start

log "Analyse resource tree"
cd ../analyzer
mvn clean package -DskipTests
java -jar target/model-graph-analyzer-${VERSION}.jar -w localhost:${WILDFLY_PORT} -u admin -p admin -n localhost:${NEO4J_PORT} /
cd ${ROOT}
mvn docker:stop

log "Build and push hpehl/model-graph-wildfly:latest"
cd target/data/databases
rm -f ${ROOT}/../docker/neo4j/data/wildfly-latest.tar.gz
tar -czf ${ROOT}/../docker/neo4j/data/wildfly-latest.tar.gz graph.db
cd ${ROOT}/../docker/neo4j
docker build --tag hpehl/model-graph-wildfly --build-arg GRAPH_DB=wildfly-latest.tar.gz .
docker push hpehl/model-graph-wildfly
cd ${ROOT}

log "Done"
