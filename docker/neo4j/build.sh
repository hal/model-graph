#!/bin/bash

docker build --tag hpehl/model-graph-wildfly:12.0.0.Final --build-arg GRAPH_DB=wildfly-12.0.0.Final.tar.gz --build-arg PLAY_URL=https://hal.github.io/model-graph/model-graph-12.html .
docker build --tag hpehl/model-graph-wildfly:11.0.0.Final --build-arg GRAPH_DB=wildfly-11.0.0.Final.tar.gz --build-arg PLAY_URL=https://hal.github.io/model-graph/model-graph-11.html .
docker build --tag hpehl/model-graph-wildfly:10.1.0.Final --build-arg GRAPH_DB=wildfly-10.1.0.Final.tar.gz --build-arg PLAY_URL=https://hal.github.io/model-graph/model-graph-10.html .
