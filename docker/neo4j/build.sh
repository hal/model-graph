#!/bin/bash

docker build --tag hpehl/model-graph-wildfly:11.0.0.Final --build-arg GRAPH_DB=wildfly-11.0.0.Final.tar.gz --build-arg WILDFLY_VERSION=11 .
docker build --tag hpehl/model-graph-wildfly:10.1.0.Final --build-arg GRAPH_DB=wildfly-10.1.0.Final.tar.gz --build-arg WILDFLY_VERSION=10 .
docker build --tag hpehl/model-graph-wildfly:9.0.2.Final  --build-arg GRAPH_DB=wildfly-9.0.2.Final.tar.gz --build-arg WILDFLY_VERSION=9 .
