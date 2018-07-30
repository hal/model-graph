# Docker 

Docker images to run Neo4j with pre-populated databases containing the management model of the last three WildFly versions using `standalone-full-ha.xml`. The following images are available:

- `hpehl/model-graph-wildfly:13.0.0.Final`
- `hpehl/model-graph-wildfly:12.0.0.Final`
- `hpehl/model-graph-wildfly:11.0.0.Final`

All Neo4j databases are read-only and don't require authentication.

## Getting Started

The easiest way to get started is to use the docker compose scripts in the `compose` (sub)folders. The scripts start Neo4j with the right port settings.  

### Single WildFly Version

The scripts in `compose/wf<n>` start the Neo4j image with the model graph database of a specific WildFly version. 

Please note that you can only use one of the `compose/wf<n>` scripts at a time. All of them use the standard ports http://localhost:7474 and bolt://localhost:7687.

- WildFly 13

    ```bash
    cd compose/wf13
    docker-compose up
    ```

- WildFly 12

    ```bash
    cd compose/wf12
    docker-compose up
    ```

- WildFly 11

    ```bash
    cd compose/wf11
    docker-compose up
    ```

### All WildFly Versions

There's also a docker compose script which starts all Neo4j instances with the model graph databases of the last three WildFly versions. This is especially useful if you want to compare resources between different WildFly versions.

```bash
cd compose
docker-compose up
```

This starts Neo4j instances with the following ports:

- WildFly 13:
  http://localhost:7413  
  bolt://localhost:7613
  
- WildFly 12:
  http://localhost:7412  
  bolt://localhost:7612
  
- WildFly 11:
  http://localhost:7411  
  bolt://localhost:7611
