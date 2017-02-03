# WildFly Model Graph

Tool which reads the management model from a WildFly instance and stores it in a [Neo4j](https://neo4j.com/) database. If not specified otherwise the tool will start at the root resource and read the resource descriptions in a recursive way. 

## Graph

The tool creates the following graph in the Neo4j database:

![Alt text](https://g.gravizo.com/g?
 digraph G {
   Resource -> Resource [label="CHILD"];
   Resource -> Attribute [label="HAS_ATTRIBUTE"];
   Attribute -> Attribute [label="REQUIRES"];
   Attribute -> Attribute [label="ALTERNATIVE"];
   Resource -> Capability [label="DECLARES_CAPABILITY"];
   Attribute -> Capability [label="REFERENCES_CAPABILITY"];
 }
)

There are three main nodes in the database:

1. Resource  
The resource holds the fully qualified address and the name of the resource. The name of a resource is the resource type. For singleton resources the name consists of the type and the name: 

    | Address                                          | Name        |
    |--------------------------------------------------|-------------|
    | /subsystem=datasources/data-source=*             | data-source |
    | /subsystem=mail/mail-session=default/server=imap | server=imap |

1. Attribute  
The attribute stores most of the attribute's metadata taken from the r-r-d operation

1. Capability  
Holds just the name of the capability. 

Operations are not yet covered, but could easily be integrated. 
 
## Get Started

In order to run the tool, you need a running WildFly and Neo4j instance. The tool understands the following command line options:

```sh
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

After running the tool head to http://localhost:7474/browser/ and enter some queries. 

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

List all resources and attributes which are part of an attribute group:

```cypher
MATCH (r:Resource)-->(a:Attribute) 
WHERE exists(a.`attribute-group`)
RETURN r.address, a.name, a.`attribute-group`
```

List all complex attributes (i.e. attributes with a value type other than STRING):

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

See https://neo4j.com/docs/cypher-refcard/current/ for a quick reference to the Cypher query language. 