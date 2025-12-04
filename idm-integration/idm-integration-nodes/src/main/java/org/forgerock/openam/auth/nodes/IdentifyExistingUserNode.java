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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static com.sun.identity.shared.FeatureEnablementConstants.ALWAYS_SET_UNIVERSAL_ID;
import static java.util.Collections.singletonMap;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getUsernameFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.ALL_FIELDS;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.EXPAND_ALL_RELATIONSHIPS;

import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.am.util.SystemPropertiesWrapper;
import com.sun.identity.idm.IdType;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Node that identifies if a user exists.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = IdentifyExistingUserNode.Config.class,
        tags = {"identity management"})
public class IdentifyExistingUserNode extends AbstractDecisionNode {

    private final Logger logger = LoggerFactory.getLogger(IdentifyExistingUserNode.class);

    static final String IDM_IDPS = "aliasList";

    private final IdentifyExistingUserNode.Config config;
    private final Realm realm;
    private final NodeUserIdentityProvider identityProvider;
    private final IdmIntegrationService idmIntegrationService;
    private final SystemPropertiesWrapper systemProperties;

    /**
     * Configuration for the Identify Existing User Node.
     */
    public interface Config {

        /**
         * The IDM attribute to identify user. This will be used to save a value from the existing object to login.
         * Not required unless login is expected without prompting for identifier
         *
         * @return the identifier
         */
        @Attribute(order = 100)
        default String identifier() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }

        /**
         * The IDM attribute used to identify the target object in a query filter.
         *
         * @return identity attribute
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        default String identityAttribute() {
            return DEFAULT_IDM_MAIL_ATTRIBUTE;
        }
    }

    /**
     * Constructs a new IdentifyExistingUserNode instance.
     *
     * @param config The node configuration.
     * @param realm The realm context.
     * @param identityProvider The identity provider.
     * @param idmIntegrationService The IDM integration service.
     * @param systemProperties The system properties.
     */
    @Inject
    public IdentifyExistingUserNode(@Assisted IdentifyExistingUserNode.Config config, @Assisted Realm realm,
            NodeUserIdentityProvider identityProvider, IdmIntegrationService idmIntegrationService,
            SystemPropertiesWrapper systemProperties) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
        this.identityProvider = identityProvider;
        this.systemProperties = systemProperties;
    }

    /**
     * Performs processing on the given shared state, which holds all the data gathered by nodes that have already
     * executed as part of this authentication session in the tree.
     *
     * <p>This method is invoked when the node is reached in the tree.</p>
     *
     * @param context The context of the tree authentication.
     * @return The next action to perform. Must not be null.
     * @throws NodeProcessException If there was a problem processing that could not be resolved to a single outcome.
     */
    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("IdentifyExistingUserNode started");

        Optional<String> identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()))
                .or(() -> stringAttribute(getUsernameFromContext(idmIntegrationService, context)));
        identity.ifPresent(id -> logger.debug("Retrieving {} {}", context.identityResource, id));

        Optional<JsonValue> managedObject = getObject(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), identity,
                ALL_FIELDS, EXPAND_ALL_RELATIONSHIPS);

        if (managedObject.isPresent()) {
            logger.debug("Existing {} identified {}", context.identityResource, managedObject.get());
            managedObject.get().get(IDM_IDPS).forEach(idp -> idmIntegrationService
                    .storeListAttributeInState(context.transientState, IDM_IDPS, idp.asString()));
            JsonValue copyState = context.sharedState.copy().put(FIELD_CONTENT_ID,
                    managedObject.get().get(FIELD_CONTENT_ID));

            var action = goTo(true);
            // save identifier so that it is present for login
            if (!Strings.isNullOrEmpty(config.identifier()) && managedObject.get().isDefined(config.identifier())) {
                String identifier = managedObject.get().get(config.identifier()).asString();
                copyState.put(USERNAME, identifier);
                idmIntegrationService.storeAttributeInState(copyState, config.identifier(), identifier);
            }

            action.replaceSharedState(copyState);

            if (copyState.get(USERNAME).isNotNull()) {
                final String username = copyState.get(USERNAME).asString();
                action.withIdentifiedIdentity(username, IdType.USER)
                    .withUniversalId(getUniversalId(username, context));
            }
            return action.build();
        }
        logger.debug("No {} identified", context.identityResource);
        return goTo(false).build();
    }

    private Optional<String> getUniversalId(final String identifier, final TreeContext context) {
        return systemProperties.getAsBoolean(ALWAYS_SET_UNIVERSAL_ID, true)
                ? identityProvider.getUniversalId(identifier, realm)
                : context.universalId.or(() -> identityProvider.getUniversalId(context.getStateFor(this)));
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
            new OutputState(FIELD_CONTENT_ID, singletonMap(TRUE_OUTCOME_ID, true)),
            new OutputState(FIELD_CONTENT_ID, singletonMap(FALSE_OUTCOME_ID, false)),
            new OutputState(IDM_IDPS, singletonMap("*", false)),
        };
    }

}
