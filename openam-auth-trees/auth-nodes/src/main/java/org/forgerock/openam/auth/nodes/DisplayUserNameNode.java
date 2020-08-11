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

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.forgerock.util.Strings.isNullOrEmpty;

import java.util.Optional;

import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Node to display a user name.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = DisplayUserNameNode.Config.class,
        tags = {"identity management"})
public class DisplayUserNameNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger(DisplayUserNameNode.class);
    private final Config config;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;

    /**
     * Node Configuration.
     */
    public interface Config {

        /**
         * The user name attribute.
         * @return the user name attribute
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String userName() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }

        /**
         * The identity attribute from the object.
         * @return identity attribute
         */
        @Attribute(order = 200)
        default String identityAttribute() {
            return DEFAULT_IDM_MAIL_ATTRIBUTE;
        }
    }

    /**
     * Guice constructor.
     *
     * @param config the node configuration
     * @param realm the realm
     * @param idmIntegrationService the IDM integration service.
     */
    @Inject
    public DisplayUserNameNode(@Assisted Realm realm, @Assisted Config config,
            IdmIntegrationService idmIntegrationService) {
        this.realm = realm;
        this.config = config;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("DisplayUserNameNode started");
        if (context.getCallback(TextOutputCallback.class).isPresent()) {
            logger.debug("Username has been displayed");
            return goToNext().build();
        }

        // return userName from state if present
        Optional<String> userName = stringAttribute(
                idmIntegrationService.getAttributeFromContext(context, config.userName()));
        if (userName.isEmpty()) {
            userName = Optional.ofNullable(getUsernameFromObject(context));
        }

        if (userName.isEmpty() || isNullOrEmpty(userName.get())) {
            throw new NodeProcessException("Unable to find username to display");
        }

        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, userName.get());

        logger.debug("Displaying username");
        return send(textOutputCallback).build();
    }

    private String getUsernameFromObject(TreeContext context) throws NodeProcessException {
        Optional<String> objectValue = stringAttribute(
                getAttributeFromContext(idmIntegrationService, context, config.identityAttribute()));
        logger.debug("Retrieving {} of {} {}", config.userName(), context.identityResource, objectValue);
        JsonValue existingObject = getObject(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), objectValue, config.userName())
                .orElseThrow(() -> new NodeProcessException("Failed to retrieve existing object"));

        return existingObject.get(config.userName()).asString();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.userName(), false)
        };
    }
}
