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
 * Copyright 2017-2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.idrepo.ldap.IdentityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * A node that decides if the username and password exists in the data store.
 *
 * <p>Expects 'username' and 'password' fields to be present in the shared state.</p>
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = DataStoreDecisionNode.Config.class)
public class DataStoreDecisionNode extends AbstractDecisionNode {

    /**
     * Configuration for the data store node.
     */
    public interface Config {
    }

    private final CoreWrapper coreWrapper;
    private final IdentityUtils identityUtils;
    private final Provider<PrivilegedAction<SSOToken>> adminTokenActionProvider;
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String BUNDLE = DataStoreDecisionNode.class.getName();

    /**
     * Guice constructor.
     * @param coreWrapper A core wrapper instance.
     * @param identityUtils A {@code IdentityUtils} instance.
     * @param adminTokenActionProvider A provider for an {@code SSOToken}.
     */
    @Inject
    public DataStoreDecisionNode(CoreWrapper coreWrapper, IdentityUtils identityUtils,
             Provider<PrivilegedAction<SSOToken>> adminTokenActionProvider) {
        this.coreWrapper = coreWrapper;
        this.identityUtils = identityUtils;
        this.adminTokenActionProvider = adminTokenActionProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("DataStoreDecisionNode started");
        AMIdentityRepository idrepo = coreWrapper.getAMIdentityRepository(
                coreWrapper.convertRealmPathToRealmDn(context.sharedState.get(REALM).asString()));
        logger.debug("AMIdentityRepository claimed");

        NameCallback nameCallback = new NameCallback("notused");
        nameCallback.setName(context.sharedState.get(USERNAME).asString());
        PasswordCallback passwordCallback = new PasswordCallback("notused", false);
        passwordCallback.setPassword(getPassword(context));
        logger.debug("NameCallback and PasswordCallback set");
        Callback[] callbacks = new Callback[]{nameCallback, passwordCallback};
        boolean success = false;
        JsonValue newState = context.sharedState.copy();
        JsonValue newTransientState = context.transientState.copy();
        Action.ActionBuilder action = goTo(true);
        ResourceBundle bundle = context.request.locales
                .getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        try {
            logger.debug("authenticating {} ", nameCallback.getName());
            boolean isAuthenticationFailed = !idrepo.authenticate(getIdentityType(), callbacks);
            if (isAuthenticationFailed) {
                action = goTo(false).withErrorMessage(bundle.getString("authenticationFailed"));
            } else if (!isActive(context, nameCallback)) {
                action = goTo(false).withErrorMessage(bundle.getString("accountLocked"));
                logger.warn("Account is not active");
            }
        } catch (InvalidPasswordException e) {
            logger.warn("invalid password error");
            try {
                if (!isActive(context, nameCallback)) {
                    logger.warn("Account is not active");
                    action = goTo(false).withErrorMessage(bundle.getString("accountLocked"));
                } else {
                    action = goTo(false).withErrorMessage(bundle.getString("authenticationFailed"));
                }
            } catch (IdRepoException ex) {
                logWarningAndThrow(e, "Exception in data store decision node");
            } catch (SSOException ex) {
                logWarningAndThrow(e, "Exception checking user status");
            }
        } catch (IdentityNotFoundException e) {
            logger.warn("invalid username error");
            action = goTo(false).withErrorMessage(bundle.getString("authenticationFailed"));
        } catch (IdRepoException | AuthLoginException e) {
            logWarningAndThrow(e, "Exception in data store decision node");
        } catch (SSOException e) {
            logWarningAndThrow(e, "Exception checking user status");
        }
        return action.replaceSharedState(newState)
                .replaceTransientState(newTransientState).build();
    }

    private void logWarningAndThrow(Exception e, String logMessage) throws NodeProcessException {
        logger.warn(logMessage);
        throw new NodeProcessException(e);
    }

    private boolean isActive(TreeContext context, NameCallback nameCallback) throws IdRepoException, SSOException {
        SSOToken token = AccessController.doPrivileged(adminTokenActionProvider.get());
        return identityUtils.getAmIdentity(token, nameCallback.getName(), getIdentityType(),
                context.sharedState.get(REALM).asString()).isActive();
    }

    IdType getIdentityType() {
        return IdType.USER;
    }

    private char[] getPassword(TreeContext context) throws NodeProcessException {
        String password =  context.transientState.get(PASSWORD).asString();
        if (password == null) {
            logger.error("Password is null, note this field is not stored across multiple requests");
            throw new NodeProcessException("Unable to authenticate");
        }
        return password.toCharArray();
    }
}
