package org.jboss.hal.modelgraph;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import com.google.common.net.HostAndPort;

/**
 * @author Harald Pehl
 */
public class HostAndPortValidator implements IParameterValidator {

    @Override
    public void validate(final String name, final String value) throws ParameterException {
        try {
            HostAndPort.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Illegal value for parameter " + name + ": " + e.getMessage());
        }
    }
}
