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

    interface Config {
    }

    private final CoreWrapper coreWrapper;

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
        AMIdentityRepository idrepo = coreWrapper.getAMIdentityRepository(
                coreWrapper.convertRealmNameToOrgName(context.sharedState.get(REALM).asString()));

        NameCallback nameCallback = new NameCallback("notused");
        nameCallback.setName(context.sharedState.get(USERNAME).asString());
        PasswordCallback passwordCallback = new PasswordCallback("notused", false);
        passwordCallback.setPassword(context.sharedState.get(PASSWORD).asString().toCharArray());
        Callback[] callbacks = new Callback[]{nameCallback, passwordCallback};
        boolean success = false;
        JsonValue newState = context.sharedState.copy();
        try {
            success = idrepo.authenticate(callbacks);
        } catch (InvalidPasswordException e) {
            // Ignore. Success is already false!
        } catch (IdentityNotFoundException e) {
            newState.remove(USERNAME);
        } catch (IdRepoException | AuthLoginException e) {
            throw new NodeProcessException(e);
        }
        newState.remove(PASSWORD);
        return goTo(success).replaceSharedState(newState).build();
    }
}
