#!/bin/bash

docker build --tag hpehl/model-graph-wildfly:11.0.0.CR1   --build-arg GRAPH_DB=wildfly-11.0.0.CR1.tar.gz   .
docker build --tag hpehl/model-graph-wildfly:10.1.0.Final --build-arg GRAPH_DB=wildfly-10.1.0.Final.tar.gz .
docker build --tag hpehl/model-graph-wildfly:9.0.2.Final  --build-arg GRAPH_DB=wildfly-9.0.2.Final.tar.gz  .
