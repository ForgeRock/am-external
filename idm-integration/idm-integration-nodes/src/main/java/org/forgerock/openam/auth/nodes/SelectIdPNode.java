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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObjectIdentityProviders;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_PASSWORD_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.LOCAL_AUTHENTICATION;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.SELECTED_IDP;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.filterIdentityProviderData;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.SelectIdPCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.social.idp.SocialIdentityProviders;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * Offer a set of choices of IdP to the client and use the choice as the node outcome.
 */
@Node.Metadata(outcomeProvider = SelectIdPNode.SelectIdPNodeOutcomeProvider.class,
        configClass = SelectIdPNode.Config.class,
        tags = {"identity management"})
public class SelectIdPNode implements Node {
    private static final String PROVIDER_PROPERTY = "provider";
    private static final String UI_CONFIG_PROPERTY = "uiConfig";
    private static final String SOCIAL_AUTHENTICATION = "socialAuthentication";
    private static final String LOCAL_OUTCOME = "localOutcome";
    private static final String SOCIAL_OUTCOME = "socialOutcome";
    private static final String BUNDLE = SelectIdPNode.class.getName();

    private final Logger logger = LoggerFactory.getLogger(SelectIdPNode.class);
    private final Config config;
    private final SocialIdentityProviders idPService;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * Whether to include local authentication in the list of available providers.
         *
         * @return true iff local authentication should be included in the list of available providers
         */
        @Attribute(order = 100)
        default boolean includeLocalAuthentication() {
            return true;
        }

        /**
         * Whether to restrict offered choices to only those already existing in the object.
         *
         * @return true iff offered choices should be limited to those existing in the object.
         */
        @Attribute(order = 200)
        default boolean offerOnlyExisting() {
            return false;
        }

        /**
         * The attribute in the retrieved object that verifies that object during a local authentication.
         *
         * @return local authn password attribute
         */
        @Attribute(order = 300)
        default String passwordAttribute() {
            return DEFAULT_IDM_PASSWORD_ATTRIBUTE;
        }

        /**
         * The IDM attribute used to identify the target object in a query filter.
         *
         * @return identity attribute
         */
        @Attribute(order = 500)
        default String identityAttribute() {
            return DEFAULT_IDM_MAIL_ATTRIBUTE;
        }


        /**
         * Specify the names of enabled identity providers to use.
         * If not specified, the node uses all providers enabled in the "Social Identity Provider Service".
         *
         * @return Identity provider Filter
         */
        @Attribute(order = 600)
        default Set<String> filteredProviders() {
            return Collections.emptySet();
        }
    }

    /**
     * Constructor.
     *
     * @param config the node config
     * @param realm the realm context
     * @param idPService service containing social provider configurations
     * @param idmIntegrationService the IDM integration service
     */
    @Inject
    public SelectIdPNode(@Assisted Config config, @Assisted Realm realm, SocialIdentityProviders idPService,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idPService = idPService;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("SelectIdPNode started");

        JsonValue providers = addLocalAuthentication(filterIdentityProviderData(json(
                idPService.getProviders(realm).entrySet().stream()
                        .map(entry -> object(
                                field(PROVIDER_PROPERTY, entry.getKey()),
                                field(UI_CONFIG_PROPERTY, entry.getValue().uiConfig())))
                        .collect(Collectors.toList()))));

        if (idmIntegrationService.isEnabled() && !providers.asList().isEmpty() && config.offerOnlyExisting()) {
            providers = limitProviders(context, providers);
        }

        providers = limitProviders(providers);

        // If there is only one provider to offer, auto-select that one and advance the tree
        if (providers.size() == 1) {
            String provider = providers.get(0).get(PROVIDER_PROPERTY).asString();
            final JsonValue sharedState = context.sharedState.copy().put(SELECTED_IDP, provider);
            logger.debug("SelectIdPNode autoadvancing with only one available IdP {}", provider);

            return goTo(provider.equals(LOCAL_AUTHENTICATION)
                    ? LOCAL_AUTHENTICATION
                    : SOCIAL_AUTHENTICATION)
                    .replaceSharedState(sharedState)
                    .build();
        } else if (providers.size() == 0) {
            logger.debug("SelectIdPNode found no available providers!");
            throw new NodeProcessException(("No available authentication providers"));
        }

        Optional<SelectIdPCallback> callback = context.getCallback(SelectIdPCallback.class);
        if (!callback.isPresent()) {
            logger.debug("SelectIdPNode returning initial callback");
            return send(new SelectIdPCallback(providers)).build();
        }

        if (config.includeLocalAuthentication() && LOCAL_AUTHENTICATION.equals(callback.get().getProvider())) {
            logger.debug("SelectIdPNode complete with local authentication");
            return goTo(LOCAL_AUTHENTICATION).build();
        }

        if (providers.stream()
                .noneMatch(provider -> provider.get(PROVIDER_PROPERTY).asString()
                        .equals(callback.get().getProvider()))) {
            logger.debug("SelectIdPNode did not recognize selected provider {}", callback.get().getProvider());
            throw new NodeProcessException("Selected provider is unknown");
        }

        final JsonValue sharedState = context.sharedState.copy().put(SELECTED_IDP, callback.get().getProvider());
        logger.debug("SelectIdPNode complete with selected IdP {}", callback.get().getProvider());

        return goTo(SOCIAL_AUTHENTICATION)
                .replaceSharedState(sharedState)
                .build();
    }

    private JsonValue limitProviders(TreeContext context, JsonValue providers) throws NodeProcessException {
        JsonValue filtered = json(array());
        List<String> existing = getObjectIdentityProviders(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(),
                stringAttribute(getAttributeFromContext(idmIntegrationService, context, config.identityAttribute()))
                        .orElseThrow(() -> new NodeProcessException("No identity stored in state")),
                config.passwordAttribute());
        providers.forEach(provider -> {
            if (existing.contains(provider.get(PROVIDER_PROPERTY).asString())) {
                filtered.add(provider);
            }
        });
        return filtered;
    }

    private JsonValue limitProviders(JsonValue providers) {
        if (config.filteredProviders().isEmpty()) {
            return providers;
        }
        JsonValue filtered = json(array());
        providers.forEach(provider -> {
            if (config.filteredProviders().contains(provider.get(PROVIDER_PROPERTY).asString())) {
                filtered.add(provider);
            }
        });
        return addLocalAuthentication(filtered);
    }

    private JsonValue addLocalAuthentication(JsonValue providers) {
        return config.includeLocalAuthentication()
                ? providers.add(object(field(PROVIDER_PROPERTY, LOCAL_AUTHENTICATION)))
                : providers;
    }

    /**
     * Defines the possible outcomes from this node. Depending on configuration, this will include a combination of
     * localAuthentication and socialAuthentication.
     */
    public static class SelectIdPNodeOutcomeProvider implements BoundedOutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            return getAllOutcomes(locales).stream().filter(outcome -> {
                if (LOCAL_AUTHENTICATION.equals(outcome.id)) {
                    return nodeAttributes.get("includeLocalAuthentication").defaultTo(true).asBoolean();
                }
                return true;
            }).toList();
        }

        @Override
        public List<Outcome> getAllOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(SelectIdPNode.BUNDLE,
                    SelectIdPNodeOutcomeProvider.class.getClassLoader());
            return List.of(
                    new Outcome(SOCIAL_AUTHENTICATION, bundle.getString(SOCIAL_OUTCOME)),
                    new Outcome(LOCAL_AUTHENTICATION, bundle.getString(LOCAL_OUTCOME))
            );
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[]{
            new InputState(config.identityAttribute())
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[]{
            new OutputState(SELECTED_IDP)
        };
    }
}
