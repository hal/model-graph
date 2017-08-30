package org.jboss.hal.modelgraph.neo4j;

import org.junit.Test;
import org.neo4j.driver.v1.Value;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Harald Pehl
 */
public class CypherTest {

    @Test
    public void simple() {
        assertEquals("foo", new Cypher("foo").statement());
    }

    @Test
    public void append() {
        Cypher cypher = new Cypher("foo").append("-bar");
        assertEquals("foo-bar", cypher.statement());
        assertTrue(cypher.parameters().isEmpty());
    }

    @Test
    public void variable() {
        Cypher cypher = new Cypher("CREATE (:Foo {")
                .append("name", "foo")
                .append("})");
        Value parameters = cypher.parameters();
        assertEquals("CREATE (:Foo {name: {name}})", cypher.statement());
        assertEquals(1, parameters.size());
        assertEquals("foo", parameters.get("name").asString());
    }

    @Test
    public void placeholder() {
        Cypher cypher = new Cypher("CREATE (:Foo {")
                .append("name", "bar", "foo")
                .append("})");
        Value parameters = cypher.parameters();
        assertEquals("CREATE (:Foo {name: {bar}})", cypher.statement());
        assertEquals(1, parameters.size());
        assertEquals("foo", parameters.get("bar").asString());
    }

    @Test
    public void backtick() {
        Cypher cypher = new Cypher("CREATE (:Foo {")
                .append("foo-bar", "bar-foo")
                .append("})");
        Value parameters = cypher.parameters();
        assertEquals("CREATE (:Foo {`foo-bar`: {foo_bar}})", cypher.statement());
        assertEquals(1, parameters.size());
        assertEquals("bar-foo", parameters.get("foo_bar").asString());
    }
}