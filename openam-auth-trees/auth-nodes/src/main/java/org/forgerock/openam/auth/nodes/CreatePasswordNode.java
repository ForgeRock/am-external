/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.utils.CollectionUtils.isEmpty;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.security.auth.callback.PasswordCallback;

import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that prompt the user to create a password.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = CreatePasswordNode.Config.class)
public class CreatePasswordNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/CreatePasswordNode";
    private final Config config;

    /**
     * Node configuration.
     */
    public interface Config {

        /**
         * The length of the password.
         *
         * @return the length
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default int minPasswordLength() {
            return 8;
        }
    }

    /**
     * Constructor.
     *
     * @param config the config
     */
    @Inject
    public CreatePasswordNode(@Assisted CreatePasswordNode.Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("CreatePasswordNode started");

        List<PasswordCallback> callbacks = context.getCallbacks(PasswordCallback.class);

        if (isEmpty(callbacks)) {
            return collectPassword(context);
        }

        PasswordPair passwords = getPasswords(callbacks);
        if (!checkPassword(passwords)) {
            return collectPassword(context);
        }

        return goToNext()
                .replaceTransientState(context.transientState.copy().put(PASSWORD, passwords.password))
                .build();
    }

    private PasswordPair getPasswords(List<PasswordCallback> callbacks) throws NodeProcessException {
        List<String> passwords = callbacks.stream()
                .map(PasswordCallback::getPassword)
                .map(String::new)
                .collect(Collectors.toList());

        if (passwords.size() != 2) {
            throw new NodeProcessException("There should be 2 PasswordCallback and " + passwords.size()
                    + " has been found");
        }
        return new PasswordPair(passwords.get(0), passwords.get(1));
    }

    private boolean checkPassword(PasswordPair passwords) {
        if (StringUtils.isBlank(passwords.password)) {
            return false;
        } else if (passwords.password.length() < config.minPasswordLength()) {
            return false;
        } else if (!passwords.password.equals(passwords.confirmPassword)) {
            return false;
        }
        return true;
    }

    private Action collectPassword(TreeContext context) {
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        return send(
                new PasswordCallback(bundle.getString("callback.password"), false),
                new PasswordCallback(bundle.getString("callback.password.confirm"), false)
        ).build();
    }

    private static class PasswordPair {
        final String password;
        final String confirmPassword;

        PasswordPair(String password, String confirmPassword) {
            this.password = password;
            this.confirmPassword = confirmPassword;
        }
    }
}
