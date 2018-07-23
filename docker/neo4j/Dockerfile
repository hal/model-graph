FROM neo4j:latest

LABEL maintainer="Harald Pehl <harald.pehl@gmail.com>"

ARG GRAPH_DB
ARG PLAY_URL

ENV NEO4J_browser_post__connect__cmd="play ${PLAY_URL}"

COPY conf/neo4j.conf /var/lib/neo4j/conf/
ADD data/${GRAPH_DB} /data/databases/
