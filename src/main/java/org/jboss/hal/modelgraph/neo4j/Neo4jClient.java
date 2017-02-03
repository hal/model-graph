package org.jboss.hal.modelgraph.neo4j;

import java.io.IOException;

import com.google.common.net.HostAndPort;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Harald Pehl
 */
public class Neo4jClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jClient.class);

    private final Driver driver;
    private final String webInterface;
    private long nodesCreated;
    private long relationsCreated;

    public Neo4jClient(final HostAndPort hostAndPort, final String username, final String password) throws IOException {
        String uri = "bolt://" + hostAndPort;
        driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
        webInterface = "http://" + hostAndPort.getHost() + ":7474/browser/";
        logger.info("Connected to Neo4j database at {}", hostAndPort);
    }

    public void execute(Cypher cypher) {
        try (Session session = driver.session();
             Transaction tx = session.beginTransaction()) {

            logger.debug("Execute {} using {}", cypher.statement(), cypher.parameters());
            StatementResult result = tx.run(cypher.statement(), cypher.parameters());
            tx.success();
            logger.debug("{} node and {} relations created",
                    result.summary().counters().nodesCreated(),
                    result.summary().counters().relationshipsCreated());
            nodesCreated += result.summary().counters().nodesCreated();
            relationsCreated += result.summary().counters().relationshipsCreated();
        }
    }

    @Override
    public void close() throws Exception {
        logger.debug("Closing connection to Neo4j database");
        driver.close();
    }

    public String getWebInterface() {
        return webInterface;
    }

    public long getNodesCreated() {
        return nodesCreated;
    }

    public long getRelationsCreated() {
        return relationsCreated;
    }
}
