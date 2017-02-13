#!/bin/bash
#
# neo4j-wf-latest    Neo4j WildFly Latest
#
# chkconfig: 345 70 30
# description: Neo4J database containing the latest WildFly management model
# processname: neo4j-wf-latest

source /etc/init.d/functions

PORT=7476
RETURN_VALUE=0
PROGRAM_NAME="neo4j-wf-latest"
NEO4J_SERVICE=/home/neo4j/neo4j-instances/ports/${PORT}/bin/neo4j
NEO4J_USER=neo4j

start() {
        echo -n "Starting $PROGRAM_NAME: "
        daemon --user ${NEO4J_USER} ${NEO4J_SERVICE} start
        RETURN_VALUE=$?
        return ${RETURN_VALUE}
}

stop() {
        echo -n "Shutting down $PROGRAM_NAME: "
        ${NEO4J_SERVICE} stop
        RETURN_VALUE=$?
        return ${RETURN_VALUE}
}

restart() {
        echo -n "Restarting $PROGRAM_NAME: "
        ${NEO4J_SERVICE} restart
        RETURN_VALUE=$?
        return ${RETURN_VALUE}
}

status() {
        echo -n "Checking $PROGRAM_NAME status: "
        ${NEO4J_SERVICE} status
        RETURN_VALUE=$?
        return ${RETURN_VALUE}
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    status)
        status
        ;;
    restart)
        stop
        start
        ;;
    *)
        echo "Usage: $PROGRAM_NAME {start|stop|status|restart}"
        exit 1
        ;;
esac
exit ${RETURN_VALUE}
