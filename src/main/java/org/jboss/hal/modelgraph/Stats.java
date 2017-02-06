package org.jboss.hal.modelgraph;

/**
 * @author Harald Pehl
 */
class Stats {

    long resources;
    long failedResources;
    long attributes;
    long operations;
    long parameters;
    long capabilities;

    @Override
    public String toString() {
        String result = String.format("Successfully created%n\t%,8d resources%n" +
                "\t%,8d attributes,%n" +
                "\t%,8d operations,%n" +
                "\t%,8d request properties and %n" +
                "\t%,8d capabilities.",
                resources, attributes, operations, parameters, capabilities);
        if (failedResources > 0) {
            result += String.format("%n\t%,8d resources could not be processed.", failedResources);
        }
        return result;
    }
}
