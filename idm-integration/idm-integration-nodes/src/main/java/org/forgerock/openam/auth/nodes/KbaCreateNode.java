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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getKbaConfig;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getLocalisedMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.KbaCreateCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.integration.idm.KbaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Node for defining and updating KBA questions and answers for IDM objects.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass = KbaCreateNode.Config.class,
        tags = {"identity management"})
public class KbaCreateNode extends SingleOutcomeNode {
    private final Logger logger = LoggerFactory.getLogger(KbaCreateNode.class);
    private final KbaCreateNode.Config config;
    private final Realm realm;
    private final IdmIntegrationService idmIntegrationService;
    private final LocaleSelector localeSelector;

    private static final Pattern NON_WORD_PATTERN = Pattern.compile("\\W", Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * Configurable message.
         *
         * @return collection of localized messages
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default Map<Locale, String> message() {
            return Collections.emptyMap();
        }

        /**
         * Toggle for allowing user defined KBA questions.
         *
         * @return whether user's can define their own KBA questions
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        default boolean allowUserDefinedQuestions() {
            return true;
        }
    }

    /**
     * Construct a KbaCreateNode.
     *
     * @param config the configuration for this node
     * @param realm the realm for the tree this node is in
     * @param idmIntegrationService the IdmIntegrationService for communicating with IDM
     * @param localeSelector a LocaleSelector for choosing the correct message to display
     */
    @Inject
    public KbaCreateNode(@Assisted Config config, @Assisted Realm realm,
            IdmIntegrationService idmIntegrationService, LocaleSelector localeSelector) {
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
        this.localeSelector = localeSelector;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("KbaCreateNode started");
        logger.debug("Retrieving KBA configuration");
        KbaConfig kbaConfig = getKbaConfig(idmIntegrationService, realm, context.request.locales);
        List<KbaCreateCallback> newCallbacks = generateCallbacks(context, kbaConfig);

        if (context.getAllCallbacks().isEmpty()) {
            // Return the KBA callbacks
            logger.debug("Collecting KBA definitions");
            return send(newCallbacks).build();
        }

        List<KbaCreateCallback> callbacks = context.getCallbacks(KbaCreateCallback.class);

        // Prevent advancing if requirements have not been met or there is a problem with the callbacks returned
        if (!validateCallbacks(context, kbaConfig, callbacks)) {
            logger.debug("Re-collecting invalid KBA definitions");
            return send(transferCallbackData(context, kbaConfig, callbacks)).build();
        }

        // Save the returned data in the transientState for the KbaUpdate node to process
        JsonValue transientState = context.transientState.copy();
        idmIntegrationService.storeAttributeInState(transientState, kbaConfig.getKbaPropertyName(),
                mapCallbacksToQandA(context, kbaConfig, callbacks));

        return goToNext()
                .replaceSharedState(context.sharedState)
                .replaceTransientState(transientState)
                .build();
    }

    private List<KbaCreateCallback> generateCallbacks(TreeContext context, KbaConfig kbaConfig)
            throws NodeProcessException {
        List<String> questions = new ArrayList<>(fetchKbaQuestions(context, kbaConfig).values());
        List<KbaCreateCallback> callbacks = new LinkedList<>();
        for (int i = 0; i < kbaConfig.getMinimumAnswersToDefine(); i++) {
            callbacks.add(new KbaCreateCallback(
                    getLocalisedMessage(context, localeSelector, this.getClass(), config.message(), "message.default"),
                    questions, config.allowUserDefinedQuestions()));
        }
        return callbacks;
    }

    private boolean validateCallbacks(TreeContext context, KbaConfig kbaConfig, List<KbaCreateCallback> callbacks)
            throws NodeProcessException {
        List<String> questions = callbacks.stream()
                .filter(callback -> callback.getSelectedQuestion() != null)
                .map(callback -> NON_WORD_PATTERN.matcher(callback.getSelectedQuestion()).replaceAll("").toLowerCase())
                .collect(Collectors.toList());
        return questions.stream().distinct().count() == questions.size()
                && (mapCallbacksToQandA(context, kbaConfig, callbacks).size() >= kbaConfig.getMinimumAnswersToDefine());
    }

    private List<KbaCreateCallback> transferCallbackData(TreeContext context, KbaConfig kbaConfig,
            List<KbaCreateCallback> callbacks) throws NodeProcessException {
        List<KbaCreateCallback> newCallbacks = generateCallbacks(context, kbaConfig);
        for (int i = 0; i < newCallbacks.size(); i++) {
            if (callbacks.size() > i) {
                newCallbacks.get(i).setSelectedQuestion(callbacks.get(i).getSelectedQuestion());
                newCallbacks.get(i).setSelectedAnswer(callbacks.get(i).getSelectedAnswer());
            }
        }
        return newCallbacks;
    }

    private List<Map<String, Object>> mapCallbacksToQandA(TreeContext context, KbaConfig kbaConfig,
            List<KbaCreateCallback> callbacks) throws NodeProcessException {
        List<Map<String, Object>> kba = new ArrayList<>();
        Map<String, String> kbaQuestions = fetchKbaQuestions(context, kbaConfig);
        for (KbaCreateCallback callback : callbacks) {
            kba.add(convertQandA(kbaQuestions, callback.getSelectedQuestion(), callback.getSelectedAnswer()));
        }
        return kba;
    }

    private Map<String, Object> convertQandA(Map<String, String> kbaQuestions, String question, String answer) {
        for (Map.Entry<String, String> entry : kbaQuestions.entrySet()) {
            if (entry.getValue().equals(question)) {
                return object(field("questionId", entry.getKey()), field("answer", answer));
            }
        }
        return object(field("customQuestion", question), field("answer", answer));
    }

    /*
     * Example KBA configuration:
     * <pre>
     * {
     *    "_id": "kba",
     *    "kbaPropertyName": "kbaInfo",
     *    "minimumAnswersToDefine": 2,
     *    "minimumAnswersToVerify": 1,
     *    "questions": {
     *       "1": {
     *          "en": "What's your favorite color?",
     *          "en_GB": "What is your favourite colour?",
     *          "fr": "Quelle est votre couleur préférée?"
     *       },
     *       "2": {
     *          "en": "Who was your first employer?"
     *       }
     *    },
     *    "numberOfAttemptsAllowed": 3,
     *    "kbaAttemptsPropertyName": "kbaLockout"
     * }
     * </pre>
     */
    Map<String, String> fetchKbaQuestions(TreeContext context, KbaConfig kbaConfig) {
        Map<String, String> questions = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : kbaConfig.getQuestions().entrySet()) {
            Map<Locale, String> localizable = new LinkedHashMap<>();
            Map<String, String> entryValue = entry.getValue();
            for (Map.Entry<String, String> questionEntry : entryValue.entrySet()) {
                localizable.put(Locale.forLanguageTag(questionEntry.getKey()), questionEntry.getValue());
            }
            questions.put(entry.getKey(),
                    getLocalisedMessage(context, localeSelector, this.getClass(), localizable, "questions.default"));
        }
        return questions;
    }

    @Override
    public OutputState[] getOutputs() {
        try {
            logger.debug("retrieving kba configuration");
            return new OutputState[]{
                new OutputState(getKbaConfig(idmIntegrationService, realm, null).getKbaAttemptsPropertyName())
            };
        } catch (NodeProcessException e) {
            logger.warn("Failed to retrieve kba configuration");
            return new OutputState[]{};
        }
    }
}
