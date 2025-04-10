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

import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getUsernameFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.util.query.JsonValueFilterVisitor.jsonValueFilterVisitor;

import java.util.Optional;

import javax.inject.Inject;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Node that evaluates a managed object's attribute against a configurable condition.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = AttributeValueDecisionNode.Config.class,
        tags = {"identity management"})
public class AttributeValueDecisionNode extends AbstractDecisionNode {

    /**
     * Enumeration of available comparison operations.
     */
    public enum ComparisonOperation {
        /**
         * Provides a QueryFilter that checks for presence of an attribute.
         */
        PRESENT {
            @Override
            QueryFilter<JsonPointer> generateQueryFilter(JsonPointer attribute, Object value) {
                return QueryFilter.present(attribute);
            }
        },

        /**
         * Provides a QueryFilter that checks if an attribute is equal to the provided comparisonValue.
         */
        EQUALS {
            @Override
            QueryFilter<JsonPointer> generateQueryFilter(JsonPointer attribute, Object value) {
                return QueryFilter.equalTo(attribute, value);
            }
        };

        /**
         * Generates a QueryFilter using the specified attribute and value.
         *
         * @param attribute the attribute to compare
         * @param value the value to compare against
         * @return a filter for attribute decisions
         */
        abstract QueryFilter<JsonPointer> generateQueryFilter(JsonPointer attribute, Object value);
    }

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The object attribute name to evaluate.
         *
         * @return the attribute name
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        String comparisonAttribute();

        /**
         * The comparison operation to use on the attribute.
         *
         * @return attribute comparison operation
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default ComparisonOperation comparisonOperation() {
            return ComparisonOperation.EQUALS;
        }

        /**
         * The comparisonValue to compare the object's attribute to.
         *
         * @return comparison comparisonValue
         */
        @Attribute(order = 300)
        Optional<String> comparisonValue();

        /**
         * The attribute to query the managed object by.
         *
         * @return managed object identity attribute
         */
        @Attribute(order = 400, validators = RequiredValueValidator.class)
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    private final Logger logger = LoggerFactory.getLogger(AttributeValueDecisionNode.class);
    private final Config config;
    private final IdmIntegrationService idmIntegrationService;
    private final Realm realm;

    /**
     * Constructs a new AttributeValueDecisionNode instance.
     *
     * @param config node configuration
     * @param realm the realm context
     * @param idmIntegrationService service stub for the IDM integration service
     */
    @Inject
    public AttributeValueDecisionNode(@Assisted Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("AttributeValueDecisionNode started");
        final String comparisonAttribute = config.comparisonAttribute();
        String identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()))
                .or(() -> stringAttribute(getUsernameFromContext(idmIntegrationService, context)))
                .orElseThrow(() -> new NodeProcessException(config.identityAttribute() + " not present in state"));
        logger.debug("Retrieving {} of {} {}", comparisonAttribute, context.identityResource, identity);
        final JsonValue object = IdmIntegrationHelper.getObject(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), Optional.of(identity), comparisonAttribute)
                .orElseThrow(() -> new NodeProcessException("Failed to read object"));

        logger.debug("Evaluating {} value", comparisonAttribute);
        return goTo(config.comparisonOperation()
                .generateQueryFilter(ptr(comparisonAttribute), config.comparisonValue()
                        .map(value -> convertValueToAttributeType(value, object))
                        .orElse(null))
                .accept(jsonValueFilterVisitor(), object))
                .replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy())
                .build();
    }

    private Object convertValueToAttributeType(String value, JsonValue object) {
        final JsonValue userAttribute = object.get(ptr(config.comparisonAttribute()));

        if (userAttribute == null) {
            return value;
        } else if (userAttribute.isBoolean()) {
            return Boolean.parseBoolean(value);
        } else if (userAttribute.isNumber()) {
            return Long.parseLong(value);
        } else {
            return value;
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute())
        };
    }
}
