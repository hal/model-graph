package org.jboss.hal.modelgraph;

import com.beust.jcommander.IStringConverter;
import com.google.common.net.HostAndPort;

/**
 * @author Harald Pehl
 */
public class HostAndPortConverter implements IStringConverter<HostAndPort> {

    @Override
    public HostAndPort convert(final String value) {
        return HostAndPort.fromString(value);
    }
}
