#!/bin/bash

# Script to download and start the latest WildFly, process the management model and swap the graph database.

MODEL_GRAPH_VERSION=0.2.0
MODEL_GRAPH_DB=7474
PROCESS_LOG=process.log
WILDFLY_ADMIN=9990
WILDFLY_DB=7476
WILDFLY_VERSION=11.0.0.Alpha1-SNAPSHOT
WORK_DIR=/tmp/model-graph

source spinner.sh

log () {
  echo -n "$(date '+%F %T') $1..." >> ${PROCESS_LOG}
  start_spinner "$1..."
}

dne () {
  stop_spinner $?
  echo "DONE" >> ${PROCESS_LOG}
}

wait4wildfly () {
  while [ $(netstat -o -n -a | grep "$1" | grep -c "LISTEN") -eq 0 ]; do
    sleep 0.5
  done
}

wait4neo4j () {
  while [ $(curl --silent --silent --write-out %{http_code} --output /dev/null http://localhost:$1) -ne "200" ]; do
    sleep 0.5
  done
}

log "Prepare processing WildFly $WILDFLY_VERSION"
touch ${PROCESS_LOG}
rm -rf ${WORK_DIR}
mkdir ${WORK_DIR}
dne

log "Download WildFly $WILDFLY_VERSION"
curl --silent --output ${WORK_DIR}/wildfly.zip https://ci.wildfly.org/guestAuth/app/rest/builds/project:WF,buildType:WF_Nightly,count:1/artifacts/content/wildfly-${WILDFLY_VERSION}.zip
dne

log "Extract WildFly $WILDFLY_VERSION"
unzip -q ${WORK_DIR}/wildfly.zip -d ${WORK_DIR}
dne

log "Start WildFly $WILDFLY_VERSION"
${WORK_DIR}/wildfly-${WILDFLY_VERSION}/bin/add-user.sh -u admin -p admin --silent
nohup ${WORK_DIR}/wildfly-${WILDFLY_VERSION}/bin/standalone.sh -c standalone-full-ha.xml &>/dev/null &
wait4wildfly ${WILDFLY_ADMIN}
dne

log "Analyse WildFly $WILDFLY_VERSION"
./neo4j-instance.sh start ${MODEL_GRAPH_DB} > /dev/null 2>&1
wait4neo4j ${MODEL_GRAPH_DB}
java -jar model-graph-${MODEL_GRAPH_VERSION}.jar -clean > /dev/null 2>&1
dne

log "Stop WildFly $WILDFLY_VERSION"
${WORK_DIR}/wildfly-${WILDFLY_VERSION}/bin/jboss-cli.sh --connect command=:shutdown > /dev/null 2>&1
dne

log "Replace Neo4j databases"
./neo4j-instance.sh stop ${MODEL_GRAPH_DB} > /dev/null 2>&1
sleep 2
./neo4j-instance.sh stop ${WILDFLY_DB} > /dev/null 2>&1
sleep 2
rm -rf neo4j-instances/ports/${WILDFLY_DB}/data/databases/graph.db
mv neo4j-instances/ports/${MODEL_GRAPH_DB}/data/databases/graph.db neo4j-instances/ports/${WILDFLY_DB}/data/databases/
mkdir neo4j-instances/ports/${MODEL_GRAPH_DB}/data/databases/graph.db
dne

log "Start Neo4j wf-latest database"
./neo4j-instance.sh start ${WILDFLY_DB} > /dev/null 2>&1
dne
