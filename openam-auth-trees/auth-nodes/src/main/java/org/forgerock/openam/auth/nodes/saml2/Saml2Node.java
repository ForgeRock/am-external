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
 * Copyright 2017-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.saml2;

import static com.sun.identity.saml2.common.SAML2Constants.RELAY_STATE;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.AM_LOCATION_COOKIE;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.RESPONSE_KEY;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.SAML2_CLIENT_BUNDLE_NAME;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.SAML_VERIFY_ERROR_CODE;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.SUCCESS_URL;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.ACCOUNT_EXISTS;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.NO_ACCOUNT;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.ATTRIBUTES_SHARED_STATE_KEY;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_INFO_SHARED_STATE_KEY;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_NAMES_SHARED_STATE_KEY;
import static org.forgerock.openam.utils.CollectionUtils.getFirstItem;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.am.saml2.api.AuthComparison;
import org.forgerock.am.saml2.api.Saml2Options;
import org.forgerock.am.saml2.api.Saml2SsoException;
import org.forgerock.am.saml2.api.Saml2SsoInitiator;
import org.forgerock.am.saml2.impl.Saml2ClientConstants;
import org.forgerock.am.saml2.impl.Saml2ResponseData;
import org.forgerock.am.saml2.impl.Saml2SsoResponseUtils;
import org.forgerock.am.saml2.profile.Saml2SsoResult;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Action.ActionBuilder;
import org.forgerock.openam.auth.node.api.Namespace;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.headers.CookieUtilsWrapper;
import org.forgerock.am.identity.application.IdentityException;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.IdType;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaManager;

/**
 * This authentication node adds support for SAML2 authentication as part of an authentication tree. The node
 * implementation will act as a SAML2 SP and will trigger an SP initiated authentication with the configured remote
 * identity provider.
 */
@Node.Metadata(outcomeProvider = AbstractSocialAuthLoginNode.SocialAuthOutcomeProvider.class,
        configClass = Saml2Node.Config.class,
        tags = {"social", "federation"},
        namespace = Namespace.PRODUCT)
public class Saml2Node extends AbstractDecisionNode {

    private static final Logger logger = LoggerFactory.getLogger(Saml2Node.class);
    private static final String MAIL_KEY_MAPPING = "mail";
    private static final String CACHE_KEY = "cacheKey";
    private static final String IS_TRANSIENT = "isTransient";
    private static final String UID = "uid";

    private final Config config;
    private final Saml2SsoInitiator ssoInitiator;
    private final Saml2SsoResponseUtils responseUtils;
    private final CookieUtilsWrapper cookieUtilsWrapper;
    private final SAML2MetaManager metaManager;
    private final LegacyIdentityService identityService;
    private final String idpEntityId;
    private final String metaAlias;
    private final Realm realm;
    private ResourceBundle bundle = null;
    private String spEntityId;
    private String storageKey;
    private String sessionIndex;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The IDP entity ID.
         *
         * @return The IDP entity ID.
         */
        @Attribute(order = 100, requiredValue = true)
        default String idpEntityId() {
            return "https://";
        }

        /**
         * The hosted service provider's metaAlias.
         *
         * @return The hosted service provider's metaAlias.
         */
        @Attribute(order = 200, requiredValue = true)
        default String metaAlias() {
            return "/sp";
        }

        /**
         * Whether the identity provider is allowed to create a new NameID value for the user.
         *
         * @return Whether the identity provider is allowed to create a new NameID value for the user.
         */
        @Attribute(order = 300)
        default boolean allowCreate() {
            return true;
        }

        /**
         * The comparison method the identity provider should use when determining the authentication method.
         *
         * @return The authentication context comparison method.
         */
        @Attribute(order = 400)
        default AuthComparison authComparison() {
            return AuthComparison.MINIMUM;
        }

        /**
         * The authentication context class reference.
         *
         * @return The authentication context class reference.
         */
        @Attribute(order = 500)
        default List<String> authnContextClassRef() {
            return emptyList();
        }

        /**
         * The authentication context declaration reference.
         *
         * @return The authentication context declaration reference.
         */
        @Attribute(order = 600)
        default List<String> authnContextDeclRef() {
            return emptyList();
        }

        /**
         * The request binding AM should use when sending the authentication request.
         *
         * @return The request binding AM should use when sending the authentication request.
         */
        @Attribute(order = 700)
        default RequestBinding requestBinding() {
            return RequestBinding.HTTP_REDIRECT;
        }

        /**
         * The binding the IDP should use when returning the SAML response.
         *
         * @return The binding the IDP should use when returning the SAML response.
         */
        @Attribute(order = 800)
        default Binding binding() {
            return Binding.HTTP_ARTIFACT;
        }

        /**
         * Whether the IDP should force re-authentication of the user.
         *
         * @return Whether the IDP should force re-authentication of the user.
         */
        @Attribute(order = 900)
        default boolean forceAuthn() {
            return false;
        }

        /**
         * Whether the IDP should silently check if the user is already authenticated.
         *
         * @return Whether the IDP should silently check if the user is already authenticated.
         */
        @Attribute(order = 1000)
        default boolean isPassive() {
            return false;
        }

        /**
         * The NameID Format the IDP should use when constructing the assertion.
         *
         * @return The NameID Format the IDP should use when constructing the assertion.
         */
        @Attribute(order = 1100)
        default String nameIdFormat() {
            return "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";
        }
    }

    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     * @param realm The realm the node is in.
     * @param ssoInitiator The SSO initiator implementation.
     * @param responseUtils The SAML2 SSO response utils implementation.
     * @param cookieUtilsWrapper The cookie utils implementation.
     * @param metaManager The SAML2 meta manager.
     * @param identityService The identity service.
     */
    @Inject
    public Saml2Node(@Assisted Config config, @Assisted Realm realm, Saml2SsoInitiator ssoInitiator,
            Saml2SsoResponseUtils responseUtils, CookieUtilsWrapper cookieUtilsWrapper, SAML2MetaManager metaManager,
            LegacyIdentityService identityService) {
        this.config = config;
        this.realm = realm;
        this.ssoInitiator = ssoInitiator;
        this.responseUtils = responseUtils;
        this.cookieUtilsWrapper = cookieUtilsWrapper;
        this.metaManager = metaManager;
        this.identityService = identityService;

        idpEntityId = config.idpEntityId();
        metaAlias = config.metaAlias();
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        this.bundle = context.request.locales.getBundleInPreferredLocale(SAML2_CLIENT_BUNDLE_NAME,
                getClass().getClassLoader());
        final HttpServletRequest request = context.request.servletRequest;
        final HttpServletResponse response = context.request.servletResponse;

        try {
            spEntityId = metaManager.getEntityByMetaAlias(metaAlias);
            if (parseBoolean(request.getParameter(Saml2ClientConstants.ERROR_PARAM_KEY))) {
                throw newResponseHandlingException(request);
            } else if (context.request.parameters.containsKey(RESPONSE_KEY)) {
                return handleReturnFromRedirect(context, request, response).build();
            }
            IDPSSODescriptorType idpDescriptor = metaManager.getIDPSSODescriptor(realm.asPath(), idpEntityId);
            SPSSODescriptorType spDescriptor = metaManager.getSPSSODescriptor(realm.asPath(), spEntityId);
            String nameIdFormat = SAML2Utils.verifyNameIDFormat(config.nameIdFormat(), spDescriptor, idpDescriptor);
            String username = context.sharedState.get(USERNAME).asString();
            return Action
                    .send(initiateSamlLoginAtIdp(request, response, nameIdFormat))
                    .withUniversalId(identityService.getUniversalId(username, realm.asPath(), IdType.USER))
                    .build();
        } catch (SAML2Exception e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Performs similar to SPSSOFederate.initiateAuthnRequest by returning to the next auth stage with a redirect
     * (either GET or POST depending on the config) which triggers remote IdP authentication.
     */
    private Callback initiateSamlLoginAtIdp(HttpServletRequest request, HttpServletResponse response,
            String nameIdFormat) throws NodeProcessException {
        Options saml2Options = Options.defaultOptions();
        saml2Options.set(Saml2Options.ALLOW_CREATE, config.allowCreate());
        saml2Options.set(Saml2Options.AUTH_COMPARISON, config.authComparison());
        saml2Options.set(Saml2Options.AUTH_CONTEXT_CLASS_REF, config.authnContextClassRef());
        saml2Options.set(Saml2Options.AUTH_CONTEXT_DECL_REF, config.authnContextDeclRef());
        saml2Options.set(Saml2Options.FORCE_AUTHN, config.forceAuthn());
        saml2Options.set(Saml2Options.IS_PASSIVE, config.isPassive());
        saml2Options.set(Saml2Options.NAME_ID_FORMAT, nameIdFormat);
        saml2Options.set(Saml2Options.REQUEST_BINDING, config.requestBinding().toString());
        saml2Options.set(Saml2Options.RESPONSE_BINDING, config.binding().toString());
        try {
            return ssoInitiator.initiateSso(request, response, realm, spEntityId, idpEntityId, saml2Options);
        } catch (Saml2SsoException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Once we're back from the ACS, we need to validate that we have not errored during the proxying process. Then we
     * detect if we need to perform a local linking authentication chain, or if the user is already locally linked, we
     * need to look up the already-linked username.
     */
    private ActionBuilder handleReturnFromRedirect(TreeContext context, HttpServletRequest request,
            HttpServletResponse response) throws NodeProcessException, SAML2Exception {
        cookieUtilsWrapper.addCookieToResponseForRequestDomains(request, response, AM_LOCATION_COOKIE, "", 0);
        storageKey = getFirstItem(context.request.parameters.get(RESPONSE_KEY));

        Saml2ResponseData data;
        try {
            data = responseUtils.readSaml2ResponseData(storageKey);
        } catch (SAML2TokenRepositoryException ex) {
            logger.error("Unable to retrieve SAML2 state from SFO", ex);
            throw new NodeProcessException(bundle.getString(Saml2ClientConstants.SAML_FAILOVER_ERROR_CODE));
        }
        if (data == null) {
            logger.error("Unable to complete SAML2 authentication, response data not found");
            throw new NodeProcessException(bundle.getString(Saml2ClientConstants.SAML_FAILOVER_ERROR_CODE));
        } else {
            try {
                responseUtils.removeSaml2ResponseData(storageKey);
            } catch (SAML2TokenRepositoryException e) {
                logger.error("Failed to remove data for responseKey starting with {}", storageKey.substring(0, 6), e);
            }
        }

        Subject subject = data.getSubject();
        Assertion assertion = data.getAssertion();
        sessionIndex = data.getSessionIndex();
        JsonValue sharedState = context.sharedState;

        Saml2SsoResult ssoResult = responseUtils.getSsoResultWithoutLocalLogin(realm, spEntityId, idpEntityId,
                subject, assertion, storageKey);

        String relayState = null;
        if (context.request.parameters.containsKey(RELAY_STATE)) {
            relayState = getFirstItem(context.request.parameters.get(RELAY_STATE));
        }
        String username = identityService.getIdentityName(ssoResult.getUniversalId());
        ActionBuilder actionBuilder;
        if (doesUserExist(ssoResult.getUniversalId())) {
            actionBuilder = Action.goTo(ACCOUNT_EXISTS.name());
            if (username != null) {
                actionBuilder.withIdentifiedIdentity(username, IdType.USER);
            }
        } else {
            actionBuilder = Action.goTo(NO_ACCOUNT.name());
        }
        return setSessionProperties(actionBuilder.replaceSharedState(
                updateSharedState(username, ssoResult, assertion, sharedState, relayState)), ssoResult.getNameId());
    }

    private boolean doesUserExist(String universalId) throws NodeProcessException {
        try {
            return isNotBlank(universalId) && identityService.doesIdentityExist(universalId);
        } catch (IdentityException e) {
            throw new NodeProcessException("Error occurred verifying identities existence", e);
        }
    }

    private JsonValue updateSharedState(String username, Saml2SsoResult ssoResult, Assertion assertion,
            JsonValue sharedState, String relayState) throws NodeProcessException {
        Map<String, Set<String>> attributes = new HashMap<>();
        try {
            if (username != null) {
                sharedState.put(USERNAME, username);
            }

            if (ssoResult.shouldPersistNameId()) {
                NameIDInfo info = new NameIDInfo(spEntityId, idpEntityId, ssoResult.getNameId(), SP_ROLE, false);
                attributes.putAll(AccountUtils.convertToAttributes(info, null));
            }
            attributes.putAll(responseUtils.mapSamlAttributes(realm, spEntityId, idpEntityId, ssoResult, assertion));
        } catch (SAML2Exception e) {
            throw new NodeProcessException(e);
        }

        sharedState.put(USER_INFO_SHARED_STATE_KEY, json(object(
                field(ATTRIBUTES_SHARED_STATE_KEY, convertToMapOfList(attributes)),
                field(USER_NAMES_SHARED_STATE_KEY,
                        object(field(SharedStateConstants.USERNAME, array(username)),
                                field(UID, array(username)))))));

        if (isNotEmpty(attributes.get(MAIL_KEY_MAPPING))) {
            sharedState.put(EMAIL_ADDRESS, getFirstItem(attributes.get(MAIL_KEY_MAPPING)));
        } else {
            logger.debug("Unable to determine email address based on the mapped SAML attributes");
        }
        if (isNotBlank(relayState)) {
            sharedState.put(SUCCESS_URL, relayState);
        }
        return sharedState;
    }

    private Map<String, List<String>> convertToMapOfList(Map<String, Set<String>> mapToConvert) {
        return mapToConvert.entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
    }

    private NodeProcessException newResponseHandlingException(HttpServletRequest request) {
        final String errorCode = request.getParameter(Saml2ClientConstants.ERROR_CODE_PARAM_KEY);
        String errorMessage = request.getParameter(Saml2ClientConstants.ERROR_MESSAGE_PARAM_KEY);

        if (StringUtils.isNotEmpty(errorMessage)) {
            logger.error("AuthConsumer endpoint reported error code {} and message: {}", errorCode, errorMessage);
        } else {
            logger.error("AuthConsumer endpoint reported error code: {}", errorCode);
        }
        errorMessage = bundle.getString(StringUtils.isNotEmpty(errorCode) ? errorCode : SAML_VERIFY_ERROR_CODE);

        return new NodeProcessException(errorMessage);
    }

    /**
     * Adds information necessary for the session to be federated completely (if attributes are being drawn in, and to
     * configure ready for SLO).
     */
    private ActionBuilder setSessionProperties(ActionBuilder actionBuilder, NameID nameId)
            throws NodeProcessException {
        //we need the following for idp initiated slo as well as sp, so always include it
        if (sessionIndex != null) {
            actionBuilder.putSessionProperty(SAML2Constants.SESSION_INDEX, sessionIndex);
        }
        try {
            actionBuilder.putSessionProperty(SAML2Constants.NAMEID, nameId.toXMLString(true, true));
        } catch (SAML2Exception e) {
            throw new NodeProcessException(e);
        }
        actionBuilder.putSessionProperty(IS_TRANSIENT,
                Boolean.toString(SAML2Constants.NAMEID_TRANSIENT_FORMAT.equals(nameId.getFormat())));
        actionBuilder.putSessionProperty(CACHE_KEY, storageKey);
        return actionBuilder;
    }

    /**
     * The currently supported bindings for sending SAML authentication requests.
     */
    public enum RequestBinding {
        /**
         * HTTP-Redirect binding.
         */
        HTTP_REDIRECT {
            @Override
            public String toString() {
                return "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";
            }
        },
        /**
         * HTTP-POST binding.
         */
        HTTP_POST {
            @Override
            public String toString() {
                return "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
            }
        },
    }

    /**
     * The supported bindings for receiving SAML responses from the identity provider.
     */
    public enum Binding {
        /**
         * HTTP-Artifact binding.
         */
        HTTP_ARTIFACT {
            @Override
            public String toString() {
                return "HTTP-Artifact";
            }
        },
        /**
         * HTTP-POST binding.
         */
        HTTP_POST {
            @Override
            public String toString() {
                return "HTTP-POST";
            }
        }
    }
}
