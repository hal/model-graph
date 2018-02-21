#!/bin/bash

docker build --tag hpehl/model-graph-wildfly:11.0.0.Final --build-arg GRAPH_DB=wildfly-11.0.0.Final.tar.gz --build-arg PLAY_URL=https://cdn.rawgit.com/hal/model-graph/87fcd023/docker/neo4j/docs/model-graph-11.html .
docker build --tag hpehl/model-graph-wildfly:10.1.0.Final --build-arg GRAPH_DB=wildfly-10.1.0.Final.tar.gz --build-arg PLAY_URL=https://cdn.rawgit.com/hal/model-graph/87fcd023/docker/neo4j/docs/model-graph-10.html .
docker build --tag hpehl/model-graph-wildfly:9.0.2.Final  --build-arg GRAPH_DB=wildfly-9.0.2.Final.tar.gz --build-arg PLAY_URL=https://cdn.rawgit.com/hal/model-graph/87fcd023/docker/neo4j/docs/model-graph-9.html .
