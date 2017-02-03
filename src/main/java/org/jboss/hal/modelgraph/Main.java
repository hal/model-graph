package org.jboss.hal.modelgraph;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.net.HostAndPort;
import org.jboss.hal.modelgraph.dmr.WildFlyClient;
import org.jboss.hal.modelgraph.neo4j.Neo4jClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Harald Pehl
 */
@SuppressWarnings("unused")
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Parameter(names = "-wildfly",
            converter = HostAndPortConverter.class,
            validateWith = HostAndPortValidator.class,
            description = "WildFly instance as <server>[:<port>] with 9990 as default port. Omit to connect to a local WildFly instance at localhost:9990.")
    private HostAndPort wildFly;

    @Parameter(names = "-wildfly-user", description = "WildFly username")
    private String wildFlyUsername = "admin";

    @Parameter(names = "-wildfly-password", description = "WildFly password")
    private String wildFlyPassword = "admin";

    @Parameter(names = "-neo4j",
            converter = HostAndPortConverter.class,
            validateWith = HostAndPortValidator.class,
            description = "Neo4j database as <server>[:<port>] with 7687 as default port. Omit to connect to a local Neo4j database at localhost:7687.")
    private HostAndPort neo4j;

    @Parameter(names = "-neo4j-user", description = "Neo4j username")
    private String neo4jUsername = "neo4j";

    @Parameter(names = "-neo4j-password", description = "Neo4j password")
    private String neo4jPassword = "neo4j";

    @Parameter(names = "-resource", description = "The root resource to analyse.")
    private String resource = "/";

    @Parameter(names = {"-help", "--help"}, help = true, description = "Shows this help")
    private boolean help;

    public static void main(String[] args) {
        Main main = new Main();
        JCommander jCommander = new JCommander(main, args);

        if (main.help) {
            jCommander.usage();
        } else {
            main.run();
        }
    }

    private void run() {
        try (WildFlyClient wc = new WildFlyClient(safeHostAndPort(wildFly, 9990), wildFlyUsername, wildFlyPassword);
             Neo4jClient nc = new Neo4jClient(safeHostAndPort(neo4j, 7687), neo4jUsername, neo4jPassword)) {

            // start with resource and store metadata into neo4j database
            Analyzer analyzer = new Analyzer(wc, nc);
            analyzer.start(resource);
            finished(analyzer, nc);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private HostAndPort safeHostAndPort(HostAndPort hostAndPort, int defaultPort) {
        HostAndPort safe;
        if (hostAndPort == null) {
            safe = HostAndPort.fromParts("localhost", defaultPort);
        } else if (!hostAndPort.hasPort()) {
            safe = HostAndPort.fromParts(hostAndPort.getHost(), defaultPort);
        } else {
            safe = hostAndPort;
        }
        return safe;
    }


    private void finished(Analyzer analyzer, Neo4jClient nc) {
        logger.info("{} resources successfully processed.", String.format("%,d", analyzer.getSuccessfulResources()));
        logger.info("{} resources could not be processed.", String.format("%,d", analyzer.getFailedResources()));
        logger.info("{} nodes and {} relations have been created.",
                String.format("%,d", nc.getNodesCreated()), String.format("%,d", nc.getRelationsCreated()));
        logger.info("Use the web interface at {} to query the database.", nc.getWebInterface());
    }
}
