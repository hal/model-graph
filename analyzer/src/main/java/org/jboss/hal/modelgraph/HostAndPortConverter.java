package org.jboss.hal.modelgraph;

import com.google.common.net.HostAndPort;
import picocli.CommandLine.ITypeConverter;

/**
 * @author Harald Pehl
 */
public class HostAndPortConverter implements ITypeConverter<HostAndPort> {

    @Override
    public HostAndPort convert(String value) {
        return HostAndPort.fromString(value);
    }
}
