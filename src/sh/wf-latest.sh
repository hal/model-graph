#!/bin/sh

# Script to download and start the latest WildFly, process the management model and swap the graph database.

WILDFLY_VERSION=11.0.0.Alpha1-SNAPSHOT
WILDFLY_DB=7476
MODEL_GRAPH_VERSION=0.1.0
MODEL_GRAPH_DB=7474

touch processing.log
rm -rf processing
mkdir processing

log "Download WildFly $WILDFLY_VERSION"
curl https://ci.wildfly.org/guestAuth/app/rest/builds/project:WF,buildType:WF_Nightly,count:1/artifacts/content/wildfly-${WILDFLY_VERSION}.zip -o wildfly.zip
log "DONE"

log "Extract WildFly $WILDFLY_VERSION"
unzip wildfly.zip -d processing
log "DONE"

log "Start WildFly $WILDFLY_VERSION"
processing/wildfly-${WILDFLY_VERSION}/bin/add-user.sh -u admin -p admin --silent
processing/wildfly-${WILDFLY_VERSION}/bin/standalone.sh -c standalone-full-ha.xml
log "DONE"

log "Stop Neo4j wf-latest"
./neo4j-instance.sh stop ${WILDFLY_DB}
log "DONE"

log "Start Neo4j model-graph"
./neo4j-instance.sh start ${MODEL_GRAPH_DB}
sleep 2
log "DONE"

log "Analyse WildFly $WILDFLY_VERSION"
java -jar model-graph-${MODEL_GRAPH_VERSION} -clean
log "DONE"

log "Stop WildFly $WILDFLY_VERSION"
processing/wildfly-${WILDFLY_VERSION}/bin/jboss-cli.sh --connect command=:shutdown
log "DONE"

log "Stop Neo4j model-graph"
./neo4j-instance.sh stop ${MODEL_GRAPH_DB}
log "DONE"

log "Replace Neo4j wf-latest"
rm -rf neo4j-instances/ports/${WILDFLY_DB}/data/databases/graph.db
mv neo4j-instances/ports/${MODEL_GRAPH_DB}/data/databases/graph.db neo4j-instances/ports/${WILDFLY_DB}/data/databases/
mkdir neo4j-instances/ports/${MODEL_GRAPH_DB}/data/databases/graph.db
log "DONE"

log "Start Neo4j wf-latest"
./neo4j-instance.sh start ${WILDFLY_DB}
log "DONE"

log() {
    echo "$(date '+%F %T') $1" >> processing.log
}
