package org.jboss.hal.modelgraph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.hal.modelgraph.dmr.Operation;
import org.jboss.hal.modelgraph.dmr.ResourceAddress;
import org.jboss.hal.modelgraph.dmr.WildFlyClient;
import org.jboss.hal.modelgraph.neo4j.Cypher;
import org.jboss.hal.modelgraph.neo4j.Neo4jClient;
import org.neo4j.driver.v1.StatementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.jboss.hal.modelgraph.dmr.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
class Analyzer {

    private static final int MAX_DEPTH = 10;
    private static final Logger logger = LoggerFactory.getLogger(Analyzer.class);
    private static final Set<String> GLOBAL_OPERATIONS = Sets.newHashSet(
            // ADD is treated special: Although it's a global operation, each resource has a different add operation
            // with different request parameters
            LIST_ADD,
            LIST_CLEAR,
            LIST_GET,
            LIST_REMOVE,
            MAP_CLEAR,
            MAP_GET,
            MAP_PUT,
            MAP_REMOVE,
            QUERY,
            READ_ATTRIBUTE,
            READ_ATTRIBUTE_GROUP,
            READ_ATTRIBUTE_GROUP_NAMES,
            READ_CHILDREN_NAMES,
            READ_CHILDREN_RESOURCES,
            READ_CHILDREN_TYPES,
            READ_OPERATION_DESCRIPTION,
            READ_OPERATION_NAMES,
            READ_RESOURCE_DESCRIPTION,
            READ_RESOURCE,
            REMOVE,
            UNDEFINE_ATTRIBUTE,
            WHOAMI,
            WRITE_ATTRIBUTE);

    private final WildFlyClient wc;
    private final Neo4jClient nc;
    private final Stats stats;
    private final Set<String> missingGlobalOperations;

    Analyzer(final WildFlyClient wc, final Neo4jClient nc) {
        this.wc = wc;
        this.nc = nc;
        this.stats = new Stats();
        this.missingGlobalOperations = new HashSet<>(GLOBAL_OPERATIONS);
    }

    void start(final String resource) {
        version();
        parse(ResourceAddress.of(resource), null);
    }


    // ------------------------------------------------------ management model

    private void version() {
        Operation operation = new Operation.Builder(READ_RESOURCE, ResourceAddress.of("/"))
                .param(ATTRIBUTES_ONLY, true)
                .build();
        ModelNode modelNode = wc.execute(operation);
        writeVersion(modelNode);
    }

    private void parse(ResourceAddress address, ResourceAddress parent) {
        if (address.size() < MAX_DEPTH) {
            parseResource(address, parent);
            readChildren(address).forEach(child -> parse(address.add(child), address));
        } else {
            logger.warn("Skipping {}. Maximum nesting of {} reached.", address.toString(), MAX_DEPTH);
        }
    }

    private void parseResource(ResourceAddress address, ResourceAddress parent) {
        Operation rrd = new Operation.Builder(READ_RESOURCE_DESCRIPTION, address)
                .param(INCLUDE_ALIASES, true)
                .param(OPERATIONS, true)
                .build();

        ModelNode resourceDescription = wc.execute(rrd);
        if (resourceDescription.isDefined()) {
            logger.info("Read {}", address.toString());

            // for a foo=* address, the result is an array
            if (resourceDescription.getType() == ModelType.LIST) {
                List<ModelNode> descriptions = resourceDescription.asList();
                if (!descriptions.isEmpty() && descriptions.get(0).hasDefined(RESULT)) {
                    resourceDescription = descriptions.get(0).get(RESULT);
                }
            }

            createResource(address);
            if (parent != null) {
                mergeChildOf(address, parent);
            }

            // capabilities
            if (resourceDescription.hasDefined(CAPABILITIES)) {
                resourceDescription.get(CAPABILITIES).asList().stream()
                        .map(modelNode -> modelNode.get(NAME).asString())
                        .forEach(capability -> mergeCapabilities(address, capability));
            }

            // attributes
            if (resourceDescription.hasDefined(ATTRIBUTES)) {
                Multimap<String, String> alternatives = ArrayListMultimap.create();
                Multimap<String, String> requires = ArrayListMultimap.create();
                resourceDescription.get(ATTRIBUTES).asPropertyList().forEach(property -> {
                    String name = property.getName();
                    ModelNode attribute = property.getValue();
                    mergeAttribute(address, name, attribute);

                    // collect alternatives and requires
                    if (attribute.hasDefined(ALTERNATIVES)) {
                        List<String> a = attribute.get(ALTERNATIVES)
                                .asList()
                                .stream()
                                .map(ModelNode::asString)
                                .collect(toList());
                        alternatives.putAll(name, a);
                    }
                    if (attribute.hasDefined(REQUIRES)) {
                        List<String> r = attribute.get(REQUIRES)
                                .asList()
                                .stream()
                                .map(ModelNode::asString)
                                .collect(toList());
                        requires.putAll(name, r);
                    }
                });

                // post process alternatives and requires
                alternatives.entries().forEach(entry ->
                        mergeAttributeRelation(address, entry.getKey(), entry.getValue(), "-[:ALTERNATIVE]-"));
                requires.entries().forEach(entry ->
                        mergeAttributeRelation(address, entry.getKey(), entry.getValue(), "-[:REQUIRES]->"));
            }

            // operations
            if (resourceDescription.hasDefined(OPERATIONS)) {
                resourceDescription.get(OPERATIONS).asPropertyList().forEach(op -> {
                    String name = op.getName();
                    ModelNode operation = op.getValue();
                    boolean globalOperation = GLOBAL_OPERATIONS.contains(name);
                    boolean create = !globalOperation || missingGlobalOperations.contains(name);

                    if (create) {
                        mergeOperation(address, name, operation, globalOperation);
                        if (operation.hasDefined(REQUEST_PROPERTIES)) {
                            Multimap<String, String> alternatives = ArrayListMultimap.create();
                            Multimap<String, String> requires = ArrayListMultimap.create();
                            operation.get(REQUEST_PROPERTIES).asPropertyList().forEach(rp -> {
                                String rpName = rp.getName();
                                ModelNode requestProperty = rp.getValue();
                                mergeRequestProperty(address, name, rpName, requestProperty);

                                // collect alternatives and requires
                                if (requestProperty.hasDefined(ALTERNATIVES)) {
                                    List<String> a = requestProperty.get(ALTERNATIVES)
                                            .asList()
                                            .stream()
                                            .map(ModelNode::asString)
                                            .collect(toList());
                                    alternatives.putAll(rpName, a);
                                }
                                if (requestProperty.hasDefined(REQUIRES)) {
                                    List<String> r = requestProperty.get(REQUIRES)
                                            .asList()
                                            .stream()
                                            .map(ModelNode::asString)
                                            .collect(toList());
                                    requires.putAll(rpName, r);
                                }
                            });

                            // post process alternatives and requires
                            alternatives.entries().forEach(entry ->
                                    mergeRequestPropertyRelation(address, name, entry.getKey(), entry.getValue(),
                                            "-[:ALTERNATIVE]-"));
                            requires.entries().forEach(entry ->
                                    mergeRequestPropertyRelation(address, name, entry.getKey(), entry.getValue(),
                                            "-[:REQUIRES]->"));
                        }
                        if (globalOperation) {
                            missingGlobalOperations.remove(name);
                        }
                    } else {
                        linkGlobalOperation(address, name);
                    }
                });
            }
        } else {
            stats.failedResources++;
        }
    }

    private List<String> readChildren(ResourceAddress address) {
        Operation rct = new Operation.Builder(READ_CHILDREN_TYPES, address)
                .param(INCLUDE_SINGLETONS, true)
                .build();

        ModelNode result = wc.execute(rct);
        if (result.isDefined()) {
            return result.asList().stream().map(ModelNode::asString).collect(toList());
        }
        return emptyList();
    }


    // ------------------------------------------------------ neo4j - resources

    private void writeVersion(ModelNode modelNode) {
        Cypher cypher = new Cypher("CREATE (:Version {")
                .append(MANAGEMENT_MAJOR_VERSION, modelNode.get(MANAGEMENT_MAJOR_VERSION).asInt()).comma()
                .append(MANAGEMENT_MICRO_VERSION, modelNode.get(MANAGEMENT_MICRO_VERSION).asInt()).comma()
                .append(MANAGEMENT_MINOR_VERSION, modelNode.get(MANAGEMENT_MINOR_VERSION).asInt())
                .append("})");

        nc.execute(cypher);
        stats.resources++;
    }

    private void createResource(ResourceAddress address) {
        Cypher cypher = new Cypher("CREATE (:Resource {")
                .append(NAME, address.getName()).comma()
                .append(ADDRESS, address.toString()).comma()
                .append(SINGLETON, address.isSingleton())
                .append("})");

        nc.execute(cypher);
        stats.resources++;
    }

    private void mergeChildOf(ResourceAddress child, ResourceAddress parent) {
        Cypher cypher = new Cypher("MATCH (child:Resource {")
                .append(ADDRESS, CHILD, child.toString()).append("}),")
                .append("(parent:Resource {")
                .append(ADDRESS, PARENT, parent.toString()).append("})")
                .append(" MERGE (child)-[:CHILD_OF]->(parent)");

        StatementResult statementResult = nc.execute(cypher);
        stats.relations += statementResult.summary().counters().relationshipsCreated();
    }

    // ------------------------------------------------------ neo4j - capability

    private void mergeCapabilities(ResourceAddress address, String capability) {
        Cypher cypher = new Cypher("MATCH (r:Resource {")
                .append(ADDRESS, address.toString()).append("})")
                .append(" MERGE (r)-[:DECLARES_CAPABILITY]->(:Capability {")
                .append(NAME, capability).append("})");

        StatementResult statementResult = nc.execute(cypher);
        stats.capabilities += statementResult.summary().counters().nodesCreated();
        stats.relations += statementResult.summary().counters().relationshipsCreated();
    }


    // ------------------------------------------------------ neo4j - attributes

    private void mergeAttribute(ResourceAddress address, String name, ModelNode attribute) {
        Cypher cypher = new Cypher("MATCH (r:Resource {")
                .append(ADDRESS, address.toString()).append("})")
                .append(" MERGE (r)-[:HAS_ATTRIBUTE]->(a:Attribute {")
                .append(NAME, name);

        addIfPresent(cypher, ACCESS_TYPE, attribute, ModelNode::asString);
        addIfPresent(cypher, ALIAS, attribute, ModelNode::asString);
        addIfPresent(cypher, ATTRIBUTE_GROUP, attribute, ModelNode::asString);
        addIfPresent(cypher, DEFAULT, attribute, ModelNode::asString);
        addIfPresent(cypher, EXPRESSIONS_ALLOWED, attribute, ModelNode::asBoolean);
        addIfPresent(cypher, MAX, attribute, ModelNode::asLong);
        addIfPresent(cypher, MAX_LENGTH, attribute, ModelNode::asLong);
        addIfPresent(cypher, MIN, attribute, ModelNode::asLong);
        addIfPresent(cypher, MIN_LENGTH, attribute, ModelNode::asLong);
        addIfPresent(cypher, NILLABLE, attribute, ModelNode::asBoolean);
        addIfPresent(cypher, REQUIRED, attribute, ModelNode::asBoolean);
        addIfPresent(cypher, RESTART_REQUIRED, attribute, ModelNode::asString);
        addIfPresent(cypher, STORAGE, attribute, ModelNode::asString);
        addIfPresent(cypher, TYPE, attribute, (value -> value.asType().name()));
        addIfPresent(cypher, UNIT, attribute, ModelNode::asString);
        addDeprecated(cypher, attribute);
        addValueType(cypher, attribute);

        cypher.append("})"); // end attribute
        if (attribute.hasDefined(CAPABILITY_REFERENCE)) {
            String capabilityReference = attribute.get(CAPABILITY_REFERENCE).asString();
            cypher.append(" MERGE (a)-[:REFERENCES_CAPABILITY]-(:Capability {")
                    .append(NAME, CAPABILITY_REFERENCE, capabilityReference)
                    .append("})");
        }

        StatementResult statementResult = nc.execute(cypher);
        stats.attributes += statementResult.summary().counters().nodesCreated();
        stats.relations += statementResult.summary().counters().relationshipsCreated();
    }

    private void mergeAttributeRelation(ResourceAddress address, String source, String target, String relation) {
        Cypher cypher = new Cypher("MATCH (r:Resource {")
                .append(ADDRESS, address.toString()).append("})")
                .append("-[:HAS_ATTRIBUTE]->(source:Attribute {")
                .append(NAME, "sourceName", source).append("}),")

                .append("(r)-[:HAS_ATTRIBUTE]->(target:Attribute {")
                .append(NAME, "targetName", target).append("})")

                .append(" MERGE (source)").append(relation).append("(target)");

        StatementResult statementResult = nc.execute(cypher);
        stats.relations += statementResult.summary().counters().relationshipsCreated();
    }


    // ------------------------------------------------------ neo4j - operations

    private void mergeOperation(ResourceAddress address, String name, ModelNode operation, boolean globalOperation) {
        Cypher cypher = new Cypher("MATCH (r:Resource {")
                .append(ADDRESS, address.toString()).append("})")
                .append(" MERGE (r)-[:PROVIDES]->(o:Operation {")
                .append(NAME, name).comma()
                .append(GLOBAL, globalOperation || ADD.equals(name)); // add is a global operation!

        addIfPresent(cypher, READ_ONLY, operation, ModelNode::asBoolean);
        addIfPresent(cypher, RUNTIME_ONLY, operation, ModelNode::asBoolean);

        if (operation.hasDefined(REPLY_PROPERTIES)) {
            ModelNode replyNode = operation.get(REPLY_PROPERTIES);
            if (replyNode.isDefined()) {
                addIfPresent(cypher, "return", replyNode, TYPE, (value -> value.asType().name()));
                addValueType(cypher, replyNode);
            }
        }
        cypher.append("})");

        StatementResult statementResult = nc.execute(cypher);
        stats.operations += statementResult.summary().counters().nodesCreated();
        stats.relations += statementResult.summary().counters().relationshipsCreated();
    }

    private void mergeRequestProperty(ResourceAddress address, String operationName, String requestPropertyName,
            ModelNode requestProperty) {
        Cypher cypher = new Cypher("MATCH (r:Resource {")
                .append(ADDRESS, address.toString())
                .append("})-[:PROVIDES]->(o:Operation {")
                .append(NAME, OPERATION_NAME, operationName).append("})")
                .append(" MERGE (o)-[:ACCEPTS]->(p:Parameter {")
                .append(NAME, requestPropertyName);

        addIfPresent(cypher, EXPRESSIONS_ALLOWED, requestProperty, ModelNode::asBoolean);
        addIfPresent(cypher, MAX, requestProperty, ModelNode::asLong);
        addIfPresent(cypher, MAX_LENGTH, requestProperty, ModelNode::asLong);
        addIfPresent(cypher, MIN, requestProperty, ModelNode::asLong);
        addIfPresent(cypher, MIN_LENGTH, requestProperty, ModelNode::asLong);
        addIfPresent(cypher, NILLABLE, requestProperty, ModelNode::asBoolean);
        addIfPresent(cypher, REQUIRED, requestProperty, ModelNode::asBoolean);
        addIfPresent(cypher, TYPE, requestProperty, (value -> value.asType().name()));
        addIfPresent(cypher, UNIT, requestProperty, ModelNode::asString);
        addDeprecated(cypher, requestProperty);

        cypher.append("})"); // end parameter
        if (requestProperty.hasDefined(CAPABILITY_REFERENCE)) {
            String capabilityReference = requestProperty.get(CAPABILITY_REFERENCE).asString();
            cypher.append(" MERGE (p)-[:REFERENCES_CAPABILITY]-(:Capability {")
                    .append(NAME, CAPABILITY_REFERENCE, capabilityReference)
                    .append("})");
        }

        StatementResult statementResult = nc.execute(cypher);
        stats.parameters += statementResult.summary().counters().nodesCreated();
        stats.relations += statementResult.summary().counters().relationshipsCreated();
    }

    private void mergeRequestPropertyRelation(ResourceAddress address, String operation, String source, String target,
            String relation) {
        Cypher cypher = new Cypher("MATCH (:Resource {")
                .append(ADDRESS, address.toString()).append("})")
                .append("-[:PROVIDES]->(o:Operation {")
                .append(NAME, OPERATION_NAME, operation).append("}),")

                .append("(o)-[:ACCEPTS]->(source:Parameter {")
                .append(NAME, "sourceName", source).append("}),")

                .append("(o)-[:ACCEPTS]->(target:Parameter {")
                .append(NAME, "targetName", target).append("})")

                .append(" MERGE (source)").append(relation).append("(target)");

        StatementResult statementResult = nc.execute(cypher);
        stats.relations += statementResult.summary().counters().relationshipsCreated();
    }

    private void linkGlobalOperation(ResourceAddress address, String operationName) {
        Cypher cypher = new Cypher("MATCH (r:Resource {")
                .append(ADDRESS, address.toString()).append("}),")
                .append("(o:Operation{")
                .append(NAME, operationName).append("})")
                .append(" MERGE (r)-[:PROVIDES]->(o)");

        StatementResult statementResult = nc.execute(cypher);
        stats.relations += statementResult.summary().counters().relationshipsCreated();
    }


    // ------------------------------------------------------ helper methods

    private void addDeprecated(Cypher cypher, ModelNode modelNode) {
        if (modelNode.hasDefined(DEPRECATED)) {
            cypher.comma().append(DEPRECATED, true);
            ModelNode deprecatedNode = modelNode.get(DEPRECATED);
            addIfPresent(cypher, SINCE, deprecatedNode, ModelNode::asString);
        }
    }

    private void addValueType(Cypher cypher, ModelNode modelNode) {
        if (modelNode.hasDefined(VALUE_TYPE)) {
            ModelNode valueTypeNode = modelNode.get(VALUE_TYPE);
            try {
                ModelType valueType = valueTypeNode.asType();
                cypher.comma().append(VALUE_TYPE, valueType.name());
            } catch (IllegalArgumentException e) {
                cypher.comma().append(VALUE_TYPE, ModelType.OBJECT.name());
            }
        }
    }

    private <T> void addIfPresent(Cypher cypher, String name, ModelNode modelNode, Function<ModelNode, T> getValue) {
        addIfPresent(cypher, name, modelNode, name, getValue);
    }

    private <T> void addIfPresent(Cypher cypher, String name, ModelNode modelNode, String attribute,
            Function<ModelNode, T> getValue) {
        if (modelNode.hasDefined(attribute)) {
            ModelNode value = modelNode.get(attribute);
            // must not be the first append(name, value) call!
            cypher.comma().append(name, getValue.apply(value));
        }
    }


    // ------------------------------------------------------ properties

    Stats stats() {
        return stats;
    }
}
