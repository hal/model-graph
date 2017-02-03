package org.jboss.hal.modelgraph;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.hal.modelgraph.dmr.Operation;
import org.jboss.hal.modelgraph.dmr.ResourceAddress;
import org.jboss.hal.modelgraph.dmr.WildFlyClient;
import org.jboss.hal.modelgraph.neo4j.Cypher;
import org.jboss.hal.modelgraph.neo4j.Neo4jClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;
import static org.jboss.hal.modelgraph.dmr.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
class Analyzer {

    private static final int MAX_DEPTH = 10;
    private static final Logger logger = LoggerFactory.getLogger(Analyzer.class);

    private final WildFlyClient wc;
    private final Neo4jClient nc;
    private long[] resources;

    Analyzer(final WildFlyClient wc, final Neo4jClient nc) {
        this.wc = wc;
        this.nc = nc;
        this.resources = new long[2]; // [0] = failed, [1] = successful
    }

    void start(final String resource) {
        parse(ResourceAddress.of(resource), null);
    }


    // ------------------------------------------------------ management model

    private void parse(ResourceAddress address, ResourceAddress parent) {
        if (address.size() < MAX_DEPTH) {
            parseResource(address, parent);
            readChildren(address).forEach(child -> parse(address.add(child), address));
        } else {
            logger.warn("Skipping {}. Maximum nesting of {} reached.", address.toString(), MAX_DEPTH);
        }
    }

    private void parseResource(ResourceAddress address, ResourceAddress parent) {
        Operation rrd = new Operation.Builder(READ_RESOURCE_DESCRIPTION_OPERATION, address)
                .param(INCLUDE_ALIASES, true)
                .build();

        ModelNode resourceDescription = wc.execute(rrd);
        if (resourceDescription.isDefined()) {
            logger.info("Parse {}", address.toString());

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
                        mergeAttributeRelation(address, entry.getKey(), entry.getValue(),"-[:REQUIRES]->"));
            }
            resources[1]++;
        } else {
            resources[0]++;
        }
    }

    private List<String> readChildren(ResourceAddress address) {
        Operation rct = new Operation.Builder(READ_CHILDREN_TYPES_OPERATION, address)
                .param(INCLUDE_SINGLETONS, true)
                .build();

        ModelNode result = wc.execute(rct);
        if (result.isDefined()) {
            return result.asList().stream().map(ModelNode::asString).collect(toList());
        }
        return Collections.emptyList();
    }


    // ------------------------------------------------------ neo4j

    private void createResource(ResourceAddress address) {
        Cypher cypher = new Cypher("CREATE (:Resource {")
                .append(NAME, address.getName()).comma()
                .append(ADDRESS, address.toString()).comma()
                .append(SINGLETON, address.isSingleton())
                .append("})");
        nc.execute(cypher);
    }

    private void mergeChildOf(ResourceAddress child, ResourceAddress parent) {
        Cypher cypher = new Cypher("MATCH (child:Resource {")
                .append(ADDRESS, CHILD, child.toString()).append("}),")
                .append("(parent:Resource {")
                .append(ADDRESS, PARENT, parent.toString()).append("})")
                .append(" MERGE (child)-[:CHILD_OF]->(parent)");
        nc.execute(cypher);
    }

    private void mergeCapabilities(ResourceAddress address, String capability) {
        Cypher cypher = new Cypher("MATCH (r:Resource {")
                .append(ADDRESS, address.toString()).append("})")
                .append(" MERGE (r)-[:DECLARES_CAPABILITY]->(:Capability {")
                .append(NAME, capability).append("})");
        nc.execute(cypher);
    }

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
        addIfPresent(cypher, MIN, attribute, ModelNode::asLong);
        addIfPresent(cypher, NILLABLE, attribute, ModelNode::asBoolean);
        addIfPresent(cypher, REQUIRED, attribute, ModelNode::asBoolean);
        addIfPresent(cypher, RESTART_REQUIRED, attribute, ModelNode::asString);
        addIfPresent(cypher, STORAGE, attribute, ModelNode::asString);
        addIfPresent(cypher, TYPE, attribute, (value -> value.asType().name()));
        addIfPresent(cypher, UNIT, attribute, ModelNode::asString);

        if (attribute.hasDefined(DEPRECATED)) {
            cypher.comma().append(DEPRECATED, true);
            ModelNode deprecatedNode = attribute.get(DEPRECATED);
            addIfPresent(cypher, SINCE, deprecatedNode, ModelNode::asString);
        }

        if (attribute.hasDefined(VALUE_TYPE)) {
            ModelNode valueTypeNode = attribute.get(VALUE_TYPE);
            try {
                ModelType valueType = valueTypeNode.asType();
                cypher.comma().append(VALUE_TYPE, valueType.name());
            } catch (IllegalArgumentException e) {
                cypher.comma().append(VALUE_TYPE, ModelType.OBJECT.name());
            }
        }

        cypher.append("})"); // end attribute
        if (attribute.hasDefined(CAPABILITY_REFERENCE)) {
            String capabilityReference = attribute.get(CAPABILITY_REFERENCE).asString();
            cypher.append(" MERGE (a)-[:REFERENCES_CAPABILITY]-(:Capability {")
                    .append(NAME, CAPABILITY_REFERENCE, capabilityReference)
                    .append("})");
        }

        nc.execute(cypher);
    }

    private void mergeAttributeRelation(ResourceAddress address, String source, String target, String relation) {
        final Cypher cypher = new Cypher("MATCH (r1:Resource {")
                .append(ADDRESS, address.toString()).append("})")
                .append("-[:HAS_ATTRIBUTE]->(source:Attribute {")
                .append(NAME, "sourceName", source).append("}),")
                .append("(r2:Resource {")
                .append(ADDRESS, address.toString()).append("})")
                .append("-[:HAS_ATTRIBUTE]->(target:Attribute {")
                .append(NAME, "targetName", target).append("})")
                .append(" MERGE (source)").append(relation).append("(target)");
        nc.execute(cypher);
    }

    private <T> void addIfPresent(Cypher cypher, String name, ModelNode modelNode, Function<ModelNode, T> getValue) {
        if (modelNode.hasDefined(name)) {
            ModelNode value = modelNode.get(name);
            // must not be the first append(name, value) call!
            cypher.comma().append(name, getValue.apply(value));
        }
    }


    // ------------------------------------------------------ properties

    long getSuccessfulResources() {
        return resources[1];
    }

    long getFailedResources() {
        return resources[0];
    }
}
