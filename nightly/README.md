# Nightly

Script to 

- download and run the latest WildFly distribution
- analyse it using [`model-graph-analyzer`](https://github.com/hal/model-graph/blob/master/analyzer/README.md) and
- build and push it as [`hpehl/model-graph-wildfly`](https://hub.docker.com/r/hpehl/model-graph-wildfly/) docker image

## Details

The script `nightly.sh` executes the following steps:

1. Download the latest WildFly distribution from https://ci.wildfly.org/viewType.html?buildTypeId=WF_Nightly
1. Build and start a docker image using the latest WildFly distribution and `standalone.sh -c standalone-full-ha.xml`
1. Build and start a Neo4j docker image
1. Build [`model-graph-analyzer`](https://github.com/hal/model-graph/blob/master/analyzer/README.md)
1. Run it to analyse the complete resource tree of the latest WildFly distribution and store the results in the Neo4j instance
1. Extract the graph database and use it to build the [`hpehl/model-graph-wildfly`](https://hub.docker.com/r/hpehl/model-graph-wildfly/) docker image 
