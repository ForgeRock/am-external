/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
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
        configClass = SocialOAuthIgnoreProfileNode.Config.class)
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
}
