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

import static java.util.Collections.emptyList;
import static org.forgerock.am.trees.api.Outcome.TRUE;

import java.util.List;

import javax.security.auth.callback.Callback;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeExecutor;
import org.forgerock.am.trees.model.TreeResult;
import org.forgerock.am.trees.model.TreeStateFactory;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder;

/**
 * An {@link AuthenticationStrategy} that uses trees to perform user authentication.
 */
class TreeAuthenticationStrategy implements AuthenticationStrategy {

    private final TreeExecutor treeExecutor;
    private final Realm realm;
    private final Tree authTree;

    /**
     * Constructs a new {@code TreeAuthenticationStrategy}.
     *
     * @param treeExecutor the {@link TreeExecutor} to execute the authentication {@link Tree}
     * @param realm the {@link Realm} in which the authentication tree is defined
     * @param authTree the authentication {@link Tree}
     */
    TreeAuthenticationStrategy(TreeExecutor treeExecutor, Realm realm, Tree authTree) {
        this.treeExecutor = treeExecutor;
        this.realm = realm;
        this.authTree = authTree;
    }

    @Override
    public boolean startAuthentication(ContextHolder holder, String clientIPAddress) {
        ExternalRequestContext.Builder externalRequestContextBuilder = new ExternalRequestContext.Builder();
        if (clientIPAddress != null) {
            externalRequestContextBuilder = externalRequestContextBuilder.clientIp(clientIPAddress);
        }
        ExternalRequestContext requestContext = externalRequestContextBuilder.build();
        TreeResult result = treeExecutor.process(realm, authTree, TreeStateFactory.createInitial(realm, null, null, 1),
                emptyList(), false, requestContext);
        holder.setRequestContext(requestContext);
        holder.setTreeResult(result);
        return true;
    }

    @Override
    public Callback[] getNextCallbacks(ContextHolder holder) {
        TreeResult result = holder.getTreeResult();
        if (result == null) {
            return new Callback[0];
        }
        List<Callback> callbacks = result.callbacks;
        return callbacks != null ? callbacks.toArray(new Callback[0]) : new Callback[0];
    }

    @Override
    public void submitCallbacks(ContextHolder holder, Callback[] callbacks) {
        TreeResult current = holder.getTreeResult();
        if (current == null) {
            return;
        }
        TreeResult result = treeExecutor.process(realm, authTree, current.treeState,
                List.of(callbacks), false, holder.getRequestContext());
        holder.setTreeResult(result);
    }

    @Override
    public boolean isAuthenticationSuccessful(ContextHolder holder) {
        TreeResult result = holder.getTreeResult();
        return result != null && TRUE.equals(result.outcome);
    }

    @Override
    public boolean terminateAuthentication(ContextHolder holder) {
        holder.setTreeResult(null);
        holder.setRequestContext(null);
        return true;
    }

    @Override
    public String getUniversalId(ContextHolder holder) {
        TreeResult result = holder.getTreeResult();
        return result != null ? result.treeState.universalId().orElse(null) : null;
    }
}
