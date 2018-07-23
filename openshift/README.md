# OpenShift 

The following commands can be used to setup the Neo4J databases on OpenShift: 

```bash
oc new-project model-graph

# WildFly 13
oc new-app --name=model-graph-13 -l wildfly=13 hpehl/model-graph-wildfly:13.0.0.Final
oc expose service/model-graph-13 -l wildfly=13 --name=model-graph-13-browser --port=7474 
oc expose service/model-graph-13 -l wildfly=13 --name=model-graph-13-bolt    --port=7687 

# WildFly 12
oc new-app --name=model-graph-12 -l wildfly=12 hpehl/model-graph-wildfly:12.0.0.Final
oc expose service/model-graph-12 -l wildfly=12 --name=model-graph-12-browser --port=7474 
oc expose service/model-graph-12 -l wildfly=12 --name=model-graph-12-bolt    --port=7687 

# WildFly 11
oc new-app --name=model-graph-11 -l wildfly=11 hpehl/model-graph-wildfly:11.0.0.Final
oc expose service/model-graph-11 -l wildfly=11 --name=model-graph-11-browser --port=7474 
oc expose service/model-graph-11 -l wildfly=11 --name=model-graph-11-bolt    --port=7687 
```

## Reduced Memory

Per default the Neo4J JVMs use 512 MB heap memory. In case your OpenShift account is limited, use one of the following commands to start Neo4J with less memory:

```bash
# WildFly 13
oc new-app --name=model-graph-13 \
           -l wildfly=13 \
           -e NEO4J_dbms_memory_heap_initial__size=128M \
           -e NEO4J_dbms_memory_heap_max__size=128M \
           hpehl/model-graph-wildfly:13.0.0.Final

# WildFly 12
oc new-app --name=model-graph-12 \
           -l wildfly=12 \
           -e NEO4J_dbms_memory_heap_initial__size=128M \
           -e NEO4J_dbms_memory_heap_max__size=128M \
           hpehl/model-graph-wildfly:12.0.0.Final

# WildFly 11
oc new-app --name=model-graph-11 \
           -l wildfly=11 \
           -e NEO4J_dbms_memory_heap_initial__size=128M \
           -e NEO4J_dbms_memory_heap_max__size=128M \
           hpehl/model-graph-wildfly:11.0.0.Final
```
