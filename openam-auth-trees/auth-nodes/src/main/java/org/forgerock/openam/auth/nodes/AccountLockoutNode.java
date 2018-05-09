/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import javax.inject.Inject;
import java.util.ResourceBundle;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import org.forgerock.openam.core.CoreWrapper;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.iplanet.sso.SSOException;


/**
 * A node that locks a user account (sets the user as "inactive").
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = AccountLockoutNode.Config.class)
public class AccountLockoutNode extends SingleOutcomeNode {


    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final CoreWrapper coreWrapper;
    private static final String BUNDLE = AccountLockoutNode.class.getName().replace(".",
            "/");
    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * Sets the Node to Lock or Unlock accounts.
         * @return the intended lock status.
         */
        @Attribute(order = 100)
        default LockStatus lockAction() {
            return LockStatus.LOCK;
        }
    }


    /**
     * Guice constructor.
     *
     * @param coreWrapper A core wrapper instance.
     * @param config The config for this instance.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public AccountLockoutNode(CoreWrapper coreWrapper, @Assisted Config config) throws NodeProcessException {
        this.coreWrapper = coreWrapper;
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("AccountLockoutNode started");
        lockoutUserAccount(context);
        return goToNext().build();
    }

    /**
     * Sets the current user in the current realm as inactive in the datastore.
     * (both values, username and realm taken from the shared state).
     *
     * @throws NodeProcessException if either of these occur:
     *         1. There is a problem reading either username or realm from the shared config.
     *         2. An error happens when AM is setting the user as inactive.
     */
    private void lockoutUserAccount(TreeContext context) throws NodeProcessException {
        String username = context.sharedState.get(USERNAME).asString();
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        if (username == null || username.isEmpty()) {
            logger.debug("no username specified for lockout");
            throw new NodeProcessException(bundle.getString("context.noname"));
        }
        String realm = context.sharedState.get(REALM).asString();
        if (realm == null || realm.isEmpty()) {
            logger.debug("no realm specified");
            throw new NodeProcessException(bundle.getString("context.norealm"));
        }
        logger.debug("username to lockout {}", username);
        logger.debug("realm {}", realm);

        AMIdentity userIdentity = coreWrapper.getIdentity(username, realm);
        logger.debug("AMIdentity created", userIdentity.getDN());

        try {
            boolean isActive = config.lockAction().isActive();
            logger.debug("Setting user {} as isActive={}", username, isActive);
            userIdentity.setActiveStatus(isActive);
        } catch (IdRepoException | SSOException e) {
            logger.warn("failed to set the user status inactive");
            throw new NodeProcessException(bundle.getString("identity.setstatus.failure"), e);
        }
    }

    /**
     * Captures the state of isActive using a more user friendly LockStatus concept.
     */
    public enum LockStatus {
        /** The lock status. **/
        LOCK(false),
        /** The unlock status. **/
        UNLOCK(true);

        private final boolean isActive;

        LockStatus(boolean isActive) {
            this.isActive = isActive;
        }

        /**
         * Returns true if the status is 'isActive'.
         * @return the status.
         */
        public boolean isActive() {
            return isActive;
        }
    }
}