# WildFly Model Graph

Tool which reads the management model from a WildFly instance and stores it as a graph in a [Neo4j](https://neo4j.com/) database. If not specified otherwise the tool starts at the root resource and reads the resource descriptions in a recursive way. 

## Graph

The tool creates the following graph:

![Alt text](https://g.gravizo.com/g?
 digraph mg {
   Resource -> Resource [label="CHILD_OF"];
   Resource -> Capability [label="DECLARES_CAPABILITY"];
   Resource -> Attribute [label="HAS_ATTRIBUTE"];
   Attribute -> Attribute [label="REQUIRES"];
   Attribute -> Attribute [label="ALTERNATIVE"];
   Attribute -> Capability [label="REFERENCES_CAPABILITY"];
   Resource -> Operation [label="PROVIDES"];
   Operation -> Parameter [label="ACCEPTS"];
   Parameter -> Parameter [label="REQUIRES"];
   Parameter -> Parameter [label="ALTERNATIVE"];
   Parameter -> Capability [label="REFERENCES_CAPABILITY"];
 }
)

There are five main nodes in the graph:

1. Resource
    
    The resource node holds the fully qualified address and the name of the resource. The name of a resource is the resource type. For singleton resources the name consists of the type and the name: 

    | Address                                          | Name        |
    |--------------------------------------------------|-------------|
    | /subsystem=datasources/data-source=*             | data-source |
    | /subsystem=mail/mail-session=default/server=imap | server=imap |
    
    Parent resources have a `CHILD_OF` relationship with their children. This makes traversing through the model tree very convenient.

1. Attribute

    The attribute node holds most of the attribute's metadata such as type, required, nillable or storage. 

1. Operation

    The operation node holds information about an operation. Global operations are stored only once (except the `add` operation). Use the flag `global` to distinguish between global and resource dependent operations.

1. Parameter

    The parameter node holds information about the request properties and has similar properties as the `attribute` node.

1. Capability
  
    The capability node holds just the name of the capability. 

See the Neo4j browser for the complete list of nodes, relations and properties. 
 
## Get Started

To get started, you need a running WildFly and Neo4j instance. To install Neo4j, download it from https://neo4j.com/download/ or use `brew install neo4j`. To start Neo4j run `neo4j start` from the command line and open [http://localhost:7474/](http://localhost:7474/). If you login for the first time, you have to change your password. To change it back to the default use 

```cypher
CALL dbms.changePassword('neo4j')
```

and refresh your browser. This makes it easier to use the default options when analysing the model tree. Anyway you can specify the WildFly and Neo4j instance using one of the command line options:

```
Usage: <main class> [options]

  Options:
    -help, --help
      Shows this help

    -clean
      Removes all indexes, nodes, relationships and properties before 
      analysing the model tree.

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

If everything runs locally using the default ports and credentials, you just need to run 

```bash
java -jar model-graph-0.0.5.jar
```

The tool will populate the Neo4j instance with nodes, relations and properties of the specified resource (sub)tree. Please make sure the Neo4j instance is empty or use the `-clean` option to remove existing data. 

If you want to analyse different management model versions, you need to setup multiple Neo4j instances and point the tool to the relevant instance. After the tool has finished, head to [http://localhost:7474/](http://localhost:7474/) and enter some queries. 

## Examples

Here are a few examples how to query the database:

### Resources & Relationships

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

### Attributes

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
WHERE NOT (a)-[:ALTERNATIVE]-() AND 
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
RETURN r.address, a.name, a.since ORDER BY a.since DESC
```

### Operations

List all resources with more than five non-global operations:
 
```cypher
MATCH (r:Resource)-[p:PROVIDES]->(o:Operation)
WHERE NOT o.global
WITH r, count(p) as operations
WHERE operations > 5
RETURN r.address, operations ORDER BY operations DESC
```

List all `add` operations with more than two required parameters:

```cypher
MATCH (r:Resource)-[:PROVIDES]->(o:Operation)-[a:ACCEPTS]->(p:Parameter)
WHERE o.name = "add" AND p.required
WITH r, o, count(a) as parameters
WHERE parameters > 2
RETURN r.address, o.name, parameters ORDER BY parameters DESC
```

List all deprecated operation parameters:

```cypher
MATCH (r:Resource)-->(o:Operation)-->(p:Parameter)
WHERE exists(p.deprecated)
RETURN r.address, o.name, p.name, p.since ORDER BY p.since DESC
```

See https://neo4j.com/docs/cypher-refcard/current/ for a quick reference of the Cypher query language. 