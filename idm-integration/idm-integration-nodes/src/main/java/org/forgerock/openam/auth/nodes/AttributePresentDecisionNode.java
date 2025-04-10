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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getUsernameFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_PASSWORD_ATTRIBUTE;
import static org.forgerock.util.query.QueryFilter.and;
import static org.forgerock.util.query.QueryFilter.equalTo;
import static org.forgerock.util.query.QueryFilter.present;

import java.util.Optional;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * The {@link AttributePresentDecisionNode} determines if an attribute is present regardless of IDM scope on managed
 * object.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = AttributePresentDecisionNode.Config.class,
        tags = {"identity management"})
public class AttributePresentDecisionNode extends AbstractDecisionNode {

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
        default String presentAttribute() {
            return DEFAULT_PASSWORD_ATTRIBUTE;
        };

        /**
         * The attribute to query the managed object by.
         *
         * @return managed object identity attribute
         */
        @Attribute(order = 200, validators = RequiredValueValidator.class)
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }
    private final Logger logger = LoggerFactory.getLogger(AttributePresentDecisionNode.class);
    private final AttributePresentDecisionNode.Config config;
    private final IdmIntegrationService idmIntegrationService;
    private final Realm realm;

    /**
     * Constructs a new AttributePresentDecisionNode instance.
     *
     * @param config node configuration
     * @param realm the realm context
     * @param idmIntegrationService service stub for the IDM integration service
     */
    @Inject
    public AttributePresentDecisionNode(@Assisted AttributePresentDecisionNode.Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("AttributePresentDecisionNode started");
        final String presentAttribute = config.presentAttribute();
        String identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()))
                .or(() -> stringAttribute(getUsernameFromContext(idmIntegrationService, context)))
                .orElseThrow(() -> new NodeProcessException(config.identityAttribute() + " not present in state"));

        QueryFilter filter = and(equalTo(ptr(config.identityAttribute()), identity),
                present(config.presentAttribute()));
        final Optional<JsonValue> existingObject = getObject(idmIntegrationService, realm, context.request.locales,
                context.identityResource, filter, config.presentAttribute());

        logger.debug("Evaluating {} value", presentAttribute);
        return goTo(!existingObject.isEmpty())
                .replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy())
                .build();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute())
        };
    }

}
