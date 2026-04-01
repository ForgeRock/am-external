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
 * Copyright 2023-2026 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.script;

import static org.forgerock.openam.auth.node.api.AuthScriptUtilities.JOURNEY;
import static org.forgerock.openam.auth.node.api.AuthScriptUtilities.LOCALES;
import static org.forgerock.openam.auth.node.api.AuthScriptUtilities.OAUTH_APPLICATION;
import static org.forgerock.openam.auth.node.api.AuthScriptUtilities.SAML_APPLICATION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.ACTION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.CALLBACKS_BUILDER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.CALLBACKS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.EXISTING_SESSION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HEADERS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.ID_REPO_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.QUERY_PARAMETER_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.REQUEST_COOKIES_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;

import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.NodeStateScriptWrapper;
import org.forgerock.openam.auth.node.api.ScriptedLocalizedMessage;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepositoryScriptWrapper;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.NextGenScriptBindings;
import org.forgerock.openam.scripting.domain.OAuthScriptedBindingObject;
import org.forgerock.openam.scripting.domain.SAMLScriptedBindingObject;

/**
 * Abstract parent class of the scripted decision node bindings.
 */
public abstract class BaseScriptedDecisionNodeBindings implements NextGenScriptBindings {
    /**
     * Resumed from suspend key for bindings.
     */
    protected static final String RESUMED_FROM_SUSPEND = "resumedFromSuspend";
    private static final String JWT_ASSERTION = "jwtAssertion";
    private static final String JWT_VALIDATOR = "jwtValidator";
    /**
     * The {@link NodeState} binding.
     */
    protected final NodeState nodeState;
    /**
     * The callbacks binding.
     */
    protected final List<? extends Callback> callbacks;
    /**
     * The headers binding.
     */
    protected final Map<String, List<String>> headers;
    /**
     * The query parameters binding.
     */
    protected final Map<String, List<String>> queryParameters;
    /**
     * The identity repository binding.
     */
    protected final ScriptedIdentityRepository scriptedIdentityRepository;
    /**
     * The resumed from suspend binding.
     */
    protected final boolean resumedFromSuspend;
    /**
     * The existing session binding.
     */
    protected final Map<String, String> existingSession;
    /**
     * The request cookies binding.
     */
    protected final Map<String, String> requestCookies;
    /**
     * The tree name binding.
     */
    protected final Tree tree;
    /**
     *  When the scripted decision node is executed as part of a SAML flow, this provides information to the script
     *  about the SAML application and incoming authentication request.
     */
    private final SAMLScriptedBindingObject samlApplication;
    /**
     *  When the scripted decision node is executed as part of an OAuth flow, this provides information to the script
     *  about the OAuth request and the client.
     */
    private final OAuthScriptedBindingObject oauthApplication;

    /**
     * The localized message binding.
     */
    protected final ScriptedLocalizedMessage scriptedLocalizedMessage;

    /**
     * Constructor for the BaseScriptedDecisionNodeBindings object.
     * @param builder the builder
     */

    protected BaseScriptedDecisionNodeBindings(Builder<?> builder) {
        this.nodeState = builder.nodeState;
        this.callbacks = builder.callbacks;
        this.headers = builder.headers;
        this.queryParameters = builder.queryParameters;
        this.scriptedIdentityRepository = builder.scriptedIdentityRepository;
        this.resumedFromSuspend = builder.resumedFromSuspend;
        this.existingSession = builder.existingSession;
        this.requestCookies = builder.requestCookies;
        this.tree = builder.tree;
        this.samlApplication = builder.samlApplication;
        this.oauthApplication = builder.oauthApplication;
        this.scriptedLocalizedMessage = builder.scriptedLocalizedMessage;
    }

    /**
     * Generates a bindings map containing all common bindings for next gen scripts.
     *
     * @return a bindings map containing all common bindings for next gen scripts
     */
    public BindingsMap commonNextGenBindings() {
        BindingsMap bindingsMap = new BindingsMap();
        bindingsMap.put(STATE_IDENTIFIER, new NodeStateScriptWrapper(nodeState));
        bindingsMap.put(CALLBACKS_IDENTIFIER, new ScriptedCallbacksWrapper(callbacks));
        bindingsMap.put(HEADERS_IDENTIFIER, headers);
        bindingsMap.put(QUERY_PARAMETER_IDENTIFIER, queryParameters);
        bindingsMap.put(ID_REPO_IDENTIFIER, new ScriptedIdentityRepositoryScriptWrapper(scriptedIdentityRepository));
        bindingsMap.put(RESUMED_FROM_SUSPEND, resumedFromSuspend);
        bindingsMap.putIfDefined(EXISTING_SESSION, existingSession);
        bindingsMap.put(JWT_ASSERTION, new JwtAssertionScriptWrapper());
        bindingsMap.put(JWT_VALIDATOR, new JwtValidatorScriptWrapper());
        bindingsMap.put(ACTION, new ActionWrapper());
        bindingsMap.put(CALLBACKS_BUILDER, new ScriptedCallbacksBuilder());
        bindingsMap.put(REQUEST_COOKIES_IDENTIFIER, requestCookies);
        bindingsMap.put(JOURNEY, new ScriptedTreeWrapper(tree));
        bindingsMap.put(SAML_APPLICATION, samlApplication);
        bindingsMap.put(OAUTH_APPLICATION, oauthApplication);
        bindingsMap.put(LOCALES, scriptedLocalizedMessage);
        return bindingsMap;
    }

    /**
     * Step 1 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep1<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets the {@link NodeState}.
         *
         * @param nodeState the {@link NodeState}
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep2<T> withNodeState(NodeState nodeState);
    }

    /**
     * Step 2 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep2<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets the callbacks.
         *
         * @param callbacks the callbacks
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep3<T> withCallbacks(List<? extends Callback> callbacks);
    }

    /**
     * Step 3 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep3<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets the headers.
         *
         * @param headers the headers
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep4<T> withHeaders(Map<String, List<String>> headers);
    }

    /**
     * Step 4 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep4<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets the query parameters.
         *
         * @param queryParameters the query parameters
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep5<T> withQueryParameters(Map<String, List<String>> queryParameters);
    }

    /**
     * Step 5 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep5<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets the scripted identity repository.
         *
         * @param scriptedIdentityRepository the scripted identity repository
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep6<T> withScriptedIdentityRepository(
                ScriptedIdentityRepository scriptedIdentityRepository);
    }

    /**
     * Step 6 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep6<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets the resumed from suspend boolean.
         *
         * @param resumedFromSuspend true if resumed from suspend
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep7<T> withResumedFromSuspend(boolean resumedFromSuspend);
    }

    /**
     * Step 8 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep7<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets existing session.
         *
         * @param existingSession the existing session
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep8<T> withExistingSession(Map<String, String> existingSession);
    }

    /**
     * Step 9 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep8<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets existing session.
         *
         * @param existingSession the existing session
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep9<T> withRequestCookies(Map<String, String> existingSession);
    }

    /**
     * Step 9 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep9<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets the tree.
         *
         * @param tree the tree.
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep10<T> withTree(Tree tree);
    }


    /**
     * Step 10 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep10<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets the saml application binding.
         *
         * @param samlApplication the saml application binding
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep11<T> withSamlApplication(
                SAMLScriptedBindingObject samlApplication);
    }

    /**
     * Step 11 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep11<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets the oauth application binding.
         *
         * @param oauthApplication the oauth application binding
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep12<T> withOauthApplication(OAuthScriptedBindingObject oauthApplication);
    }

    /**
     * Step 12 of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsStep12<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Sets the scripted localized message binding.
         *
         * @param scriptedLocalizedMessage the scripted localized message binding
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsFinalStep<T> withScriptedLocalizedMessage(
                ScriptedLocalizedMessage scriptedLocalizedMessage);
    }

    /**
     * Final step of the builder.
     * @param <T> The concrete type of bindings returned by the builder.
     */
    public interface BaseScriptedDecisionNodeBindingsFinalStep<T extends BaseScriptedDecisionNodeBindings> {
        /**
         * Builds the implementing class of {@link BaseScriptedDecisionNodeBindings}.
         *
         * @return the implementing class of {@link BaseScriptedDecisionNodeBindings}
         */
        T build();
    }

    /**
     * Builder object to construct a {@link ScriptedDecisionNodeBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     * @param <T> The concrete type of bindings returned by the builder.
     */
    protected abstract static class Builder<T extends BaseScriptedDecisionNodeBindings>
            implements BaseScriptedDecisionNodeBindingsStep1<T>, BaseScriptedDecisionNodeBindingsStep2<T>,
            BaseScriptedDecisionNodeBindingsStep3<T>, BaseScriptedDecisionNodeBindingsStep4<T>,
            BaseScriptedDecisionNodeBindingsStep5<T>, BaseScriptedDecisionNodeBindingsStep6<T>,
            BaseScriptedDecisionNodeBindingsStep7<T>, BaseScriptedDecisionNodeBindingsStep8<T>,
            BaseScriptedDecisionNodeBindingsStep9<T>, BaseScriptedDecisionNodeBindingsStep10<T>,
            BaseScriptedDecisionNodeBindingsStep11<T>, BaseScriptedDecisionNodeBindingsStep12<T>,
            BaseScriptedDecisionNodeBindingsFinalStep<T> {

        private NodeState nodeState;
        private ScriptedIdentityRepository scriptedIdentityRepository;
        private boolean resumedFromSuspend;
        private List<? extends Callback> callbacks;
        private Map<String, List<String>> queryParameters;
        private Map<String, List<String>> headers;
        private Map<String, String> existingSession;
        private Map<String, String> requestCookies;
        private Tree tree;
        private SAMLScriptedBindingObject samlApplication;
        private OAuthScriptedBindingObject oauthApplication;
        private ScriptedLocalizedMessage scriptedLocalizedMessage;

        /**
         * Set the nodeState for the builder.
         *
         * @param nodeState The nodeState.
         * @return The next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep2<T> withNodeState(NodeState nodeState) {
            this.nodeState = nodeState;
            return this;
        }

        /**
         * Set the callbacks for the builder.
         *
         * @param callbacks The List of callbacks.
         * @return The next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep3<T> withCallbacks(List<? extends Callback> callbacks) {
            this.callbacks = callbacks;
            return this;
        }

        /**
         * Set the headers for the builder.
         *
         * @param headers The http call headers.
         * @return The next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep4<T> withHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Set the queryParameters for the builder.
         *
         * @param queryParameters The queryParameters of the request.
         * @return The next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep5<T> withQueryParameters(
                Map<String, List<String>> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }

        /**
         * Set the scriptedIdentityRepository for the builder.
         *
         * @param scriptedIdentityRepository The {@link ScriptedIdentityRepository}.
         * @return The next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep6<T> withScriptedIdentityRepository(
                ScriptedIdentityRepository scriptedIdentityRepository) {
            this.scriptedIdentityRepository = scriptedIdentityRepository;
            return this;
        }

        /**
         * Set if the tree is resumedFromSuspend for the builder.
         *
         * @param resumedFromSuspend If the tree was resumed from suspended state.
         * @return The next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep7<T> withResumedFromSuspend(boolean resumedFromSuspend) {
            this.resumedFromSuspend = resumedFromSuspend;
            return this;
        }

        /**
         * Set the existingSession.
         *
         * @param existingSession the existing session.
         * @return The next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep8<T> withExistingSession(Map<String, String> existingSession) {
            this.existingSession = existingSession;
            return this;
        }

        /**
         * Set the requestCookies.
         *
         * @param requestCookies the request cookies.
         * @return The next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep9<T> withRequestCookies(Map<String, String> requestCookies) {
            this.requestCookies = requestCookies;
            return this;
        }


        /**
         * Set the tree.
         *
         * @param tree the tree.
         * @return The next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep10<T> withTree(Tree tree) {
            this.tree = tree;
            return this;
        }

        /**
         * Set the saml application binding for the builder.
         *
         * @param samlApplication the saml application binding.
         * @return the next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep11<T> withSamlApplication(
                SAMLScriptedBindingObject samlApplication) {
            this.samlApplication = samlApplication;
            return this;
        }

        /**
         * Set the oauth application binding for the builder.
         *
         * @param oauthApplication the oauth application binding.
         * @return the next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep12<T> withOauthApplication(
                OAuthScriptedBindingObject oauthApplication) {
            this.oauthApplication = oauthApplication;
            return this;
        }

        /**
         * Set the scripted localized message binding.
         *
         * @param scriptedLocalizedMessage the scripted localized message binding
         * @return the next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsFinalStep<T> withScriptedLocalizedMessage(
                ScriptedLocalizedMessage scriptedLocalizedMessage) {
            this.scriptedLocalizedMessage = scriptedLocalizedMessage;
            return this;
        }

    }
}
