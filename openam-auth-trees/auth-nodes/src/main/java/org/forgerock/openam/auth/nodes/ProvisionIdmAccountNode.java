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
 * Copyright 2018-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static com.sun.identity.idm.IdType.USER;
import static java.util.stream.Collectors.joining;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USER_INFO;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.callback.Callback;

import org.apache.http.client.utils.URIBuilder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.integration.idm.ClientTokenJwtGenerator;
import org.forgerock.openam.integration.idm.IdmIntegrationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node to call out to the IDM user registration service during the social
 * auth login flow.
 *
 * @deprecated Use {@link org.forgerock.openam.auth.nodes.SelectIdPNode},
 * {@link org.forgerock.openam.auth.nodes.SocialProviderHandlerNode} and
 * {@link org.forgerock.openam.auth.nodes.CreateObjectNode} instead.
 */
@Deprecated
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = ProvisionIdmAccountNode.Config.class,
        tags = {"idm", "federation"})
public class ProvisionIdmAccountNode extends SingleOutcomeNode {

    /**
     * Possible AM oauth request parameters.
     */
    public static final String[] PARAMS = new String[]{"realm", "service", "goto"};

    static final String IDM_FLOW_INITIATED_KEY = "IdmFlowInitiated";
    static final String USER_NAMES_KEY = "userNames";

    private final Logger logger = LoggerFactory.getLogger(ProvisionIdmAccountNode.class);
    private final Realm realm;
    private final SocialOAuth2Helper authModuleHelper;
    private final Config config;
    private final ClientTokenJwtGenerator clientTokenJwtGenerator;
    private final IdmIntegrationConfig idmConfigProvider;
    private final LegacyIdentityService identityService;

    /**
     * The interface Config.
     */
    public interface Config {
        /**
         * Account provider class.
         * @return The fully qualified classname.
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String accountProviderClass() {
            return "org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider";
        }
    }

    /**
     * Constructs a new {@link ProvisionIdmAccountNode} with the provided {@link ProvisionIdmAccountNode.Config}.
     *
     * @param config The node configuration.
     * @param realm The realm context.
     * @param authModuleHelper Helper class for oauth2.
     * @param clientTokenJwtGenerator Helper class for creating signed JWT to pass to IDM for account provisioning.
     * @param idmConfigProvider IDM configuration provider
     * @param identityService An instance of the IdentityService.
     */
    @Inject
    public ProvisionIdmAccountNode(@Assisted Config config, @Assisted Realm realm,
            SocialOAuth2Helper authModuleHelper, ClientTokenJwtGenerator clientTokenJwtGenerator,
            IdmIntegrationConfig idmConfigProvider, LegacyIdentityService identityService) {
        this.config = config;
        this.realm = realm;
        this.authModuleHelper = authModuleHelper;
        this.clientTokenJwtGenerator = clientTokenJwtGenerator;
        this.idmConfigProvider = idmConfigProvider;
        this.identityService = identityService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ProvisionIdmAccountNode node started");

        if (!context.sharedState.isDefined(IDM_FLOW_INITIATED_KEY)) {
            return send(createIDMCallback(context))
                    .replaceSharedState(context.sharedState.add(IDM_FLOW_INITIATED_KEY, Boolean.TRUE))
                    .build();
        }
        JsonValue sharedState = context.sharedState.copy();
        String username = getCreatedUser(context.sharedState);
        sharedState.add(USERNAME, username);
        String realm = context.sharedState.get(REALM).asString();
        sharedState.remove(IDM_FLOW_INITIATED_KEY);
        return goToNext()
                .withUniversalId(identityService.getUniversalId(username, realm, USER))
                .withIdentifiedIdentity(username, USER)
                .replaceSharedState(sharedState)
                .build();
    }

    private Callback createIDMCallback(TreeContext context) throws NodeProcessException {
        try {
            String idmUri = buildIdmUri(context);
            logger.debug("ProvisionIDMAccountNode: Creating callback to {}", idmUri);

            RedirectCallback callback = new RedirectCallback(idmUri, null, "GET");
            callback.setTrackingCookie(true);
            return callback;
        } catch (JsonProcessingException e) {
            throw new NodeProcessException(e);
        }
    }

    private String getCreatedUser(JsonValue sharedState) throws NodeProcessException {
        Map<String, Set<String>> userNames = convertMapOfListToMapOfSet(sharedState
                .get(USER_INFO)
                .get(USER_NAMES_KEY)
                .asMapOfList(String.class));
        try {
            AccountProvider accountProvider = OAuthUtil.instantiateAccountProvider(config.accountProviderClass());
            Optional<String> user = authModuleHelper.userExistsInTheDataStore(
                    realm.asPath(), accountProvider, userNames);
            if (user.isEmpty()) {
                throw new NodeProcessException("Unable to find user in datastore");
            }
            return user.get();
        } catch (AuthLoginException e) {
            throw new NodeProcessException(e);
        }
    }

    private String buildIdmUri(TreeContext context) throws JsonProcessingException {
        IdmIntegrationConfig.GlobalConfig idmConfig = idmConfigProvider.global();
        String urlRoot = idmConfig.idmDeploymentUrl().endsWith("/")
                ? idmConfig.idmDeploymentUrl()
                : idmConfig.idmDeploymentUrl() + "/";
        urlRoot += "#handleOAuth/&";
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.addParameter("returnParams", generateRequestParams(context));
        uriBuilder.addParameter("clientToken", clientTokenJwtGenerator.generate(idmConfig, context.sharedState));
        return urlRoot + uriBuilder.toString().substring(1);
    }

    private String generateRequestParams(TreeContext context) {
        return context.request.parameters.entrySet().stream()
                .filter(e -> Arrays.asList(PARAMS).contains(e.getKey()))
                .map(e -> e.getKey() + "=" + e.getValue().get(0))
                .collect(joining("&"));
    }

    private Map<String, Set<String>> convertMapOfListToMapOfSet(Map<String, List<String>> attrMap) {
        return attrMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, p -> new HashSet<>(p.getValue())));
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(USERNAME)
        };
    }
}
