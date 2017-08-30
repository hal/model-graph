package org.jboss.hal.modelgraph.dmr;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

import com.google.common.net.HostAndPort;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.hal.modelgraph.dmr.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class WildFlyClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(WildFlyClient.class);

    private final ModelControllerClient mcc;

    public WildFlyClient(HostAndPort hostAndPort, String username, String password)
            throws UnknownHostException {
        mcc = ModelControllerClient.Factory.create(InetAddress.getByName(hostAndPort.getHost()), hostAndPort.getPort(),
                callbacks -> {
                    for (Callback current : callbacks) {
                        if (current instanceof NameCallback) {
                            NameCallback ncb = (NameCallback) current;
                            ncb.setName(username);
                        } else if (current instanceof PasswordCallback) {
                            PasswordCallback pcb = (PasswordCallback) current;
                            pcb.setPassword(password.toCharArray());
                        } else if (current instanceof RealmCallback) {
                            RealmCallback rcb = (RealmCallback) current;
                            rcb.setText(rcb.getDefaultText());
                        } else {
                            throw new UnsupportedCallbackException(current);
                        }
                    }
                });
        logger.info("Connected to WildFly instance at {}", hostAndPort);
    }

    public ModelNode execute(Operation operation) {
        ModelNode result = new ModelNode();
        try {
            ModelNode modelNode = mcc.execute(operation);
            if (modelNode.hasDefined(OUTCOME)) {
                String outcome = modelNode.get(OUTCOME).asString();
                if (SUCCESS.equals(outcome)) {
                    if (modelNode.hasDefined(RESULT)) {
                        result = modelNode.get(RESULT);
                    }
                } else if (FAILED.equals(outcome)) {
                    if (modelNode.hasDefined(FAILURE_DESCRIPTION)) {
                        String error = modelNode.get(FAILURE_DESCRIPTION).asString();
                        logger.error("Unable to execute {}: {}", operation.asCli(), error);
                    }
                } else {
                    logger.error("Unable to execute {}: Unknown outcome {}", operation.asCli(), outcome);
                }
            } else {
                logger.error("Unable to execute {}: No outcome", operation.asCli());
            }
        } catch (IOException e) {
            logger.error("Unable to execute {}: {}", operation.asCli(), e.getMessage());
        }
        return result;
    }

    @Override
    public void close() throws Exception {
        logger.debug("Closing connection to WildFly instance");
        mcc.close();
    }
}
