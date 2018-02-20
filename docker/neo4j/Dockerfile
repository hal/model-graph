FROM openjdk:8-jre-alpine

LABEL maintainer="Harald Pehl <harald.pehl@gmail.com>"

RUN apk add --no-cache --quiet \
    bash \
    curl

ENV NEO4J_SHA256=0e5c6492cd274edf06c5f10d2b64711bd559aaff37c646e03bfa65e613994174 \
    NEO4J_TARBALL=neo4j-community-3.3.1-unix.tar.gz \
    NEO4J_EDITION=community \
    NEO4J_browser_post__connect__cmd="play http://localhost:8080/model-graph.html"
ARG GRAPH_DB
ARG NEO4J_URI=http://dist.neo4j.org/neo4j-community-3.3.1-unix.tar.gz

RUN curl --fail --silent --show-error --location --remote-name ${NEO4J_URI} \
    && echo "${NEO4J_SHA256}  ${NEO4J_TARBALL}" | sha256sum -csw - \
    && tar --extract --file ${NEO4J_TARBALL} --directory /var/lib \
    && mv /var/lib/neo4j-* /var/lib/neo4j \
    && rm ${NEO4J_TARBALL} \
    && mv /var/lib/neo4j/data /data \
    && ln -s /data /var/lib/neo4j/data \
    && apk del curl

ENV PATH /var/lib/neo4j/bin:$PATH

WORKDIR /var/lib/neo4j

COPY docker-entrypoint.sh /docker-entrypoint.sh
COPY conf/neo4j.conf /var/lib/neo4j/conf/
ADD data/$GRAPH_DB /data/databases/

RUN adduser -u 1001 -G root -D -g "Neo4J" neo4j \
    && chown neo4j:root /docker-entrypoint.sh \
    && chmod u+rx,g+rx /docker-entrypoint.sh \
    && chown -R neo4j:root /var/lib/neo4j \
    && chmod -R u+w,g+w /var/lib/neo4j \
    && chown -R neo4j:root /data/databases/graph.db \
    && chmod -R u+w,g+w /data/databases/graph.db

USER 1001

EXPOSE 7474 7473 7687

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["neo4j"]
