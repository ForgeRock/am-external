
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
package org.forgerock.openam.auth.nodes.framework;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.audit.context.AuditRequestContext.getAuditRequestContext;
import static org.forgerock.openam.auth.nodes.framework.BackChannelNodeConstants.REDIRECT_URI_STATE_KEY;
import static org.forgerock.openam.auth.nodes.framework.BackChannelNodeConstants.TRANSACTION_ID_STATE_KEY;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.forgerock.am.authn.backchannel.BackChannelTransactionInitializeRequestData;
import org.forgerock.am.authn.backchannel.BackChannelTransactionInitializeRequestData.Subject;
import org.forgerock.am.authn.backchannel.BackChannelTransactionInitializeResponseData;
import org.forgerock.am.authn.backchannel.BackChannelTransactionService;
import org.forgerock.am.authn.backchannel.exceptions.BackChannelAuthErrorCode;
import org.forgerock.am.authn.backchannel.exceptions.BackChannelAuthException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.annotations.sm.I18nKey;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.framework.typeadapters.BackchannelTree;
import org.forgerock.openam.auth.nodes.validators.GreaterThanZeroValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.services.baseurl.InvalidBaseUrlException;
import org.forgerock.openam.sm.annotations.adapters.TimeUnit;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * This node initializes a back channel transaction and stores the transaction ID and redirect URI in the shared state.
 */
@Node.Metadata(outcomeProvider = BackChannelInitNode.BackChannelInitOutcomeProvider.class,
        configClass = BackChannelInitNode.Config.class, tags =  {"utilities", "backchannel"})
public class BackChannelInitNode implements Node {

    private static final Logger logger = LoggerFactory.getLogger(BackChannelInitNode.class);

    private final Realm realm;
    private final Config config;
    private final BackChannelTransactionService backChannelAuthnService;
    private final BaseURLProviderFactory baseURLProviderFactory;
    private JsonValue auditEntry;

    @Inject
    BackChannelInitNode(@Assisted Realm realm, @Assisted Config config,
            BackChannelTransactionService backChannelAuthnService, BaseURLProviderFactory baseURLProviderFactory) {
        this.realm = realm;
        this.config = config;
        this.backChannelAuthnService = backChannelAuthnService;
        this.baseURLProviderFactory = baseURLProviderFactory;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        NodeState nodeState = context.getStateFor(this);
        Subject subject;
        try {
            subject = getSubject(nodeState);
        } catch (NodeProcessException e) {
            logger.warn("Error retrieving subject from node state", e);
            return Action.goTo(BackChannelInitOutcomeProvider.ERROR_OUTCOME).build();
        }
        Map<String, Object> data = getData(nodeState);
        BackChannelTransactionInitializeRequestData requestData =
                new BackChannelTransactionInitializeRequestData("service", config.tree(), subject, data, null,
                        config.timeout().map(Duration::toSeconds).orElse(null), config.allowRetry());
        try {
            var response = backChannelAuthnService.initialize(requestData, realm,
                    getUrl(realm, config.redirectUrlType(), context));
            nodeState.putShared(TRANSACTION_ID_STATE_KEY, response.transaction());
            if (!RedirectUrlType.NONE.equals(config.redirectUrlType())) {
                nodeState.putShared(REDIRECT_URI_STATE_KEY, response.redirectUri());
            }
            recordAuditDetails(response);
            return Action.goTo(BackChannelInitOutcomeProvider.CREATED_OUTCOME).build();
        } catch (BackChannelAuthException e) {
            if (e.errorCode().equals(BackChannelAuthErrorCode.INVALID_IDENTITY)) {
                return Action.goTo(BackChannelInitOutcomeProvider.UNKNOWN_SUBJECT).build();
            }
            logger.error("Error initializing back channel transaction", e);
            return Action.goTo(BackChannelInitOutcomeProvider.ERROR_OUTCOME).build();
        }
    }

    private URI getUrl(Realm realm, RedirectUrlType redirectUrlType, TreeContext context) throws NodeProcessException {
        var baseURLProvider = baseURLProviderFactory.get(realm);
        try {
            return switch (redirectUrlType) {
            case GET -> new URI(String.format("%s?realm=%s", baseURLProvider.getRootURL(context.request.servletRequest)
                                                                     + "/UI/Login", realm));
            case POST -> new URI(baseURLProvider.getRealmURL(context.request.servletRequest,
                    "/json", realm) + "/authenticate");
            case CUSTOM -> new URI(String.format("%s?realm=%s",
                    config.customRedirectUrl().orElseThrow(() -> new NodeProcessException(
                            "Custom redirect URL is not set.")), realm));
            case NONE -> null;
            };
        } catch (InvalidBaseUrlException | URISyntaxException e) {
            throw new NodeProcessException("Error creating redirect URL", e);
        }
    }



    private void recordAuditDetails(BackChannelTransactionInitializeResponseData response)
            throws BackChannelAuthException {
        var status = backChannelAuthnService.checkStatus(response.transaction());
        status.auditTrackingIds()
                .forEach(trackingId -> getAuditRequestContext().addTrackingId(trackingId));

        auditEntry = JsonValue.json(object(
                field("trackingIds", status.auditTrackingIds())));
        if (status.subject() != null) {
            auditEntry.put("subject", object(
                    field("type", status.subject().type()),
                    field("name", status.subject().name())));
        }
    }

    private Map<String, Object> getData(NodeState nodeState) {
        return config.dataKey()
                       .map(nodeState::getObject)
                       .filter(JsonValue::isMap)
                       .map(JsonValue::asMap)
                       .orElse(null);
    }

    private Subject getSubject(NodeState nodeState) throws NodeProcessException {
        if (!SubjectType.NONE.equals(config.idType())) {
            if (config.usernameKey().isEmpty() || config.usernameKey().get().isEmpty()
                        || !nodeState.isDefined(config.usernameKey().get())) {
                throw new NodeProcessException("The username key is required but not found in the shared state.");
            }
            return new Subject(config.idType().type,
                    requireNonNull(nodeState.get(config.usernameKey().get())).asString());
        } else if (config.usernameKey().isPresent() && !config.usernameKey().get().isEmpty()) {
            logger.debug("The username attribute is ignored because the subject type is set to NONE.");
        }
        return null;
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        return auditEntry;
    }

    @Override
    public InputState[] getInputs() {
        List<InputState> inputs = new ArrayList<>();
        config.usernameKey().ifPresent(key -> {
            inputs.add(new InputState(key, true));
        });
        config.dataKey().ifPresent(key -> {
            inputs.add(new InputState(key, false));
        });
        return inputs.toArray(InputState[]::new);
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
                new OutputState(TRANSACTION_ID_STATE_KEY, Map.of(
                        BackChannelInitOutcomeProvider.CREATED_OUTCOME, true,
                        BackChannelInitOutcomeProvider.ERROR_OUTCOME, false)),
                new OutputState(REDIRECT_URI_STATE_KEY, Map.of(
                        BackChannelInitOutcomeProvider.CREATED_OUTCOME, true,
                        BackChannelInitOutcomeProvider.ERROR_OUTCOME, false))
        };
    }

    /**
     * The configuration for the {@link BackChannelInitNode}.
     */
    public interface Config {

        /**
         * The tree to be used for the back channel transaction.
         * @return The tree to be used for the back channel transaction.
         */
        @Attribute(order = 100, requiredValue = true)
        @BackchannelTree
        String tree();

        /**
         * The type of subject to be used in the backchannel transaction.
         * <p>If set to NONE then no subject type is used.
         * @return The type of subject to be used in the backchannel transaction.
         */
        @Attribute(order = 200, requiredValue = true)
        default SubjectType idType() {
            return SubjectType.NONE;
        }

        /**
         * The key to be used to find the username in shared state for the backchannel transaction.
         * @return The key to be used for the username in the backchannel transaction.
         */
        @Attribute(order = 300)
        Optional<String> usernameKey();

        /**
         * The data to be submitted with the backchannel transaction.
         * @return The data to be submitted with the backchannel transaction.
         */
        @Attribute(order = 500)
        Optional<String> dataKey();

        /**
         * The type of redirect URL to be provided for the backchannel transaction.
         * @return The type of redirect URL to be provided for the backchannel transaction.
         */
        @Attribute(order = 600, requiredValue = true)
        default RedirectUrlType redirectUrlType() {
            return RedirectUrlType.GET;
        }

        /**
         * The custom redirect URL to be used for the backchannel transaction.
         * <p>This is only used if the redirectUrlSource is set to CUSTOM_URL.
         * @return The custom redirect URL to be used for the backchannel transaction.
         */
        @Attribute(order = 800)
        Optional<String> customRedirectUrl();

        /**
         * The timeout in seconds for the backchannel transaction.
         * <p>If not set, the default timeout for the realm will be used.
         * @return The timeout in seconds for the backchannel transaction.
         */
        @Attribute(order = 900, validators = GreaterThanZeroValidator.class)
        @TimeUnit(SECONDS)
        Optional<Duration> timeout();

        /**
         * Whether to allow retrying the backchannel transaction.
         * @return Whether to allow retrying the backchannel transaction.
         */
        @Attribute(order = 1000, requiredValue = true)
        default Boolean allowRetry() {
            return true;
        }
    }

    /**
     * The type of subject to be used in the backchannel transaction.
     */
    public enum SubjectType {
        /**
         * The subject type is a user.
         */
        @I18nKey("subjectType.user")
        USER("user"),
        /**
         * The subject type is an agent.
         */
        @I18nKey("subjectType.agent")
        AGENT("agent"),
        /**
         * No subject type is specified.
         */
        @I18nKey("subjectType.none")
        NONE(null);

        private final String type;

        SubjectType(String type) {
            this.type = type;
        }
    }

    /**
     * The type of redirect URL to be provided for the backchannel transaction.
     */
    public enum RedirectUrlType {
        /**
         * The redirect URL type is GET.
         */
        @I18nKey("redirectUrlType.get")
        GET,
        /**
         * The redirect URL type is POST.
         */
        @I18nKey("redirectUrlType.post")
        POST,
        /**
         * The redirect URL type is CUSTOM.
         * <p>This requires a custom URL to be provided in the node configuration.
         */
        @I18nKey("redirectUrlType.custom")
        CUSTOM,
        /**
         * No redirect URL type is specified.
         */
        @I18nKey("redirectUrlType.none")
        NONE
    }

    /**
     * Outcome provider for the JourneyInitNode.
     */
    public static class BackChannelInitOutcomeProvider implements BoundedOutcomeProvider {

        private static final String CREATED_OUTCOME = "created";
        private static final String UNKNOWN_SUBJECT = "unknown";
        private static final String ERROR_OUTCOME = "error";

        @Override
        public List<Outcome> getAllOutcomes(PreferredLocales locales) {
            var bundle = locales.getBundleInPreferredLocale(BackChannelInitNode.class.getName(),
                    BackChannelInitNode.class.getClassLoader());
            return List.of(new Outcome(CREATED_OUTCOME, bundle.getString(CREATED_OUTCOME)),
                    new Outcome(UNKNOWN_SUBJECT, bundle.getString(UNKNOWN_SUBJECT)),
                    new Outcome(ERROR_OUTCOME, bundle.getString(ERROR_OUTCOME)));
        }

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            if (nodeAttributes == null || nodeAttributes.isNull()) {
                return getAllOutcomes(locales);
            }
            if (SubjectType.NONE.equals(SubjectType.valueOf(nodeAttributes.get("idType").asString()))) {
                var bundle = locales.getBundleInPreferredLocale(BackChannelInitNode.class.getName(),
                        BackChannelInitNode.class.getClassLoader());
                return List.of(new Outcome(CREATED_OUTCOME, bundle.getString(CREATED_OUTCOME)),
                        new Outcome(ERROR_OUTCOME, bundle.getString(ERROR_OUTCOME)));
            }
            return getAllOutcomes(locales);
        }
    }
}
