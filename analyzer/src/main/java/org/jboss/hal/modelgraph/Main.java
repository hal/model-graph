package org.jboss.hal.modelgraph;

import java.io.IOException;
import java.util.Properties;

import com.google.common.net.HostAndPort;
import org.jboss.hal.modelgraph.dmr.WildFlyClient;
import org.jboss.hal.modelgraph.neo4j.Neo4jClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@SuppressWarnings({"FieldCanBeLocal", "unused", "WeakerAccess"})
@Command(name = "analyzer",
        version = {"Analyzer %1$s", "Build %2$s", "%3$s"},
        sortOptions = false,
        descriptionHeading = "%n",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        headerHeading = "%n",
        footerHeading = "%n",
        description = "Reads the management model from a WildFly instance and stores it as a graph in a Neo4j database")
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Option(names = {"-w", "--wildfly"},
            description = "WildFly instance as <server>[:<port>] with 9990 as default port. Omit to connect to a local WildFly instance at localhost:9990.")
    HostAndPort wildFly;

    @Option(names = {"-u", "--wildfly-user"}, description = "WildFly username")
    String wildFlyUsername = "admin";

    @Option(names = {"-p", "--wildfly-password"}, description = "WildFly password")
    String wildFlyPassword = "admin";

    @Option(names = {"-n", "--neo4j"},
            description = "Neo4j database as <server>[:<port>] with 7687 as default port. Omit to connect to a local Neo4j database at localhost:7687.")
    HostAndPort neo4j;

    @Option(names = {"-s", "--neo4j-user"}, description = "Neo4j username")
    String neo4jUsername = "neo4j";

    @Option(names = {"-t", "--neo4j-password"}, description = "Neo4j password")
    String neo4jPassword = "neo4j";

    @Option(names = {"-c", "--clean"},
            description = "remove all indexes, nodes, relationships and properties before analysing the management model tree.")
    boolean clean = false;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version information and exit")
    boolean versionInfoRequested;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message and exit")
    boolean helpRequested;

    @Parameters(description = "the root resource to analyse.")
    String resource = "/";

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        CommandLine commandLine = new CommandLine(main)
                .registerConverter(HostAndPort.class, HostAndPort::fromString);

        try {
            commandLine.parse(args);
            if (main.helpRequested) {
                commandLine.usage(System.err);
                System.exit(2);
            }
            if (main.versionInfoRequested) {
                commandLine.printVersionHelp(System.err, Ansi.AUTO, readVersionInfos());
                System.exit(0);
            }
            main.run();

        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            commandLine.usage(System.err, Ansi.AUTO);
            System.exit(1);
        }
    }

    private static Object[] readVersionInfos() {
        Properties properties = new Properties();
        try {
            properties.load(Main.class.getResourceAsStream("/version.properties"));
        } catch (IOException e) {
            logger.error("Unable to read version infos: " + e.getMessage());
        }
        return new Object[]{
                properties.getProperty("version", "n/a"),
                properties.getProperty("build.date", "n/a"),
                properties.getProperty("build.url", "n/a")
        };
    }

    private void run() {
        try (WildFlyClient wc = new WildFlyClient(safeHostAndPort(wildFly, 9990), wildFlyUsername, wildFlyPassword);
             Neo4jClient nc = new Neo4jClient(safeHostAndPort(neo4j, 7687), neo4jUsername, neo4jPassword, clean)) {

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
        logger.info("{}", analyzer.stats());
        logger.info("Use the web interface at {} to query the database.", nc.getWebInterface());
    }
}
