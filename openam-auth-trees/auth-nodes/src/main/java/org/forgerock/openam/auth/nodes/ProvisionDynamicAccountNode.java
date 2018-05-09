/*
 *  Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 *  Use of this code requires a commercial software license with ForgeRock AS.
 *  or with one of its affiliates. All use shall be exclusively subject
 *  to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.*;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;
import org.forgerock.openam.core.realms.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Provisions a user identity using a generated password.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = ProvisionDynamicAccountNode.Config.class)
public class ProvisionDynamicAccountNode extends SingleOutcomeNode {
    private static final Logger logger = LoggerFactory.getLogger("amAuth");
    static final String USER_STATUS = "inetuserstatus";
    static final String USER_PASSWORD = "userPassword";

    static final String ACTIVE = "Active";
    private final ProvisionDynamicAccountNode.Config config;
    private final SocialOAuth2Helper authModuleHelper;
    private final Realm realm;

    /**
     * The interface Config.
     */
    public interface Config {

        /**
         * Account provider class.
         *
         * @return the string
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String accountProviderClass() {
            return "org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider";
        }
    }

    /**
     * Constructs a new {@link ProvisionDynamicAccountNode} with the
     * provided {@link ProvisionDynamicAccountNode.Config}.
     *
     * @param config provides the settings for initialising an {@link ProvisionDynamicAccountNode}.
     * @param realm The realm context for the authentication.
     * @param authModuleHelper Helper class for oauth2 authentication.
     * @throws NodeProcessException if there is a problem during construction.
     */
    @Inject
    public ProvisionDynamicAccountNode(@Assisted ProvisionDynamicAccountNode.Config config,
            @Assisted Realm realm,
            SocialOAuth2Helper authModuleHelper) {
        this.config = config;
        this.realm = realm;
        this.authModuleHelper = authModuleHelper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ProvisionAccount node started");

        checkIfUserNamesAreInSharedSession(context.sharedState);
        return goToNext()
                .replaceSharedState(context.sharedState.put(USERNAME, provisionAccount(context)))
                .build();
    }

    private void checkIfUserNamesAreInSharedSession(JsonValue sharedState) throws NodeProcessException {
        if (!sharedState.isDefined(USER_INFO_SHARED_STATE_KEY)) {
            throw new NodeProcessException("No user information has been found in the shared state. You must call a "
                    + "node that sets this information first");
        }

        if (!sharedState.get(USER_INFO_SHARED_STATE_KEY).isDefined(USER_NAMES_SHARED_STATE_KEY)) {
            throw new NodeProcessException("The user information doesn't contain the userNames. You must call a "
                    + "node that sets this information first");
        }
    }

    private String provisionAccount(TreeContext context) throws NodeProcessException {
        Map<String, Set<String>> userAttributes = SocialOAuth2Helper.convertMapOfListToMapOfSet(context.sharedState
                .get(USER_INFO_SHARED_STATE_KEY)
                .get("attributes")
                .asMapOfList(String.class));
        userAttributes.put(USER_PASSWORD, Collections.singleton(getUserPassword(context)));
        userAttributes.put(USER_STATUS, Collections.singleton(ACTIVE));

        try {
            String user = authModuleHelper.provisionUser(
                    realm.asPath(),
                    OAuthUtil.instantiateAccountProvider(config.accountProviderClass()),
                    userAttributes);

            if (user == null) {
                throw new NodeProcessException("Unable to create user");
            }
            logger.debug("User created: " + user);
            return user;
        } catch (AuthLoginException e) {
            throw new NodeProcessException(e);
        }
    }

    private String getUserPassword(TreeContext context) throws NodeProcessException {
        return context.transientState.isDefined(PASSWORD)
                ? context.transientState.get(PASSWORD).asString()
                : authModuleHelper.getRandomData();
    }
}