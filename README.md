# WildFly Model Graph

This repository contains 

- a [command line tool](analyzer/README.md) which reads the management model from a WildFly instance and stores it as a graph in a [Neo4j](https://neo4j.com/) database.
- [Docker images](docker/README.md) with graph databases for WildFly 9, 10 and 11.
- [scripts](openshift/README.md) to use the graph databases on OpenShift.

## Graph Database

![Model Graph](docker/nginx/html/model-graph.png)

There are six main nodes in the database:

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
    
1. Constraint

    The constraint node holds information about attribute constraints. Each constraint has a name and a type. 

1. Capability
  
    The capability node holds just the name of the capability. 

In addition the database contains a `Version` node with information about the WildFly and management model version. See the Neo4j browser for the complete list of nodes, relations and properties. 
 
## Queries

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

The top twenty resources with lots of attributes:

```cypher
MATCH (r:Resource)-[has:HAS_ATTRIBUTE]->()
RETURN r.address, COUNT(has) as attributes
ORDER BY attributes DESC
LIMIT 20
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

List all attributes which are both required and nillable, but which don't have alternatives:

```cypher
MATCH (r:Resource)-->(a:Attribute)
WHERE NOT (a)-[:ALTERNATIVE]-() AND 
      a.required = true AND 
      a.nillable = true AND 
      a.storage = "configuration"
RETURN r.address, a.name
```

List all attributes which are required and have a default value:

```cypher
MATCH (r:Resource)-->(a:Attribute)
WHERE a.required = true AND 
      exists(a.default)
RETURN r.address, a.name, a.default
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
ORDER BY a.since DESC
```

### Operations

List all resources with more than five non-global operations:
 
```cypher
MATCH (r:Resource)-[p:PROVIDES]->(o:Operation)
WHERE NOT o.global
WITH r, count(p) as operations
WHERE operations > 5
RETURN r.address, operations 
ORDER BY operations DESC
```

List all `add` operations with more than two required parameters:

```cypher
MATCH (r:Resource)-[:PROVIDES]->(o:Operation)-[a:ACCEPTS]->(p:Parameter)
WHERE o.name = "add" AND p.required
WITH r, o, count(a) as parameters
WHERE parameters > 2
RETURN r.address, o.name, parameters 
ORDER BY parameters DESC
```

List all deprecated operation parameters:

```cypher
MATCH (r:Resource)-->(o:Operation)-->(p:Parameter)
WHERE exists(p.deprecated)
RETURN r.address, o.name, p.name, p.since 
ORDER BY p.since DESC
```

### Version

Show the release and management model version:

```cypher
MATCH (v:Version) 
RETURN v.`release-codename` + " " + v.`release-version` as Release,
	   v.`management-major-version` + "." + v.`management-minor-version` + "." + v.`management-micro-version` as `Management Model Version`
```

See https://neo4j.com/docs/cypher-refcard/current/ for a quick reference of the Cypher query language. 