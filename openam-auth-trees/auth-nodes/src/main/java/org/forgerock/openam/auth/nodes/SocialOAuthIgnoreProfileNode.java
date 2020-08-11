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
 * Copyright 2018-2019 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.USER_INFO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.spi.AuthLoginException;

/**
 * This node must be used in pair with the {@code AbstractSocialAuthLoginNode} to obtain a session without
 * linking it to a profile.
 *
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = SocialOAuthIgnoreProfileNode.Config.class,
        tags = {"social", "federation"})
public class SocialOAuthIgnoreProfileNode extends SingleOutcomeNode {

    /**
     * config for the node.
     */
    public interface Config {
        //no config needed
    }

    /**
     * Constructor.
     * @param config the config
     */
    @Inject
    public SocialOAuthIgnoreProfileNode(@Assisted SocialOAuthIgnoreProfileNode.Config config) {
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        try {
            return loginSuccess(context.sharedState);
        } catch (LoginException e) {
            throw new NodeProcessException(e);
        }
    }

    private Action loginSuccess(JsonValue sharedState) throws AuthLoginException {
        checkIfUserNamesAreInSharedSession(sharedState);

        String user = getMappedUsername(sharedState.get("userInfo").get("userNames").asMapOfList(String.class));
        return goToNext()
                .replaceSharedState(sharedState.add(SharedStateConstants.USERNAME, user))
                .build();
    }

    private void checkIfUserNamesAreInSharedSession(JsonValue sharedState) throws AuthLoginException {
        if (!sharedState.isDefined("userInfo")) {
            throw new AuthLoginException("No user information has been found in the shared state. You must call a "
                    + "node that sets this information first");
        }

        if (!sharedState.get("userInfo").isDefined("userNames")) {
            throw new AuthLoginException("The user information doesn't contain the userNames. You must call a "
                    + "node that sets this information first");
        }
    }

    private String getMappedUsername(Map<String, List<String>> userNames) throws AuthLoginException {
        return userNames.values().stream()
                .flatMap(Collection::stream)
                .findFirst()
                .orElseThrow(() -> new AuthLoginException("Username not found in the mapped attributes"));
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(USER_INFO)
        };
    }
}
