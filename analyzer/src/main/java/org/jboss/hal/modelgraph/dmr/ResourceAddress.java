/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.hal.modelgraph.dmr;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/** Represents a fully qualified DMR address ready to be put into a DMR operation. */
public class ResourceAddress extends ModelNode {

    public static ResourceAddress of(final String address) {
        if (!Strings.isNullOrEmpty(address) && !"/".equals(address)) {
            ModelNode node = new ModelNode();
            String normalized = address.startsWith("/") ? address.substring(1) : address;
            Splitter.on('/').withKeyValueSeparator('=').split(normalized)
                    .forEach((key, value) -> node.add().set(key, value));
            return new ResourceAddress(node);
        }
        return new ResourceAddress();
    }

    private ResourceAddress() {
        setEmptyList();
    }

    private ResourceAddress(ModelNode address) {
        set(address);
    }

    public ResourceAddress add(final String segment) {
        ResourceAddress address = new ResourceAddress(this);
        if (segment != null) {
            List<String> parts = Splitter.on('=').limit(2).splitToList(segment);
            if (parts.size() == 1) {
                address.add().set(parts.get(0), "*");
            } else if (parts.size() == 2) {
                address.add().set(parts.get(0), parts.get(1));
            }
        }
        return address;
    }

    public String getName() {
        if (size() == 0) {
            return "/";
        } else if (lastName() != null && lastValue() != null) {
            if ("*".equals(lastValue())) {
                return lastName();
            } else {
                return lastName() + "=" + lastValue();
            }
        } else {
            return "n/a";
        }
    }

    public int size() {
        return isDefined() ? asList().size() : 0;
    }

    public boolean isSingleton() {
        return !(size() == 0 || "*".equals(lastValue()));
    }

    @Override
    public String toString() {
        // Do not change implementation, it's used in neo4j!
        StringBuilder builder = new StringBuilder();
        if (isDefined()) {
            builder.append("/");
            for (Iterator<Property> iterator = asPropertyList().iterator(); iterator.hasNext(); ) {
                Property segment = iterator.next();
                builder.append(segment.getName()).append("=").append(segment.getValue().asString());
                if (iterator.hasNext()) {
                    builder.append("/");
                }
            }
        } else {
            builder.append("n/a");
        }
        return builder.toString();
    }

    private String lastName() {
        List<Property> properties = asPropertyList();
        if (!properties.isEmpty()) {
            return properties.get(properties.size() - 1).getName();
        }
        return null;
    }

    private String lastValue() {
        List<Property> properties = asPropertyList();
        if (!properties.isEmpty()) {
            return properties.get(properties.size() - 1).getValue().asString();
        }
        return null;
    }
}
