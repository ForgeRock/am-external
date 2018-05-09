/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

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
import org.forgerock.openam.idrepo.ldap.IdentityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;

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
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Guice constructor.
     * @param coreWrapper A core wrapper instance.
     */
    @Inject
    public DataStoreDecisionNode(CoreWrapper coreWrapper) {
        this.coreWrapper = coreWrapper;
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
        try {
            logger.debug("authenticating {} ", nameCallback.getName());
            success = idrepo.authenticate(callbacks);
            boolean isActive = coreWrapper.getIdentity(nameCallback.getName(),
                    context.sharedState.get(REALM).asString()).isActive();
            success = success && isActive;
        } catch (InvalidPasswordException e) {
            logger.warn("invalid password error");
            // Ignore. Success is already false!
        } catch (IdentityNotFoundException e) {
            logger.warn("invalid username error");
            newState.remove(USERNAME);
        } catch (IdRepoException | AuthLoginException e) {
            logger.warn("Exception in data store decision node");
            throw new NodeProcessException(e);
        } catch (SSOException e) {
            logger.warn("Exception checking user status");
            throw new NodeProcessException(e);
        }
        return goTo(success).replaceSharedState(newState)
                .replaceTransientState(newTransientState).build();
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
