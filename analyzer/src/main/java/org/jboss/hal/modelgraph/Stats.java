package org.jboss.hal.modelgraph;

class Stats {

    long resources;
    long failedResources;
    long attributes;
    long sensitive;
    long operations;
    long parameters;
    long capabilities;
    long relations;

    @Override
    public String toString() {
        String result = String.format("Successfully created%n\t%,8d resources%n" +
                        "\t%,8d attributes%n" +
                        "\t%,8d sensitive constraints%n" +
                        "\t%,8d operations%n" +
                        "\t%,8d request properties%n" +
                        "\t%,8d capabilities and%n" +
                        "\t%,8d relationships",
                resources, attributes, sensitive, operations, parameters, capabilities, relations);
        if (failedResources > 0) {
            result += String.format("%n\t%,8d resources could not be processed.", failedResources);
        }
        return result;
    }
}
