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
 * Copyright 2019-2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static com.sun.identity.authentication.util.ISAuthConstants.AUTH_TYPE;
import static com.sun.identity.authentication.util.ISAuthConstants.NODE_TYPE;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.inject.Inject;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.SessionUpgradeVerifier;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;

/**
 * A node that enables anonymous sessions to be upgraded.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = AnonymousSessionUpgradeNode.Config.class,
        tags = {"utilities"})
public class AnonymousSessionUpgradeNode extends SingleOutcomeNode {

    /**
     * Node Config Declaration.
     */
    public interface Config {
    }

    private final Realm realm;
    private final Provider<SessionService> sessionServiceProvider;
    private final SessionUpgradeVerifier sessionUpgradeVerifier;
    private final PrivilegedAction<SSOToken> adminTokenAction;
    private final Logger logger = LoggerFactory.getLogger(AnonymousSessionUpgradeNode.class);

    /**
     * Guice constructor.
     * @param realm The realm.
     * @param sessionServiceProvider A provider of {@code SessionService}.
     * @param sessionUpgradeVerifier The SessionUpgradeVerifier instance.
     * @param adminTokenAction The admin token provider.
     */
    @Inject
    public AnonymousSessionUpgradeNode(@Assisted Realm realm, Provider<SessionService> sessionServiceProvider,
            SessionUpgradeVerifier sessionUpgradeVerifier, PrivilegedAction<SSOToken> adminTokenAction) {
        this.realm = realm;
        this.sessionServiceProvider = sessionServiceProvider;
        this.sessionUpgradeVerifier = sessionUpgradeVerifier;
        this.adminTokenAction = adminTokenAction;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("AnonymousSessionUpgradeNode started");
        String authNodes;
        String authModules;
        try {
            SessionID oldSessionID = new SessionID(context.request.ssoTokenId);
            Session oldSession = sessionServiceProvider.get().getSession(oldSessionID);
            authNodes = oldSession.getProperty(NODE_TYPE);
            authModules = oldSession.getProperty(AUTH_TYPE);
        } catch (SessionException e) {
            throw new NodeProcessException("Failed to get previous session", e);
        }

        try {
            SSOToken adminToken = AccessController.doPrivileged(adminTokenAction);
            AMAuthenticationManager authManager = new AMAuthenticationManager(adminToken, realm.asPath());

            if (sessionUpgradeVerifier.isAnonymousViaChain(authManager, authModules)
                    || sessionUpgradeVerifier.isAnonymousViaTree(authNodes)) {
                return goToNext()
                        .addNodeType(context, "AnonymousSessionUpgrade")
                        .build();
            } else {
                return goToNext()
                        .build();
            }
        } catch (AMConfigurationException e) {
            throw new NodeProcessException("Unable to create AMAuthenticationManager instance", e);
        }
    }
}