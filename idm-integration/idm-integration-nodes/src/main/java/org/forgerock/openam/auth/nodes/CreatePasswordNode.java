/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static javax.security.auth.callback.TextOutputCallback.ERROR;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.utils.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that prompt the user to create a password.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = CreatePasswordNode.Config.class,
        tags = {"identity management"})
public class CreatePasswordNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger(CreatePasswordNode.class);
    @VisibleForTesting
    static final String BUNDLE = CreatePasswordNode.class.getName();
    private final Config config;
    private List<Callback> passwordCallbacks;
    private ResourceBundle bundle;

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
        bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        initialiseCallbacks();

        List<PasswordCallback> callbacks = context.getCallbacks(PasswordCallback.class);
        if (isEmpty(callbacks)) {
            return send(passwordCallbacks).build();
        }

        PasswordPair passwords = getPasswords(callbacks);
        if (!checkPassword(passwords)) {
            return send(passwordCallbacks).build();
        }

        return goToNext()
                .replaceTransientState(context.transientState.copy().put(PASSWORD, passwords.password))
                .build();
    }

    private void initialiseCallbacks() {
        passwordCallbacks = new ArrayList<>();
        passwordCallbacks.add(new PasswordCallback(bundle.getString("callback.password"), false));
        passwordCallbacks.add(new PasswordCallback(bundle.getString("callback.password.confirm"), false));
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
            passwordCallbacks.add(getErrorCallback(String.format(bundle.getString("error.password.length"),
                    config.minPasswordLength())));
            return false;
        } else if (!passwords.password.equals(passwords.confirmPassword)) {
            passwordCallbacks.add(getErrorCallback(bundle.getString("error.password.mismatch")));
            return false;
        }
        return true;
    }

    private TextOutputCallback getErrorCallback(String message) {
        return new TextOutputCallback(ERROR, message);
    }

    private static class PasswordPair {
        final String password;
        final String confirmPassword;

        PasswordPair(String password, String confirmPassword) {
            this.password = password;
            this.confirmPassword = confirmPassword;
        }
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(PASSWORD)
        };
    }
}
