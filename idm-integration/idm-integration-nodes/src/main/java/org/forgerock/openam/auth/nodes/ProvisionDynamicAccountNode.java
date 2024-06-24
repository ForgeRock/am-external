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
 * Copyright 2018-2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static com.sun.identity.idm.IdType.USER;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toMap;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.ATTRIBUTES_SHARED_STATE_KEY;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_INFO_SHARED_STATE_KEY;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_NAMES_SHARED_STATE_KEY;

import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
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
        configClass = ProvisionDynamicAccountNode.Config.class,
        tags = {"social", "federation"})
public class ProvisionDynamicAccountNode extends SingleOutcomeNode {
    private static final Logger logger = LoggerFactory.getLogger(ProvisionDynamicAccountNode.class);
    static final String USER_STATUS = "inetuserstatus";
    static final String USER_PASSWORD = "userPassword";

    static final String ACTIVE = "Active";
    private final ProvisionDynamicAccountNode.Config config;
    private final SocialOAuth2Helper authModuleHelper;
    private final LegacyIdentityService identityService;
    private final IdmIntegrationService idmIntegrationService;
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
     * @param identityService An instance of the IdentityService.
     * @param idmIntegrationService An instance of the IdmIntegrationService.
     */
    @Inject
    public ProvisionDynamicAccountNode(@Assisted ProvisionDynamicAccountNode.Config config, @Assisted Realm realm,
            SocialOAuth2Helper authModuleHelper, LegacyIdentityService identityService,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.authModuleHelper = authModuleHelper;
        this.identityService = identityService;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ProvisionAccount node started");

        Map<String, Set<String>> userAttributes = retrieveUserAttributesFromState(context);
        String username = provisionAccount(context, userAttributes);
        String realm = context.sharedState.get(REALM).asString();
        return goToNext()
                .withUniversalId(identityService.getUniversalId(username, realm, USER))
                .withIdentifiedIdentity(username, USER)
                .replaceSharedState(context.sharedState.put(USERNAME, username))
                .build();
    }

    private Map<String, Set<String>> retrieveUserAttributesFromState(TreeContext context)
            throws NodeProcessException {
        JsonValue userAttributes = IdmIntegrationService.mapContextToObject(context);
        if (userAttributes.size() != 0) {
            return userAttributes.keys().stream()
                    .collect(toMap(key -> key, key -> singleton(userAttributes.get(key).getObject().toString())));
        }

        if (!context.sharedState.isDefined(USER_INFO_SHARED_STATE_KEY)) {
            throw new NodeProcessException("No user information has been found in the shared state. You must call a "
                    + "node that sets this information first");
        }

        if (!context.sharedState.get(USER_INFO_SHARED_STATE_KEY).isDefined(USER_NAMES_SHARED_STATE_KEY)) {
            throw new NodeProcessException("The user information doesn't contain the userNames. You must call a "
                    + "node that sets this information first");
        }

        return SocialOAuth2Helper.convertMapOfListToMapOfSet(context.sharedState
                .get(USER_INFO_SHARED_STATE_KEY)
                .get(ATTRIBUTES_SHARED_STATE_KEY)
                .asMapOfList(String.class));
    }

    private String provisionAccount(TreeContext context, Map<String, Set<String>> userAttributes)
            throws NodeProcessException {
        userAttributes.put(USER_PASSWORD, singleton(getUserPassword(context)));
        userAttributes.put(USER_STATUS, singleton(ACTIVE));

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
        return context.getState(PASSWORD) != null
                ? context.getState(PASSWORD).asString()
                : authModuleHelper.getRandomData();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(USER_INFO_SHARED_STATE_KEY),
            new InputState(PASSWORD, false)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(USERNAME)
        };
    }
}
