package org.jboss.hal.modelgraph.neo4j;

import java.io.IOException;

import com.google.common.net.HostAndPort;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.exceptions.DatabaseException;
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

    public Neo4jClient(final HostAndPort hostAndPort, final String username, final String password, final boolean clean)
            throws IOException {
        String uri = "bolt://" + hostAndPort;
        driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
        webInterface = "http://" + hostAndPort.getHost() + ":7474/browser/";
        logger.info("Connected to Neo4j database at {}", hostAndPort);
        setup(clean);
    }

    private void setup(boolean clean) {
        if (clean) {
            try (Session session = driver.session();
                 Transaction tx = session.beginTransaction()) {
                StatementResult result = tx.run("MATCH (n) DETACH DELETE(n)");
                logger.info("Removed {} nodes and {} relations",
                        result.summary().counters().nodesDeleted(),
                        result.summary().counters().relationshipsDeleted());
                tx.success();
            }
            failSafeDrop("DROP INDEX ON :Parameter(name)");
            failSafeDrop("DROP INDEX ON :Operation(name)");
            failSafeDrop("DROP INDEX ON :Capability(name)");
            failSafeDrop("DROP INDEX ON :Resource(name)");
            failSafeDrop("DROP CONSTRAINT ON (r:Resource) ASSERT r.address IS UNIQUE");
            failSafeDrop("DROP INDEX ON :Attribute(name)");
        }
        try (Session session = driver.session();
             Transaction tx = session.beginTransaction()) {
            tx.run("CREATE INDEX ON :Resource(name)");
            tx.run("CREATE CONSTRAINT ON (r:Resource) ASSERT r.address IS UNIQUE");
            tx.run("CREATE INDEX ON :Attribute(name)");
            tx.run("CREATE INDEX ON :Capability(name)");
            tx.run("CREATE INDEX ON :Operation(name)");
            tx.run("CREATE INDEX ON :Parameter(name)");
            tx.success();
        }
    }

    private void failSafeDrop(String statement) {
        try (Session session = driver.session();
             Transaction tx = session.beginTransaction()) {
            tx.run(statement);
            tx.success();
        } catch (DatabaseException e) {
            logger.warn(e.getMessage());
        }
    }

    public StatementResult execute(Cypher cypher) {
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
            return result;
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
