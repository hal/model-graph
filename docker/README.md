# Docker 

Docker images to run Neo4j with pre-populated databases containing the management model of WildFly 9, 10, 11 and latest using `standalone-full-ha.xml`. The following images are available:

- [`hpehl/model-graph-wildfly`](https://hub.docker.com/r/hpehl/model-graph-wildfly/) ([latest](https://ci.wildfly.org/viewType.html?buildTypeId=WF_Nightly) WildFly version)
- `hpehl/model-graph-wildfly:11.0.0.Final`
- `hpehl/model-graph-wildfly:10.1.0.Final`
- `hpehl/model-graph-wildfly:9.0.2.Final`

All Neo4j images use `neo4j` as username & password.

Furthermore there's a Nginx based docker image called [`hpehl/model-graph-nginx`](https://hub.docker.com/r/hpehl/model-graph-nginx/). This image provides documentation about the model-graph database, nodes, relations and sample queries. The Neo4j images are configured to show the documentation in the Neo4j browser. When running the Nginx image make sure to use 8080 as the host port:

```bash
docker run --publish 8080:8080 hpehl/model-graph-nginx
``` 

## Getting Started

The easiest way to get started is to use the docker compose scripts in the `compose` (sub)folders. The scripts start Neo4j and Nginx with the right port settings.  

### Single WildFly Version

The scripts in `compose/wf<n>` start the Nginx image and a Neo4j image with the model graph database of a specific WildFly version. 

Please note that you can only use one of the `compose/wf<n>` scripts at a time. All of them use the standard ports http://localhost:7474 and bolt://localhost:7687.

- WildFly Latest

    ```bash
    cd compose/wf-latest
    docker-compose up
    ```

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

There's also a docker compose script which starts all Neo4j instances with the model graph databases for WildFly 9, 10, 11 and latest. This is especially useful if you want to compare resources between different WildFly versions.

```bash
cd compose
docker-compose up
```

This starts Neo4j instances with the following ports:

- WildFly Latest:  
  http://localhost:7474  
  bolt://localhost:7687
  
- WildFly 11:  
  http://localhost:7411  
  bolt://localhost:7611
  
- WildFly 10:  
  http://localhost:7410  
  bolt://localhost:7610
  
- WildFly 9:  
  http://localhost:7409  
  bolt://localhost:7609
