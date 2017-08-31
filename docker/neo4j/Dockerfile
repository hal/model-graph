FROM neo4j
ARG GRAPH_DB=wildfly-11.0.0.CR1.tar.gz

LABEL maintainer="Harald Pehl <harald.pehl@gmail.com>"

COPY conf/neo4j.conf /var/lib/neo4j/conf/
ADD data/$GRAPH_DB /data/databases/
