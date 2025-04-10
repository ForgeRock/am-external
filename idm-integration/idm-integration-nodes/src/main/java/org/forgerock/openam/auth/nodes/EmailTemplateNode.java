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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getUsernameFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_WELCOME_EMAIL_TEMPLATE;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javax.inject.Named;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Node that sends email via the IDM email service.
 */
@Node.Metadata(outcomeProvider = EmailTemplateNode.EmailTemplateOutcomeProvider.class,
        configClass = EmailTemplateNode.Config.class,
        tags = {"utilities"})
public class EmailTemplateNode implements Node {
    private static final String BUNDLE = "org.forgerock.openam.auth.nodes.EmailTemplateNode";

    private final ExecutorService executorService;
    private final Logger logger = LoggerFactory.getLogger(EmailTemplateNode.class);
    private final EmailTemplateNode.Config config;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * IDM email template to send.
         * @return the template name.
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String emailTemplateName() {
            return DEFAULT_IDM_WELCOME_EMAIL_TEMPLATE;
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
         * The identity attribute from the object.
         * @return identity attribute
         */
        @Attribute(order = 300, validators = {RequiredValueValidator.class})
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    /**
     * Guice constructor.
     *
     * @param config the node configuration
     * @param realm the realm
     * @param idmIntegrationService the IDM integration service.
     * @param executorService the executor service to use when sending template
     */
    @Inject
    public EmailTemplateNode(@Assisted Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService,
            @Named("EmailTemplateExecutor") ExecutorService executorService) {

        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
        this.executorService = executorService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("EmailTemplateNode started");

        Optional<String> identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()))
                .or(() -> stringAttribute(getUsernameFromContext(idmIntegrationService, context)));
        identity.ifPresent(id -> logger.debug("Retrieving {} {}", context.identityResource, id));

        // email can only be sent with object existing
        JsonValue templateObject = getObject(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), identity)
                .orElse(json(object()));

        String recipient = Optional.ofNullable(templateObject.get(config.emailAttribute()).defaultTo(null)
                .asString()).filter(s -> !s.isEmpty())
                .orElse(null);

        // continue to next node if unable to send email
        if (templateObject.size() == 0 || recipient == null) {
            logger.debug("No email address found. Email not sent");
            return goTo(EmailTemplateOutcome.EMAIL_NOT_SENT.name()).build();
        }

        logger.debug("Sending email");
        idmIntegrationService.sendTemplate(executorService, realm, context.request.locales, config.emailTemplateName(),
                recipient, templateObject);
        return goTo(EmailTemplateOutcome.EMAIL_SENT.name()).build();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute())
        };
    }

    /**
     * Possible outcomes.
     */
    public enum EmailTemplateOutcome {
        /**
         * Email was sent.
         */
        EMAIL_SENT,
        /**
         * Email count not be sent.
         */
        EMAIL_NOT_SENT
    }

    /**
     * Defines the possible outcomes from this node.
     */
    public static class EmailTemplateOutcomeProvider implements StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(EmailTemplateNode.BUNDLE,
                    EmailTemplateNode.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(EmailTemplateOutcome.EMAIL_SENT.name(),
                            bundle.getString("emailSent")),
                    new Outcome(EmailTemplateOutcome.EMAIL_NOT_SENT.name(),
                            bundle.getString("emailNotSent")));
        }
    }
}
