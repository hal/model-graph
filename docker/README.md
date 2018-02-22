# Docker 

Docker images to run Neo4j with pre-populated databases containing the management model of WildFly 9, 10 and 11 using `standalone-full-ha.xml`. The following images are available:

- `hpehl/model-graph-wildfly:11.0.0.Final`
- `hpehl/model-graph-wildfly:10.1.0.Final`
- `hpehl/model-graph-wildfly:9.0.2.Final`

All Neo4j databases are read-only and don't require authentication.

## Getting Started

The easiest way to get started is to use the docker compose scripts in the `compose` (sub)folders. The scripts start Neo4j with the right port settings.  

### Single WildFly Version

The scripts in `compose/wf<n>` start the Neo4j image with the model graph database of a specific WildFly version. 

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

There's also a docker compose script which starts all Neo4j instances with the model graph databases for WildFly 9, 10 and 11. This is especially useful if you want to compare resources between different WildFly versions.

```bash
cd compose
docker-compose up
```

This starts Neo4j instances with the following ports:

- WildFly 11:  
  http://localhost:7411  
  bolt://localhost:7611
  
- WildFly 10:  
  http://localhost:7410  
  bolt://localhost:7610
  
- WildFly 9:  
  http://localhost:7409  
  bolt://localhost:7609
