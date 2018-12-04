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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.stream.Collectors.joining;
import static org.forgerock.openam.auth.node.api.Action.send;

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
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.ClientTokenJwtGenerator;
import org.forgerock.openam.integration.idm.IdmIntegrationConfig;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
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
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = ProvisionIdmAccountNode.Config.class)
public class ProvisionIdmAccountNode extends SingleOutcomeNode {

    /**
     * Possible AM oauth request parameters.
     */
    public static final String[] PARAMS = new String[]{"realm", "service", "goto"};

    static final String IDM_FLOW_INITIATED_KEY = "IdmFlowInitiated";

    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final IdmIntegrationService idmIntegrationService;
    private final Realm realm;
    private final SocialOAuth2Helper authModuleHelper;
    private final Config config;
    private final ClientTokenJwtGenerator clientTokenJwtGenerator;

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
     * @param idmIntegrationService Service stub for the IDM integration service.
     * @param clientTokenJwtGenerator Helper class for creating signed JWT to pass to IDM for account provisioning.
     * @throws NodeProcessException if there is a problem during construction.
     */
    @Inject
    public ProvisionIdmAccountNode(
            @Assisted Config config,
            @Assisted Realm realm,
            SocialOAuth2Helper authModuleHelper,
            IdmIntegrationService idmIntegrationService,
            ClientTokenJwtGenerator clientTokenJwtGenerator) throws NodeProcessException {
        this.config = config;
        this.realm = realm;
        this.authModuleHelper = authModuleHelper;
        this.idmIntegrationService = idmIntegrationService;
        this.clientTokenJwtGenerator = clientTokenJwtGenerator;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ProvisionIdmAccountNode node started");

        if (!context.sharedState.isDefined(IDM_FLOW_INITIATED_KEY)) {
            return send(createIDMCallback(context))
                    .replaceSharedState(context.sharedState.add(IDM_FLOW_INITIATED_KEY, Boolean.TRUE))
                    .build();
        }
        String user = getCreatedUser(context.sharedState);
        return goToNext()
                .replaceSharedState(context.sharedState.add(SharedStateConstants.USERNAME, user))
                .build();
    }

    private Callback createIDMCallback(TreeContext context) throws NodeProcessException {
        try {
            String idmUri = buildIdmUri(context);
            logger.debug("ProvisionIDMAccountNode: Creating callback to {}", idmUri);

            RedirectCallback callback = new RedirectCallback(idmUri.toString(), null, "GET");
            callback.setTrackingCookie(true);
            return callback;
        } catch (JsonProcessingException e) {
            throw new NodeProcessException(e);
        }
    }

    private String getCreatedUser(JsonValue sharedState) throws NodeProcessException {
        Map<String, Set<String>> userNames = convertMapOfListToMapOfSet(sharedState
                .get("userInfo")
                .get("userNames")
                .asMapOfList(String.class));
        try {
            AccountProvider accountProvider = OAuthUtil.instantiateAccountProvider(config.accountProviderClass());
            Optional<String> user = authModuleHelper.userExistsInTheDataStore(
                    realm.asPath(), accountProvider, userNames);
            if (!user.isPresent()) {
                throw new NodeProcessException("Unable to find user in datastore");
            }
            return user.get();
        } catch (AuthLoginException e) {
            throw new NodeProcessException(e);
        }
    }

    private String buildIdmUri(TreeContext context) throws JsonProcessingException {
        IdmIntegrationConfig idmConfig = idmIntegrationService.getConfig(realm.asPath());
        String urlRoot = idmConfig.getUrl().endsWith("/") ? idmConfig.getUrl() : idmConfig.getUrl() + "/";
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
                .collect(Collectors.toMap(p -> p.getKey(), p -> new HashSet<>(p.getValue())));
    }
}
