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
 * Copyright 2017-2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.helpers.AuthNodeUserIdentityHelper.getAMIdentity;

import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.service.AMAccountLockout;
import com.sun.identity.idm.AMIdentity;

/**
 * A node that locks a user account (sets the user as "inactive").
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = AccountLockoutNode.Config.class,
        tags = {"risk"})
public class AccountLockoutNode extends SingleOutcomeNode {

    private final Logger logger = LoggerFactory.getLogger(AccountLockoutNode.class);
    private final CoreWrapper coreWrapper;
    private final AMAccountLockout.Factory amAccountLockoutFactory;
    private static final String BUNDLE = AccountLockoutNode.class.getName();
    private final LegacyIdentityService identityService;
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
     * @param identityService An instance of the IdentityService.
     * @param amAccountLockoutFactory factory for generating account lockout objects.
     * @param config The config for this instance.
     */
    @Inject
    public AccountLockoutNode(CoreWrapper coreWrapper, LegacyIdentityService identityService,
            AMAccountLockout.Factory amAccountLockoutFactory, @Assisted Config config) {
        this.coreWrapper = coreWrapper;
        this.identityService = identityService;
        this.config = config;
        this.amAccountLockoutFactory = amAccountLockoutFactory;
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
        NodeState nodeState = context.getStateFor(this);
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

        Optional<AMIdentity> userIdentity =
                getAMIdentity(context.universalId, nodeState, identityService, coreWrapper);
        if (userIdentity.isEmpty()) {
            logger.debug("Could not find the identity with username {} in the realm {}", username, realm);
            throw new NodeProcessException(bundle.getString("identity.not.found"));
        }
        logger.debug("AMIdentity created: {}", userIdentity.get().getUniversalId());

        try {
            AMAccountLockout lockout = amAccountLockoutFactory.create(coreWrapper.realmOf(realm));
            boolean isActive = config.lockAction().isActive();
            logger.debug("Setting user {} as isActive={}", username, isActive);
            lockout.setUserAccountActiveStatus(userIdentity.get(), isActive);
        } catch (RealmLookupException e) {
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
