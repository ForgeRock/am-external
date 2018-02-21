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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Arrays.asList;
import static org.forgerock.openam.core.realms.Realm.root;

import javax.inject.Inject;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Core nodes installed by default with no engine dependencies.
 */
public class NodesPlugin extends AbstractNodeAmPlugin {

    /** The ID of the Zero Page Login node that is used by the default tree. */
    public static final String ZERO_PAGE_NODE_ID = "70052da0-ef9e-4767-b27e-df831189c9f0";

    /** The ID of the Username Collector node that is used by the default tree. */
    public static final String USERNAME_NODE_ID = "92c44010-e1e9-4a4a-8a16-8278d661d68d";

    /** The ID of the Password Collector node that is used by the default tree. */
    public static final String PASSWORD_NODE_ID = "4d46c420-999c-435a-ae34-0eca29876fe2";

    /** The ID of the Data Store Decision node that is used by the default tree. */
    public static final String DATA_STORE_NODE_ID = "e5ec495a-2ae2-4eca-8afb-9781dea04170";

    private final AnnotatedServiceRegistry serviceRegistry;

    /**
     * DI-enabled constructor.
     * @param serviceRegistry A service registry instance.
     */
    @Inject
    public NodesPlugin(AnnotatedServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public String getPluginVersion() {
        return "1.0.0";
    }

    @Override
    protected Iterable<? extends Class<? extends Node>> getNodes() {
        return asList(
                AuthLevelDecisionNode.class,
                DataStoreDecisionNode.class,
                PasswordCollectorNode.class,
                RemoveSessionPropertiesNode.class,
                SetSessionPropertiesNode.class,
                ScriptedDecisionNode.class,
                UsernameCollectorNode.class,
                ZeroPageLoginNode.class
        );
    }

    @Override
    public void onInstall() throws PluginException {
        super.onInstall();
        try {
            serviceRegistry.createRealmInstance(root(), ZERO_PAGE_NODE_ID, new ZeroPageLoginNode.Config() { });
            serviceRegistry.createRealmInstance(root(), USERNAME_NODE_ID, new UsernameCollectorNode.Config() { });
            serviceRegistry.createRealmInstance(root(), PASSWORD_NODE_ID, new PasswordCollectorNode.Config() { });
            serviceRegistry.createRealmInstance(root(), DATA_STORE_NODE_ID, new DataStoreDecisionNode.Config() { });
        } catch (SSOException | SMSException e) {
            throw new PluginException("Could not create default instances of nodes", e);
        }
    }
}
