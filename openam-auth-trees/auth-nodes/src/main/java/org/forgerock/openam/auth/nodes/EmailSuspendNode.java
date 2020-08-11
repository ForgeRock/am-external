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

import static java.util.Collections.singletonMap;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.suspend;
import static org.forgerock.openam.auth.node.api.SuspendedTextOutputCallback.info;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getLocalisedMessage;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_REGISTRATION_EMAIL_TEMPLATE;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.inject.Named;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.SuspendedTextOutputCallback;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Node that sends an email template and suspends the tree. The resume link for the tree can be found as part of
 * the templateObject and is named `resumeURI`.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = EmailSuspendNode.Config.class,
        tags = {"utilities"})
public class EmailSuspendNode extends SingleOutcomeNode {
    static final String RESUME_URI = "resumeURI";

    private final EmailSuspendNode.Config config;
    private final LocaleSelector localeSelector;
    private final ExecutorService executorService;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;
    private final Logger logger = LoggerFactory.getLogger(EmailSuspendNode.class);

    /**
     * Configuration for the node.
     */
    public interface Config extends EmailTemplateNode.Config {

        /**
         * IDM email template to send.
         * @return the template name
         */
        @Override
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String emailTemplateName() {
            return DEFAULT_IDM_REGISTRATION_EMAIL_TEMPLATE;
        }

        /**
         * The email attribute in the IDM managed object.
         * @return the email attribute
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        default String emailAttribute() {
            return DEFAULT_IDM_MAIL_ATTRIBUTE;
        }

        /**
         * The message to return when node is suspended.
         * @return the message
         */
        @Attribute(order = 300, validators = {RequiredValueValidator.class})
        default Map<Locale, String> emailSuspendMessage() {
            return singletonMap(new Locale("en"),
                    "An email has been sent to the address you entered. Click the link in that email to proceed.");
        }

        /**
         * Boolean that determines whether or not object lookup should occur. When value is false, object for email
         * template will be taken from sharedState.
         * @return whether object lookup should occur
         */
        @Attribute(order = 400)
        default boolean objectLookup() {
            return false;
        }

        /**
         * The identity attribute from the object.
         * @return identity attribute
         */
        @Attribute(order = 500, validators = {RequiredValueValidator.class})
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    /**
     * Guice constructor.
     *
     * @param config                the node configuration
     * @param realm                 the realm
     * @param idmIntegrationService the IDM integration service.
     * @param executorService       the executor service to use when sending template
     * @param localeSelector        a LocaleSelector for choosing the correct message to display
     */
    @Inject
    public EmailSuspendNode(@Assisted Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService,
            @Named("EmailTemplateExecutor") ExecutorService executorService, LocaleSelector localeSelector) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
        this.executorService = executorService;
        this.localeSelector = localeSelector;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("EmailSuspendNode started");
        if (context.hasResumedFromSuspend()) {
            return goToNext().build();
        }

        Optional<String> identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()));
        identity.ifPresent(id -> logger.debug("Retrieving {} {}", context.identityResource, id));

        JsonValue templateObject = config.objectLookup()
                ? getObject(idmIntegrationService, realm, context.request.locales, context.identityResource,
                        config.identityAttribute(), identity)
                        .orElse(json(object()))
                : idmIntegrationService.getSharedAttributesFromContext(context);

        String recipient = Optional.ofNullable(templateObject.get(config.emailAttribute()).defaultTo(null)
                .asString()).filter(s -> !s.isEmpty())
                .orElse(null);

        return suspend(resumeURI -> createSuspendOutcome(context, resumeURI, recipient, templateObject)).build();
    }

    private SuspendedTextOutputCallback createSuspendOutcome(TreeContext context, URI resumeURI,
            String recipient, JsonValue templateObject) {

        String message = getLocalisedMessage(context, localeSelector, this.getClass(), config.emailSuspendMessage(),
                "emailSuspendMessage.default");

        // if required fields are not present, force node suspend without sending email
        if (templateObject.size() == 0 || recipient == null) {
            logger.debug("Object and recipient required to send email. Email not sent");
            return info(message);
        }

        templateObject.put(RESUME_URI, resumeURI);
        logger.debug("Sending email");
        idmIntegrationService.sendTemplate(executorService, realm, context.request.locales, config.emailTemplateName(),
                recipient, templateObject);

        return info(message);
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute())
        };
    }
}
