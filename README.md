# WildFly Model Graph

Tool which reads the management model from a WildFly instance and stores it as a graph in a [Neo4j](https://neo4j.com/) database. If not specified otherwise the tool starts at the root resource and reads the resource descriptions in a recursive way. 

## Graph

The tool creates the following graph:

![Alt text](https://g.gravizo.com/g?
 digraph mg {
   rankdir=LR;
   Resource -> Resource [label="CHILD_OF"];
   Resource -> Attribute [label="HAS_ATTRIBUTE"];
   Attribute -> Attribute [label="REQUIRES"];
   Attribute -> Attribute [label="ALTERNATIVE"];
   Resource -> Capability [label="DECLARES_CAPABILITY"];
   Attribute -> Capability [label="REFERENCES_CAPABILITY"];
 }
)

There are three main nodes in the graph:

1. Resource
    
    The resource node holds the fully qualified address and the name of the resource. The name of a resource is the resource type. For singleton resources the name consists of the type and the name: 

    | Address                                          | Name        |
    |--------------------------------------------------|-------------|
    | /subsystem=datasources/data-source=*             | data-source |
    | /subsystem=mail/mail-session=default/server=imap | server=imap |
    
    Parent resources have a `CHILD_OF` relationship with their children. This makes traversing through the model tree very convenient.

1. Attribute  
The attribute node holds most of the attribute's metadata such as type, required, nillable or storage. 

1. Capability  
The capability node holds just the name of the capability. 

Operations are not yet part of the graph, but could easily be added. See the Neo4j browser for the complete list of nodes, relations and properties. 
 
## Get Started

To get started, you need a running WildFly and Neo4j instance. You can specify the WildFly and Neo4j instance using one of the command line options:

```
Usage: <main class> [options]

  Options:
    -help, --help
      Shows this help

    -neo4j
      Neo4j database as <server>[:<port>] with 7687 as default port. Omit to 
      connect to a local Neo4j database at localhost:7687.

    -neo4j-password
      Neo4j password
      Default: neo4j

    -neo4j-user
      Neo4j username
      Default: neo4j

    -resource
      The root resource to analyse.
      Default: /

    -wildfly
      WildFly instance as <server>[:<port>] with 9990 as default port. Omit to 
      connect to a local WildFly instance at localhost:9990.

    -wildfly-password
      WildFly password
      Default: admin

    -wildfly-user
      WildFly username
      Default: admin
```

If everything runs locally using the default ports, you just need to run 

```bash
java -jar model-graph-0.0.1.jar
```

The tool will populate the Neo4j instance with nodes, relations and properties of the specified resource (sub)tree. Please make sure the Neo4j instance is empty. If you have nodes of a previous run or other data, this might distort the queries. 

If you want to analyse different management model versions, you need to setup multiple Neo4j instances and point the tool to the relevant instance. After the tool has finished, head to [http://localhost:7474/browser/](http://localhost:7474/browser/) and enter some queries. 

## Examples

Here are a few examples what you can do with the collected data:

Show the `alternatives` and `requires` relations of the `connection-definitions` resource:

```cypher
MATCH g=(r:Resource)-->(:Attribute)-[:ALTERNATIVE|:REQUIRES]->(:Attribute) 
WHERE r.name = "connection-definitions" 
RETURN g
```

Show all resources where's a `requires` relation between attributes:

```cypher
MATCH g=(r:Resource)-->(:Attribute)-[:REQUIRES]->(:Attribute) 
RETURN g
```

Show all `data-source` resource trees:

```cypher
MATCH g=(r:Resource)-[:CHILD_OF*..10]->()
WHERE r.name = "data-source"
RETURN g
```

List all attributes which have a capability reference to `org.wildfly.network.socket-binding`:

```cypher
MATCH (r:Resource)-->(a:Attribute)-[:REFERENCES_CAPABILITY]->(c:Capability)
WHERE c.name = "org.wildfly.network.socket-binding"
RETURN r.address, a.name
```

List all attributes which match the regexp `.*socket-binding.*`, but do not have a capability reference

```cypher
MATCH (r:Resource)-->(a:Attribute)
WHERE a.name =~ ".*socket-binding.*" AND 
      NOT (a)-[:REFERENCES_CAPABILITY]-()
RETURN r.address, a.name
```

List all attributes which are both required and nillable together with their alternatives:

```cypher
MATCH (r:Resource)-->(a:Attribute)-[:ALTERNATIVE]-(alt) 
WHERE a.required = true AND 
      a.nillable = true AND 
      a.storage = "configuration"
RETURN r.address, a.name, alt.name
```

List all attributes which are both required and nillable, but which don't have alternatives (should return no results!)

```cypher
MATCH (r:Resource)-->(a:Attribute)
WHERE NOT (a)-[:ALTERNATIVE]-()  AND 
      a.required = true AND 
      a.nillable = true AND 
      a.storage = "configuration"
RETURN r.address, a.name
```

List all complex attributes (i.e. attributes with a value type other than `STRING`):

```cypher
MATCH (r:Resource)-->(a:Attribute) 
WHERE exists(a.`value-type`) AND a.`value-type` = "OBJECT"
RETURN r.address, a.name
```

List all deprecated attributes:

```cypher
MATCH (r:Resource)-->(a:Attribute) 
WHERE exists(a.deprecated)
RETURN r.address, a.name, a.since
```

See https://neo4j.com/docs/cypher-refcard/current/ for a quick reference of the Cypher query language. 