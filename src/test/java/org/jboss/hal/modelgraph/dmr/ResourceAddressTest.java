package org.jboss.hal.modelgraph.dmr;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Harald Pehl
 */
public class ResourceAddressTest {

    @Test
    public void of() {
        assertEquals("/", ResourceAddress.of(null).toString());
        assertEquals("/", ResourceAddress.of("").toString());
        assertEquals("/", ResourceAddress.of("/").toString());
        assertEquals("/foo=bar", ResourceAddress.of("/foo=bar").toString());
        assertEquals("/subsystem=datasources/data-source=*",
                ResourceAddress.of("/subsystem=datasources/data-source=*").toString());
    }

    @Test
    public void add() {
        ResourceAddress a1 = ResourceAddress.of("/");
        ResourceAddress a2 = a1.add("foo=bar/baz=qux");
        assertNotSame(a1, a2);
        assertEquals("/", a1.toString());
        assertEquals("/foo=bar/baz=qux", a2.toString());
    }

    @Test
    public void getName() {
        ResourceAddress a1 = ResourceAddress.of("/foo=*");
        ResourceAddress a2 = a1.add("foo=bar");
        assertEquals("foo", a1.getName());
        assertEquals("foo=bar", a2.getName());
    }

    @Test
    public void isSingleton() {
        ResourceAddress a1 = ResourceAddress.of("/foo=*");
        ResourceAddress a2 = a1.add("foo=bar");
        assertFalse("foo", a1.isSingleton());
        assertTrue("/foo=bar", a2.isSingleton());
    }

    @Test
    public void size() {
        assertEquals(0, ResourceAddress.of("/").size());
        assertEquals(1, ResourceAddress.of("/foo=bar").size());
        assertEquals(2, ResourceAddress.of("/foo=bar/baz=qux").size());
    }

}