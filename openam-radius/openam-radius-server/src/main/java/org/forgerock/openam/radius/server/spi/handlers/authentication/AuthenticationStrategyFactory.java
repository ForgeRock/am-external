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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server.spi.handlers.authentication;

import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import org.forgerock.am.trees.api.TreeExecutor;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.radius.server.InvalidRadiusClientConfigurationException;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.OpenAMAuthFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

/**
 * Factory to create {@link AuthenticationStrategy} instances.
 */
public class AuthenticationStrategyFactory {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationStrategyFactory.class);

    private final TreeProvider treeProvider;
    private final TreeExecutor treeExecutor;
    private final OpenAMAuthFactory amAuthFactory;
    private final RealmLookup realmLookup;

    /**
     * Constructs a new {@code AuthenticationStrategyFactory}.
     *
     * @param treeProvider the {@link TreeProvider} to find authentication trees
     * @param treeExecutor the {@link TreeExecutor} to execute authentication trees
     * @param amAuthFactory the {@link OpenAMAuthFactory} used by {@link ChainAuthenticationStrategy}
     * @param realmLookup the {@link RealmLookup} to find realms
     */
    @Inject
    public AuthenticationStrategyFactory(TreeProvider treeProvider, TreeExecutor treeExecutor,
            OpenAMAuthFactory amAuthFactory, RealmLookup realmLookup) {
        this.treeProvider = treeProvider;
        this.treeExecutor = treeExecutor;
        this.amAuthFactory = amAuthFactory;
        this.realmLookup = realmLookup;
    }

    /**
     * Creates a new instance of an {@link AuthenticationStrategy}.
     *
     * @param realmName the name of the realm in which the tree or chain is defined
     * @param treeName the tree name
     * @param chainName the chain name
     * @return an {@link AuthenticationStrategy}
     */
    public AuthenticationStrategy create(String realmName, String treeName, String chainName)
            throws InvalidRadiusClientConfigurationException {
        if (isBlank(realmName)) {
            logger.error("AuthenticationStrategyFactory: Missing realm configuration.");
            throw new InvalidRadiusClientConfigurationException("Missing realm configuration.");
        }

        if (isNotBlank(treeName)) {
            try {
                Realm realm = realmLookup.lookup(realmName);
                return new TreeAuthenticationStrategy(treeExecutor, realm,
                        treeProvider.getTree(realm, treeName).orElseThrow(
                                () -> new IllegalArgumentException("No tree found with name " + treeName)
                        ));
            } catch (RealmLookupException | IllegalArgumentException e) {
                logger.error("AuthenticationStrategyFactory: Unable to find tree: {} in realm: {}",
                        treeName, realmName, e);
                throw new InvalidRadiusClientConfigurationException(
                        "Invalid tree '%s' in realm '%s'".formatted(treeName, realmName), e);
            }
        }
        if (isNotBlank(chainName)) {
            return new ChainAuthenticationStrategy(amAuthFactory, realmName, chainName);
        }
        logger.error("AuthenticationStrategyFactory: No tree or chain configured for realm: {}", realmName);
        throw new InvalidRadiusClientConfigurationException("No authentication service configured.");
    }
}
