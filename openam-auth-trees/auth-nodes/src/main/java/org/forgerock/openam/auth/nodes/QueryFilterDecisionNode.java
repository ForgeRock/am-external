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
 * Copyright 2019-2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.ALL_FIELDS;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.EXPAND_ALL_RELATIONSHIPS;
import static org.forgerock.util.query.JsonValueFilterVisitor.jsonValueFilterVisitor;

import java.util.Optional;

import javax.inject.Inject;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.QueryFilterValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node to determine whether a user object matches a query filter.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = QueryFilterDecisionNode.Config.class,
        tags = {"identity management"})
public class QueryFilterDecisionNode extends AbstractDecisionNode {

    private final Logger logger = LoggerFactory.getLogger(QueryFilterDecisionNode.class);

    private final QueryFilterDecisionNode.Config config;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The query filter to apply to the user object.
         *
         * @return the query filter to apply to the user object
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class, QueryFilterValidator.class})
        String queryFilter();

        /**
         * The attribute to query the IDM object by.
         *
         * @return the identity attribute of the IDM object
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    /**
     * Builds the node.
     *
     * @param config the node configuration
     * @param realm the realm context
     * @param idmIntegrationService Service stub for the IDM integration service
     */
    @Inject
    public QueryFilterDecisionNode(@Assisted QueryFilterDecisionNode.Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("QueryFilterDecisionNode started");

        String identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()))
                .orElseThrow(() -> new NodeProcessException(config.identityAttribute() + " not present in state"));
        logger.debug("Retrieving {} {}", context.identityResource, identity);
        JsonValue existingObject = getObject(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), Optional.of(identity),
                ALL_FIELDS, EXPAND_ALL_RELATIONSHIPS)
                .orElseThrow(() -> new NodeProcessException("Failed to retrieve existing object"));

        QueryFilter<JsonPointer> filter = QueryFilters.parse(config.queryFilter());
        boolean result = filter.accept(jsonValueFilterVisitor(), existingObject);

        return goTo(result).replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy()).build();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute())
        };
    }
}
