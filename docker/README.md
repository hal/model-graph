# Docker 

Docker images to run Neo4j with pre-populated databases containing the management model of WildFly 9, 10 and 11 using `standalone-full-ha.xml`. The following images are available:

- `hpehl/model-graph-wildfly` (WildFly 11.0.0.CR1)
- `hpehl/model-graph-wildfly:10.1.0.Final`
- `hpehl/model-graph-wildfly:9.0.2.Final`

All Neo4j images use `neo4j` as username & password.

Furthermore there's a Nginx based docker image called `hpehl/model-graph-nginx`. This image provides documentation about the model-graph database, the nodes and relations and sample queries. The Neo4j images are configured to show the documentation in the Neo4j browser. When running the Nginx docker image make sure to use 8080 as the host port:

```bash
docker run --publish 8080:80 hpehl/model-graph-nginx
``` 

## Getting Started

The `compose` folder contains docker compose scripts to start Neo4j and Nginx.  

### Single WildFly Version

Please note that you can only use one of the `compose/wf<n>` scripts at a time. All of them use the standard ports http://localhost:7474 and bolt://localhost:7687.

- WildFly 11

    ```bash
    cd compose/wf11
    docker-compose up
    ```

- WildFly 10

    ```bash
    cd compose/wf10
    docker-compose up
    ```

- WildFly 9

    ```bash
    cd compose/wf9
    docker-compose up
    ```

### All WildFly Versions

Useful when you want to compare resources between different WildFly versions.

```bash
cd compose
docker-compose up
```

This starts three Neo4j instances using the following ports:

- WildFly 11:  
  http://localhost:7411  
  bolt://localhost:7611 
  
- WildFly 10:  
  http://localhost:7410  
  bolt://localhost:7610
  
- WildFly 9:  
  http://localhost:7409  
  bolt://localhost:7609
