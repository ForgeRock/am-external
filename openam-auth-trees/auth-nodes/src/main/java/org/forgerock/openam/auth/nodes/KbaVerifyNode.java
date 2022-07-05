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
 * Copyright 2019-2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.stream.Collectors.toMap;
import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getKbaConfig;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getLocalisedMessage;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getUsernameFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.patchObject;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.stringAttribute;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_KBAINFO_ATTRIBUTE;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.inject.Inject;
import javax.security.auth.callback.PasswordCallback;

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
import org.forgerock.openam.integration.idm.KbaConfig;
import org.forgerock.selfservice.core.crypto.CryptoService;
import org.forgerock.selfservice.core.crypto.JsonCryptoException;
import org.forgerock.selfservice.core.util.Answers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node for verification of KBA answers from an end-user.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class, configClass = KbaVerifyNode.Config.class,
        tags = {"identity management"})
public class KbaVerifyNode extends AbstractDecisionNode {

    private static final String CUSTOM_QUESTION = "customQuestion";
    private static final String QUESTION_ID = "questionId";
    private static final String ANSWER = "answer";

    private final Logger logger = LoggerFactory.getLogger(KbaVerifyNode.class);
    private final KbaVerifyNode.Config config;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;
    private final LocaleSelector localeSelector;
    private final CryptoService cryptoService;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Configurable attribute for storing KBA info in the user object in IDM.
         *
         * @return the KBA info attribute in IDM
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String kbaInfoAttribute() {
            return DEFAULT_KBAINFO_ATTRIBUTE;
        }

        /**
         * Configurable identity attribute for search query in IDM.
         *
         * @return the attribute used to find the user object in IDM
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        default String identityAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }
    }

    /**
     * Construct a KbaVerifyNode.
     *
     * @param config the configuration for this node
     * @param realm the realm for the tree this node is in
     * @param idmIntegrationService the IdmIntegrationService for communicating with IDM
     * @param localeSelector a LocaleSelector for choosing the correct question to display
     * @param cryptoService a CryptoService impl
     */
    @Inject
    public KbaVerifyNode(@Assisted Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService, LocaleSelector localeSelector,
            CryptoService cryptoService) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
        this.localeSelector = localeSelector;
        this.cryptoService = cryptoService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("KbaVerifyNode started");
        logger.debug("Retrieving KBA configuration");
        KbaConfig kbaConfig = getKbaConfig(idmIntegrationService, realm, context.request.locales);

        Optional<String> identity = stringAttribute(getAttributeFromContext(idmIntegrationService, context,
                config.identityAttribute()))
                .or(() -> stringAttribute(getUsernameFromContext(idmIntegrationService, context)));
        identity.ifPresent(id -> logger.debug("Retrieving {} of {} {}", kbaConfig.getKbaPropertyName(),
                    context.identityResource, id));

        JsonValue userObject = getObject(idmIntegrationService, realm, context.request.locales,
                context.identityResource, config.identityAttribute(), identity)
                .orElseThrow(() -> new NodeProcessException("Failed to retrieve user object"));
        if (!failCounterWithinLimits(kbaConfig, userObject)) {
            throw new NodeProcessException("Maximum attempts exceeded");
        }

        List<PasswordCallback> newCallbacks = generateCallbacks(context, kbaConfig, userObject);

        if (context.getAllCallbacks().isEmpty()) {
            // Return the KBA callbacks
            logger.debug("Collecting KBA answers");
            return send(newCallbacks).build();
        }

        List<PasswordCallback> callbacks = context.getCallbacks(PasswordCallback.class);

        // Verify answers given
        return goTo(verifyCallbacks(context, kbaConfig, userObject, callbacks))
                .replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy())
                .build();
    }

    private List<PasswordCallback> generateCallbacks(TreeContext context, KbaConfig kbaConfig, JsonValue userObject)
            throws NodeProcessException {
        if (kbaConfig.getMinimumAnswersToVerify() == 0) {
            throw new NodeProcessException("Minimum answers to verify is set to zero");
        }
        JsonValue userKba = userObject.get(config.kbaInfoAttribute());
        if (userKba.size() < kbaConfig.getMinimumAnswersToDefine()) {
            throw new NodeProcessException("Object has defined fewer questions than the minimum");
        }
        List<PasswordCallback> callbacks = new ArrayList<>();
        for (JsonValue question : userKba) {
            if (question.isDefined(CUSTOM_QUESTION)) {
                callbacks.add(new PasswordCallback(question.get(CUSTOM_QUESTION).asString(), false));
            } else {
                callbacks.add(new PasswordCallback(translateQuestion(context, kbaConfig,
                        question.get(QUESTION_ID).asString()), false));
            }
        }
        while (callbacks.size() > kbaConfig.getMinimumAnswersToVerify()) {
            callbacks.remove(new Random().nextInt(callbacks.size()));
        }
        return callbacks;
    }

    private String translateQuestion(TreeContext context, KbaConfig kbaConfig, String questionId)
            throws NodeProcessException {
        Map<String, String> questions = kbaConfig.getQuestions().get(questionId);
        if (!questions.isEmpty()) {
            Map<Locale, String> locales = questions.entrySet().stream()
                    .collect(toMap(e -> Locale.forLanguageTag(e.getKey()), e -> e.getValue(),
                        (value1, value2) -> value2, LinkedHashMap::new));

            return getLocalisedMessage(context, localeSelector, this.getClass(), locales, "questions.default");
        }
        throw new NodeProcessException("Question id " + questionId + " is not defined");
    }

    private boolean verifyCallbacks(TreeContext context, KbaConfig kbaConfig, JsonValue userObject,
            List<PasswordCallback> callbacks) throws NodeProcessException {
        try {
            if (callbacks.size() < kbaConfig.getMinimumAnswersToVerify()) {
                logger.warn("Not enough answers provided");
                return false;
            }
            JsonValue kbaInfo = userObject.get(config.kbaInfoAttribute());
            for (PasswordCallback callback : callbacks) {
                JsonValue answer = findAnswerForQuestion(callback.getPrompt(), kbaConfig, kbaInfo);
                if (answer == null || !cryptoService.matches(
                        Answers.normaliseAnswer(new String(callback.getPassword())), answer.get(ANSWER))) {
                    logger.warn("Provided answer(s) are invalid");
                    incrementFailCounter(context, kbaConfig, userObject);
                    return false;
                }
            }
            resetFailCounter(context, kbaConfig, userObject);
            return true;
        } catch (JsonCryptoException e) {
            logger.warn("Unable to hash provided answers", e);
            throw new NodeProcessException(e);
        }
    }

    private JsonValue findAnswerForQuestion(String question, KbaConfig kbaConfig, JsonValue kbaInfo) {
        return kbaInfo.stream()
                .filter(kba -> kba.isDefined(CUSTOM_QUESTION) && kba.get(CUSTOM_QUESTION).asString().equals(question))
                .findFirst()
                .orElse(json(kbaConfig.getQuestions()).stream()
                        .filter(kba -> kba.asMap().values().contains(question))
                        .map(kba -> kbaInfo.stream()
                                .filter(info -> info.isDefined(QUESTION_ID)
                                        && info.get(QUESTION_ID).asString().equals(kba.getPointer().leaf()))
                                .findFirst()
                                .orElse(null))
                        .findFirst()
                        .orElse(null));
    }

    private boolean failCounterWithinLimits(KbaConfig kbaConfig, JsonValue userObject) {
        if (kbaConfig.getKbaAttemptsPropertyName() == null) {
            return true;
        }
        return userObject.get(ptr(config.kbaInfoAttribute())
                .child(kbaConfig.getKbaAttemptsPropertyName()).toString())
                .defaultTo(0).asInteger()
                <= json(kbaConfig.getNumberOfAttemptsAllowed()).defaultTo(Integer.MAX_VALUE).asInteger();
    }

    private void incrementFailCounter(TreeContext context, KbaConfig kbaConfig, JsonValue userObject)
            throws NodeProcessException {
        if (kbaConfig.getKbaAttemptsPropertyName() == null) {
            return;
        }
        logger.debug("Incrementing failed kba count of {}", userObject.get(FIELD_CONTENT_ID));
        String failCounter = kbaConfig.getKbaAttemptsPropertyName();
        patchObject(idmIntegrationService, realm, context.request.locales, context.identityResource,
                userObject.get(FIELD_CONTENT_ID).asString(),
                object(field(ptr(config.kbaInfoAttribute()).child(failCounter).toString(),
                        userObject.get(config.kbaInfoAttribute()).get(failCounter).defaultTo(0).asInteger() + 1)));
    }

    private void resetFailCounter(TreeContext context, KbaConfig kbaConfig, JsonValue userObject)
            throws NodeProcessException {
        if (kbaConfig.getKbaAttemptsPropertyName() == null) {
            return;
        }
        logger.debug("Resetting failed kba count of {}", userObject.get(FIELD_CONTENT_ID));
        String failCounter = kbaConfig.getKbaAttemptsPropertyName();
        patchObject(idmIntegrationService, realm, context.request.locales, context.identityResource,
                userObject.get(FIELD_CONTENT_ID).asString(),
                object(field(ptr(config.kbaInfoAttribute()).child(failCounter).toString(), 0)));
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(config.identityAttribute())
        };
    }
}
