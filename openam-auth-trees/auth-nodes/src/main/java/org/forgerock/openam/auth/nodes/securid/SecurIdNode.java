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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.securid;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.securid.SecurIdCallbackUtils.CANCEL_BUTTON;
import static org.forgerock.openam.auth.nodes.securid.SecurIdCallbackUtils.CANCEL_NOT_SELECTED_INDEX;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.PUSH;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SecurIdMethodType.VOICE_OR_SMS;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AUTHN_ATTEMPT_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.AVAILABLE_METHODS;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CHOSEN_METHOD;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.CONFIRMATION_CB;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.PUSH_REFERENCE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.NodeStateKeys.RESPONSE_MESSAGE_ID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.OutcomeId.CANCEL;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.OutcomeId.ERROR;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.OutcomeId.FAILURE;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.OutcomeId.NOT_ENROLLED;
import static org.forgerock.openam.auth.nodes.securid.SecurIdNode.OutcomeId.SUCCESS;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.PasswordCallback;

import jakarta.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider.LocalizedMessageProviderFactory;
import org.forgerock.openam.auth.node.api.Namespace;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.http.typeadapter.HttpClient;
import org.forgerock.openam.sm.annotations.adapters.SecretPurpose;
import org.forgerock.openam.sm.validation.URLValidator;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;

/**
 * Node that provides multifactor authentication using RSA SecurID.
 * It supports various methods such as Push, Tokencode, Voice, SMS, Approve, Device Biometrics, and QR Code.
 * The node allows users to select their preferred MFA method and handles the authentication process accordingly.
 */
@Node.Metadata(outcomeProvider = SecurIdNode.OutcomeProvider.class, configClass = SecurIdNode.Config.class,
        tags = {"mfa", "multi-factor authentication" }, namespace = Namespace.PRODUCT)
public class SecurIdNode extends AbstractDecisionNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurIdNode.class);
    private static final String BUNDLE = SecurIdNode.class.getName();

    private final Config config;
    private final SecurIdRestClient restClient;
    private final SecurIdResponseHandler responseHandler;
    private final SecurIdVerifyService verifyService;
    private final LocalizedMessageProvider localizedMessageProvider;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The base URL for the SecurID REST API.
         * @return The base URL.
         */
        @Attribute(order = 100, requiredValue = true, validators = URLValidator.class)
        String baseUrl();

        /**
         * The client ID for the SecurID REST API.
         * @return The client ID.
         */
        @Attribute(order = 200)
        Optional<String> clientId();

        /**
         * The assurance policy to use for the SecurID authentication.
         * @return The assurance policy.
         */
        @Attribute(order = 300)
        Optional<String> assurancePolicy();

        /**
         * The client key for the SecurID REST API.
         * @return The client key.
         */
        @Attribute(order = 400, requiredValue = true, resourceName = "clientKeySecretLabelIdentifier")
        @SecretPurpose("am.authentication.nodes.securid.%s.client.key")
        Purpose<GenericSecret> clientKeyPurpose();

        /**
         * The authentication attempt timeout value to send in the authentication initialization request.
         * @return The timeout value in seconds.
         */
        @Attribute(order = 500)
        default int authnAttemptTimeout() {
            return 180;
        }

        /**
         * The HTTP client to use for making requests to the SecurID REST API.
         * @return The HTTP client name.
         * @see org.forgerock.openam.http.service.HttpClientService
         */
        @Attribute(order = 600)
        @HttpClient
        default String httpClient() {
            return "";
        }

        /**
         * The prompt message to display when asking the user to select their preferred MFA method.
         * @return The prompt message.
         */
        @Attribute(order = 700)
        default Map<Locale, String> mfaChoicePrompt() {
            return emptyMap();
        }

        /**
         * The message to display while waiting for a response from the user.
         * This is typically shown when waiting for a push notification or QR code scan.
         * @return The waiting message.
         */
        @Attribute(order = 800)
        default Map<Locale, String> waitingMessage() {
            return emptyMap();
        }

    }

    @Inject
    SecurIdNode(@Assisted Config config,
                @Assisted Realm realm,
                SecurIdRestClient.Factory restClientFactory,
                SecurIdVerifyService.Factory verifyServiceFactory,
                SecurIdResponseHandler.Factory responseHandlerFactory,
                AvailableMethodHandler.Factory methodHandlerFactory,
                LocalizedMessageProviderFactory localizedMessageProviderFactory) {
        this.config = config;
        restClient = restClientFactory.create(realm, config.baseUrl(), config.clientKeyPurpose(),
                config.authnAttemptTimeout(), config.httpClient());
        verifyService = verifyServiceFactory.create(restClient);
        var methodHandler = methodHandlerFactory.create(verifyService);
        responseHandler = responseHandlerFactory.create(methodHandler);
        localizedMessageProvider = localizedMessageProviderFactory.create(realm);
    }

    @Override
    public Action process(TreeContext context) {
        try {
            NodeState nodeState = context.getStateFor(this);
            String waitingMessage = localizedMessageProvider.getLocalizedMessage(context, SecurIdNode.class,
                    config.waitingMessage(), "default.waitingMessage");
            String mfaChoicePrompt = localizedMessageProvider.getLocalizedMessage(context, SecurIdNode.class,
                    config.mfaChoicePrompt(), "default.mfaChoicePrompt");

            if (!context.hasCallbacks()) {
                var response = restClient.initialize(getUsername(nodeState),
                        config.clientId().orElse(null),
                        config.assurancePolicy().orElse(null));
                var action = responseHandler.handleInitialize(response, nodeState, waitingMessage, mfaChoicePrompt);
                if (!action.sendingCallbacks()) {
                    cleanUpNodeState(nodeState);
                }
                return action;
            } else {
                if (cancelWasPushed(context, nodeState)) {
                    cleanUpNodeState(nodeState);
                    return Action.goTo(CANCEL).build();
                }

                var choiceCallback = context.getCallback(ChoiceCallback.class);
                if (choiceCallback.isPresent()) {
                    var challengeCallbacks = handleChoiceCallback(choiceCallback.get(), nodeState, waitingMessage);
                    return Action.send(challengeCallbacks).build();
                }

                var method = SecurIdMethod.fromMethodId(requireNonNull(nodeState.get(CHOSEN_METHOD)).asString());
                var response = restClient.verify(
                        requireNonNull(nodeState.get(RESPONSE_MESSAGE_ID)).asString(),
                        requireNonNull(nodeState.get(AUTHN_ATTEMPT_ID)).asString(),
                        getSubjectCredentials(method, context, nodeState));
                var action = responseHandler.handleVerify(response, nodeState, method, waitingMessage, mfaChoicePrompt);
                if (!action.sendingCallbacks()) {
                    cleanUpNodeState(nodeState);
                }
                return action;
            }
        } catch (Exception ex) {
            cleanUpNodeState(context.getStateFor(this));
            LOGGER.error("Exception occurred: ", ex);
            return Action.goTo(ERROR).withHeader("Error occurred").build();
        }
    }

    private static String getUsername(NodeState nodeState) throws NodeProcessException {
        if (!nodeState.isDefined(USERNAME)) {
            throw new NodeProcessException("username does not exist in node state");
        }
        var username = requireNonNull(nodeState.get(USERNAME)).asString();
        if (isBlank(username)) {
            throw new NodeProcessException("username does not exist in node state");
        }
        return username;
    }

    private static boolean cancelWasPushed(TreeContext context, NodeState nodeState) {
        var callback = context.getCallback(ConfirmationCallback.class);
        if (callback.isEmpty()) {
            return false;
        }
        var confirmationCallback = callback.get();
        if (confirmationCallback.getSelectedIndex() == CANCEL_NOT_SELECTED_INDEX) {
            return false;
        }
        String pushedButton = requireNonNull(nodeState.get(CONFIRMATION_CB))
                .asList(String.class)
                .get(confirmationCallback.getSelectedIndex());
        return pushedButton.equalsIgnoreCase(CANCEL_BUTTON);
    }

    private List<Callback> handleChoiceCallback(ChoiceCallback choiceCallback, NodeState nodeState,
                                                String waitingMessage)
            throws NodeProcessException, NoSuchSecretException, URISyntaxException, IOException {
        int selectedIndex = choiceCallback.getSelectedIndexes()[0];
        String methodId = requireNonNull(nodeState.get(AVAILABLE_METHODS)).asList(String.class).get(selectedIndex);
        nodeState.remove(AVAILABLE_METHODS);
        nodeState.putShared(CHOSEN_METHOD, methodId);
        return verifyService.beginVerification(SecurIdMethod.fromMethodId(methodId), nodeState, waitingMessage);
    }

    private static List<SubjectCredentials> getSubjectCredentials(SecurIdMethod method, TreeContext context,
                                                          NodeState nodeState) {
        if (method.type().equals(PUSH)) {
            var referenceId = Optional.ofNullable(nodeState.get(PUSH_REFERENCE_ID)).map(JsonValue::asString)
                    .orElse(null);
            return List.of(new SubjectCredentials(method.name(), referenceId, null));
        } else {
            var methodName = (method.type().equals(VOICE_OR_SMS)) ? method.name() + "_VERIFY_CODE" : method.name();
            return List.of(new SubjectCredentials(methodName, null,
                    List.of(new SubjectCredentials.CollectedInput(methodName, getToken(context), "STRING"))));
        }
    }

    private static String getToken(TreeContext context) {
        return context.getAllCallbacks().stream()
                .filter(c -> c instanceof PasswordCallback)
                .map(c -> (PasswordCallback) c)
                .map(pc -> String.copyValueOf(pc.getPassword()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Defines the possible outcomes from this node.
     */
    static final class OutcomeId {
        static final String SUCCESS = "SUCCESS";
        static final String ERROR = "ERROR";
        static final String FAILURE = "FAILURE";
        static final String NOT_ENROLLED = "NOTENROLLED";
        static final String CANCEL = "CANCEL";
    }

    /**
     * Defines the possible outcomes from this node.
     */
    public static class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(SUCCESS, bundle.getString("SuccessOutcome")),
                    new Outcome(FAILURE, bundle.getString("FailureOutcome")),
                    new Outcome(NOT_ENROLLED, bundle.getString("NotEnrolledOutcome")),
                    new Outcome(CANCEL, bundle.getString("CancelOutcome")),
                    new Outcome(ERROR, bundle.getString("ErrorOutcome")));
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
                new InputState(USERNAME, true),
                new InputState(PUSH_REFERENCE_ID, false),
                new InputState(RESPONSE_MESSAGE_ID, false),
                new InputState(AUTHN_ATTEMPT_ID, false),
                new InputState(AVAILABLE_METHODS, false),
                new InputState(CHOSEN_METHOD, false),
                new InputState(CONFIRMATION_CB, false)
        };
    }

    /**
     * Keys used in the node state to store information about the current state of the node.
     */
    static final class NodeStateKeys {
        static final String AUTHN_ATTEMPT_ID = "SecurID-authnAttemptId";
        static final String AVAILABLE_METHODS = "SecurID-availableMethods";
        static final String CHOSEN_METHOD = "SecurID-chosenMethod";
        static final String CONFIRMATION_CB = "SecurID-confirmationCallback";
        static final String PUSH_REFERENCE_ID = "SecurID-PushReferenceId";
        static final String RESPONSE_MESSAGE_ID = "SecurID-responseMessageId";

        private NodeStateKeys() {
        }
    }

    private static void cleanUpNodeState(NodeState nodeState) {
        nodeState.remove(AUTHN_ATTEMPT_ID);
        nodeState.remove(AVAILABLE_METHODS);
        nodeState.remove(CHOSEN_METHOD);
        nodeState.remove(CONFIRMATION_CB);
        nodeState.remove(PUSH_REFERENCE_ID);
        nodeState.remove(RESPONSE_MESSAGE_ID);
    }
}
