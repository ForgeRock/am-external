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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static com.sun.identity.idm.IdType.USER;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.security.PrivilegedAction;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.am.identity.persistence.IdentityStore;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.idrepo.ldap.IdentityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * A node that decides if the username and password exists in the data store.
 *
 * <p>Expects 'username' and 'password' fields to be present in the shared state.</p>
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = DataStoreDecisionNode.Config.class,
        tags = {"basic authn", "basic authentication"})
public class DataStoreDecisionNode extends AbstractDecisionNode {

    /**
     * Configuration for the data store node.
     */
    public interface Config {
    }

    private final CoreWrapper coreWrapper;
    private final LegacyIdentityService identityService;
    private final Provider<PrivilegedAction<SSOToken>> adminTokenActionProvider;
    private final Logger logger = LoggerFactory.getLogger(DataStoreDecisionNode.class);
    private static final String BUNDLE = DataStoreDecisionNode.class.getName();

    /**
     * Guice constructor.
     * @param coreWrapper A core wrapper instance.
     * @param identityService A {@link LegacyIdentityService} instance.
     * @param adminTokenActionProvider A provider for an {@code SSOToken}.
     */
    @Inject
    public DataStoreDecisionNode(CoreWrapper coreWrapper, LegacyIdentityService identityService,
             Provider<PrivilegedAction<SSOToken>> adminTokenActionProvider) {
        this.coreWrapper = coreWrapper;
        this.identityService = identityService;
        this.adminTokenActionProvider = adminTokenActionProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("DataStoreDecisionNode started");
        final String realm = context.sharedState.get(REALM).asString();
        IdentityStore identityStore = coreWrapper.getIdentityRepository(realm);
        logger.debug("IdentityRepository claimed");

        NameCallback nameCallback = new NameCallback("notused");
        final String username = context.sharedState.get(USERNAME).asString();
        nameCallback.setName(username);
        PasswordCallback passwordCallback = new PasswordCallback("notused", false);
        passwordCallback.setPassword(getPassword(context, username));
        logger.debug("NameCallback and PasswordCallback set");
        Callback[] callbacks = new Callback[]{nameCallback, passwordCallback};
        Action.ActionBuilder action = goTo(true);
        if (username != null) {
            action.withIdentifiedIdentity(username, getIdentityType());
        }
        try {
            logger.debug("authenticating {} ", nameCallback.getName());
            if (String.valueOf(passwordCallback.getPassword()).isBlank()) {
                logger.debug("password is blank outcome is false for user {} ", nameCallback.getName());
                action = goTo(false);
            } else {
                boolean isAuthenticationFailed = !identityStore.authenticate(getIdentityType(), callbacks);
                if (isAuthenticationFailed) {
                    action = goTo(false);
                }
            }
        } catch (InvalidPasswordException e) {
            logger.warn("invalid password error");
            action = goTo(false);
        } catch (IdentityNotFoundException e) {
            logger.warn("invalid username error");
            action = goTo(false);
        } catch (IdRepoException | AuthLoginException e) {
            logError(e, "Exception in data store decision node");
        }
        return action
                .withUniversalId(identityService.getUniversalId(username, realm, getIdentityType()))
                .replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy()).build();
    }

    private void logError(Exception e, String logMessage) throws NodeProcessException {
        logger.warn(logMessage);
        throw new NodeProcessException(e);
    }

    IdType getIdentityType() {
        return USER;
    }

    private char[] getPassword(TreeContext context, String username) throws NodeProcessException {
        String password = context.getState(PASSWORD).asString();
        if (password == null) {
            logger.error("Password is null for username {} ", username);
            throw new NodeProcessException("Unable to authenticate");
        }
        return password.toCharArray();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(REALM),
            new InputState(USERNAME),
            new InputState(PASSWORD)
        };
    }
}
