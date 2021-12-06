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
 * Copyright 2015-2021 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.saml2;

import static com.sun.identity.shared.Constants.UNIVERSAL_IDENTIFIER;
import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.AM_LOCATION_COOKIE;
import static org.forgerock.openam.authentication.modules.saml2.Constants.BINDING;
import static org.forgerock.openam.authentication.modules.saml2.Constants.DEFAULT_CALLBACK;
import static org.forgerock.openam.authentication.modules.saml2.Constants.ENTITY_NAME;
import static org.forgerock.openam.authentication.modules.saml2.Constants.LOCAL_CHAIN;
import static org.forgerock.openam.authentication.modules.saml2.Constants.LOGIN_STEP;
import static org.forgerock.openam.authentication.modules.saml2.Constants.MAX_CALLBACKS_INJECTED;
import static org.forgerock.openam.authentication.modules.saml2.Constants.META_ALIAS;
import static org.forgerock.openam.authentication.modules.saml2.Constants.NAME_ID_FORMAT;
import static org.forgerock.openam.authentication.modules.saml2.Constants.REDIRECT;
import static org.forgerock.openam.authentication.modules.saml2.Constants.REDIRECT_CALLBACK;
import static org.forgerock.openam.authentication.modules.saml2.Constants.REQ_BINDING;
import static org.forgerock.openam.authentication.modules.saml2.Constants.SLO_ENABLED;
import static org.forgerock.openam.authentication.modules.saml2.Constants.SLO_RELAY_STATE;
import static org.forgerock.openam.authentication.modules.saml2.Constants.START;
import static org.forgerock.openam.authentication.modules.saml2.Constants.STATE_ERROR;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;

import java.security.Principal;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.saml2.profile.SPCache;
import org.forgerock.am.saml2.api.AuthComparison;
import org.forgerock.am.saml2.api.Saml2Options;
import org.forgerock.am.saml2.api.Saml2SsoException;
import org.forgerock.am.saml2.api.Saml2SsoInitiator;
import org.forgerock.am.saml2.impl.Saml2ClientConstants;
import org.forgerock.am.saml2.impl.Saml2ResponseData;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.plugins.DefaultLibrarySPAccountMapper;
import com.sun.identity.saml2.plugins.SAML2PluginsUtils;
import com.sun.identity.saml2.plugins.SPAttributeMapper;
import com.sun.identity.saml2.profile.ResponseInfo;
import com.sun.identity.saml2.profile.SPACSUtils;
import com.sun.identity.saml2.profile.SPSSOFederate;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.locale.L10NMessageImpl;
import com.sun.identity.sm.DNMapper;

/**
 * SAML2 Authentication Module, acting from the SP's POV. Will redirect to a SAML2 IdP for authentication, then
 * return and complete.  Supports HTTP-Redirect and HTTP-POST bindings for sending the AuthnRequests, and HTTP-POST,
 * HTTP-Artifact binding for processing SAML responses.
 */
public class SAML2 extends AMLoginModule {

    private static final Logger logger = LoggerFactory.getLogger(SAML2.class);
    private static final Logger DEBUG = LoggerFactory.getLogger(SAML2.class);
    private static final String BUNDLE_NAME = "amAuthSAML2";
    private static final String PROPERTY_VALUES_SEPARATOR = "|";

    //From config
    private String entityName;
    private String metaAlias;
    private String reqBinding;
    private String binding;
    private String localChain;
    private String sloRelayState;
    private boolean singleLogoutEnabled;
    private String nameIDFormat;
    private final Options saml2Options = Options.defaultOptions();

    //Internal state
    private Assertion authnAssertion;
    private Subject assertionSubject;
    private Principal principal;
    private AuthContext authenticationContext;
    private String realm;
    private int previousLength = 0;
    private ResourceBundle bundle = null;
    private String sessionIndex;
    private boolean isTransient;
    private ResponseInfo respInfo;
    private String storageKey;

    private SAML2MetaManager metaManager;

    @Override
    public void init(javax.security.auth.Subject subject, Map sharedState, Map options) {
        saml2Options.set(Saml2Options.ALLOW_CREATE, getBooleanMapAttr(options, Constants.ALLOW_CREATE, true));
        String authComparison = getMapAttr(options, Constants.AUTH_COMPARISON, "exact");
        saml2Options.set(Saml2Options.AUTH_COMPARISON, AuthComparison.valueOf(authComparison.toUpperCase()));
        saml2Options.set(Saml2Options.AUTH_CONTEXT_CLASS_REF,
                asList(getMapAttr(options, Constants.AUTHN_CONTEXT_CLASS_REF, "").split("\\|"))
                        .stream().filter(StringUtils::isNotEmpty).collect(Collectors.toList()));
        saml2Options.set(Saml2Options.AUTH_CONTEXT_DECL_REF,
                asList(getMapAttr(options, Constants.AUTHN_CONTEXT_DECL_REF, "").split("\\|"))
                        .stream().filter(StringUtils::isNotEmpty).collect(Collectors.toList()));
        saml2Options.set(Saml2Options.FORCE_AUTHN, getBooleanMapAttr(options, Constants.FORCE_AUTHN, false));
        saml2Options.set(Saml2Options.IS_PASSIVE, getBooleanMapAttr(options, Constants.IS_PASSIVE, false));
        saml2Options.set(Saml2Options.NAME_ID_FORMAT, getMapAttr(options, NAME_ID_FORMAT));
        saml2Options.set(Saml2Options.REQUEST_BINDING, getMapAttr(options, REQ_BINDING));
        saml2Options.set(Saml2Options.RESPONSE_BINDING, getMapAttr(options, BINDING));

        nameIDFormat = CollectionHelper.getMapAttr(options, NAME_ID_FORMAT);
        entityName = CollectionHelper.getMapAttr(options, ENTITY_NAME);
        metaAlias = CollectionHelper.getMapAttr(options, META_ALIAS);
        reqBinding = CollectionHelper.getMapAttr(options, REQ_BINDING);
        binding = CollectionHelper.getMapAttr(options, BINDING);
        localChain = CollectionHelper.getMapAttr(options, LOCAL_CHAIN);
        singleLogoutEnabled = getBooleanMapAttr(options, SLO_ENABLED, false);
        sloRelayState = CollectionHelper.getMapAttr(options, SLO_RELAY_STATE);
        metaManager = SAML2Utils.getSAML2MetaManager();
        realm = DNMapper.orgNameToRealmName(getRequestOrg());

        bundle = amCache.getResBundle(BUNDLE_NAME, getLoginLocale());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int process(final Callback[] realCallbacks, int state) throws LoginException {

        final HttpServletRequest request = getHttpServletRequest();
        final HttpServletResponse response = getHttpServletResponse();

        if (null == request) {
            return processError(bundle.getString("samlNullRequest"),
                    "SAML2 :: process() : Http Request is null - programmatic login is not supported.");
        }

        try {
            final String spName = metaManager.getEntityByMetaAlias(metaAlias);
            if (authenticationContext != null) {
                state = LOGIN_STEP;
            }

            switch (state) {
            case START:
                return initiateSAMLLoginAtIDP(response, request);
            case REDIRECT:
                if (parseBoolean(request.getParameter(Saml2ClientConstants.ERROR_PARAM_KEY))) {
                    DEBUG.debug("SAML2 :: process() : Handling error from redirect.");
                    return handleRedirectError(request, response);
                }
                final String responseKey = getResponseKey(request);
                if (StringUtils.isEmpty(responseKey)) {
                    DEBUG.debug("SAML2 :: process() : Response key is empty, restarting SSO flow.");
                    // Flow has been restarted before authn has successfully completed at IdP.
                    return initiateSAMLLoginAtIDP(response, request);
                }
                DEBUG.debug("SAML2 :: process() : Response key is populated, handling redirect.");
                return handleReturnFromRedirect(state, request, spName, response, responseKey);
            case LOGIN_STEP:
                return stepLogin(realCallbacks, state);
            default:
                return processError(bundle.getString("invalidLoginState"), "Unrecognised login state: {}", state);
            }

        } catch (SAML2Exception e) {
            return processError(e, null, "SAML2 :: process() : Authentication Error");
        }
    }

    /**
     * Performs similar to SPSSOFederate.initiateAuthnRequest by returning to the next auth stage
     * with a redirect (either GET or POST depending on the config) which triggers remote IdP authentication.
     */
    private int initiateSAMLLoginAtIDP(final HttpServletResponse response, final HttpServletRequest request)
            throws SAML2Exception, AuthLoginException {

        final String spEntityId = SPSSOFederate.getSPEntityId(metaAlias);

        Saml2SsoInitiator saml2SsoInitiator = InjectorHolder.getInstance(Saml2SsoInitiator.class);
        try {
            Callback callback = saml2SsoInitiator.initiateSso(request, response, Realms.of(realm), spEntityId,
                    entityName, saml2Options);
            replaceCallback(REDIRECT, REDIRECT_CALLBACK, callback);
        } catch (RealmLookupException | Saml2SsoException ex) {
            logger.error("Unable to initiate SAML2 SSO", ex);
            throw new AuthLoginException(Constants.AM_AUTH_SAML2, "invalidLoginState", null);
        }

        return REDIRECT;
    }

    /**
     * Once we're back from the ACS we detect if we need to perform
     * a local linking authentication chain or if the user is already
     * locally linked, we need to look up the already-linked username.
     */
    private int handleReturnFromRedirect(final int state, final HttpServletRequest request, final String spName,
            final HttpServletResponse response, final String responseKey) throws AuthLoginException {

        //first make sure to delete the cookie
        removeCookiesForRedirects(request, response);

        final String username;
        Saml2ResponseData data = null;

        if (!StringUtils.isBlank(responseKey)) {
            try {
                data = (Saml2ResponseData) SAML2FailoverUtils.retrieveSAML2Token(responseKey);
            } catch (SAML2TokenRepositoryException e) {
                return processError(bundle.getString("samlFailoverError"),
                        "SAML2.handleReturnFromRedirect : Error reading from failover map.", e);
            }
        }

        if (data == null) {
            return processError(bundle.getString("localLinkError"), "SAML2 :: handleReturnFromRedirect() : "
                    + "Unable to perform local linking - response data not found");
        } else {
            try {
                SAML2FailoverUtils.deleteSAML2Token(responseKey);
            } catch (SAML2TokenRepositoryException e) {
                logger.error("Failed to remove data for responseKey {}", responseKey, e);
            }
            SPCache.samlResponseDataHash.put(responseKey, data);
        }

        storageKey = responseKey;
        assertionSubject = data.getSubject();
        authnAssertion = data.getAssertion();
        sessionIndex = data.getSessionIndex();
        respInfo = data.getResponseInfo();

        try { //you're already linked or we auto looked up user
            username = SPACSUtils.getSsoResultWithoutLocalLogin(assertionSubject, authnAssertion,
                    realm, spName, metaManager, entityName, storageKey).getUniversalId();
            if (SAML2PluginsUtils.isDynamicProfile(realm)) {
                String spEntityId = SPSSOFederate.getSPEntityId(metaAlias);
                if (shouldPersistNameID(spEntityId)) {
                    NameIDInfo info = new NameIDInfo(spEntityId, entityName, getNameId(), SAML2Constants.SP_ROLE,
                            false);
                    setUserAttributes(AccountUtils.convertToAttributes(info, null));
                }
            }
            if (username != null) {
                principal = new SAML2Principal(username);
                return success(authnAssertion, getNameId(), username);
            }
        } catch (SAML2Exception e) {
            return processError(e, null, "SAML2.handleReturnFromRedirect : Unable to perform user lookup.");
        }

        if (StringUtils.isBlank(localChain)) {
            return processError(bundle.getString("localLinkError"), "SAML2 :: handleReturnFromRedirect() : "
                    + "Unable to perform local linking - local auth chain not found.");
        }

        //generate a sub-login context, owned by this module, and start login sequence to it
        authenticationContext = new AuthContext(realm);
        authenticationContext.login(AuthContext.IndexType.SERVICE, localChain, null, null, null, null);

        return injectCallbacks(null, state);
    }

    /**
     * Retrieve the response key from the request.
     * @param request The Http Servlet Request.
     * @return The response key returned from the IdP, or null if not present.
     */
    private String getResponseKey(final HttpServletRequest request) {
        if (request.getParameter("jsonContent") != null) {
            return JsonValueBuilder.toJsonValue(request.getParameter("jsonContent")).get("responsekey").asString();
        } else {
            return request.getParameter(Saml2ClientConstants.RESPONSE_KEY);
        }
    }

    /**
     * Grab error code/message and display to user via processError.
     */
    private int handleRedirectError(final HttpServletRequest request, final HttpServletResponse response) throws
            AuthLoginException {
        removeCookiesForRedirects(request, response);

        final String errorCode = request.getParameter(Saml2ClientConstants.ERROR_CODE_PARAM_KEY);
        final String errorMessage = request.getParameter(Saml2ClientConstants.ERROR_MESSAGE_PARAM_KEY);
        if (StringUtils.isNotEmpty(errorMessage)) {
            return processError(errorMessage, "SAML2 :: handleReturnFromRedirect() : "
                    + "error forwarded from saml2AuthAssertionConsumer.jsp.  Error code - {}. "
                    + "Error message - {}", String.valueOf(errorCode), errorMessage);
        } else if (StringUtils.isNotEmpty(errorCode)) {
            return processError(bundle.getString(errorCode), "SAML2 :: handleReturnFromRedirect() : "
                    + "error forwarded from saml2AuthAssertionConsumer.jsp.  Error code - {}. "
                    + "Error message - {}", errorCode, errorMessage);
        } else {
            return processError(bundle.getString("samlVerify"), "SAML2 :: handleReturnFromRedirect() : "
                    + "error forwarded from saml2AuthAssertionConsumer.jsp.  Error code - {}. "
                    + "Error message - {}", errorMessage);
        }
    }

    /**
     * Clears out the cookie from the user agent so we don't leave detritus.
     */
    private void removeCookiesForRedirects(final HttpServletRequest request, final HttpServletResponse response) {
        final Set<String> domains = AuthClientUtils.getCookieDomainsForRequest(request);

        // Set the return URL Cookie
        for (String domain : domains) {
            CookieUtils.addCookieToResponse(response,
                    CookieUtils.newCookie(AM_LOCATION_COOKIE, "", 0, "/", domain));
        }
    }

    /**
     * In conjuncture with injectCallbacks, steps through an internal auth chain (stored in authenticationContext) until
     * it's completed by repeatedly injecting the callbacks from the internal chain's modules and submitting
     * them until the status has confirmed failed or succeeded.
     */
    private int stepLogin(final Callback[] realCallbacks, final int state) throws AuthLoginException {

        if (authenticationContext == null || authenticationContext.getStatus().equals(AuthContext.Status.FAILED)) {
            return processError(bundle.getString("samlLocalAuthFailed"),
                    "SAML2 :: process() : failed to perform local authentication - {} ",
                    bundle.getString("samlLocalAuthFailed"));
        } else if (authenticationContext.getStatus().equals(AuthContext.Status.IN_PROGRESS)) {
            return injectCallbacks(realCallbacks, state);
        } else if (authenticationContext.getStatus().equals(AuthContext.Status.SUCCESS)) {
            try {
                final NameID nameId = getNameId();
                final String userName = authenticationContext.getSSOToken().getProperty(UNIVERSAL_IDENTIFIER);
                linkAccount(userName, nameId);
                return success(authnAssertion, nameId, userName);
            } catch (L10NMessageImpl l10NMessage) {
                return processError(l10NMessage, null,
                        "SAML2 :: process() : failed to perform local authentication - {} ",
                        l10NMessage.getL10NMessage(getLoginLocale()));
            } finally {
                authenticationContext.logout();
            }
        }

        return processError(bundle.getString("invalidLoginState"), "SAML2 :: stepLogin() : unexpected login state");
    }

    /**
     * Sets the auth module's logged-in username via storeUsernamePasswd, triggers call
     * to add information necessary for SLO (if configured) and returns success.
     */
    private int success(Assertion assertion, NameID nameId, String userName) throws AuthLoginException, SAML2Exception {
        setSessionProperties(nameId);
        linkAttributeValues(assertion, userName);
        DEBUG.debug("SAML2 :: User Authenticated via SAML2 - {}", getPrincipal().getName());
        storeUsernamePasswd(DNUtils.DNtoName(getPrincipal().getName()), null);
        return ISAuthConstants.LOGIN_SUCCEED;
    }

    /**
     * Submits completed callbacks (from the just-completed step - the first time this is called realCallbacks should
     * be null as there is no just-completed step in the internal auth module), and injects the next lot if there
     * are any.
     */
    private int injectCallbacks(final Callback[] realCallbacks, final int state) throws AuthLoginException {

        if (authenticationContext.hasMoreRequirements()) {
            //replace existing callbacks
            if (realCallbacks != null) {
                authenticationContext.submitRequirements(realCallbacks);
            }

            if (authenticationContext.hasMoreRequirements()) {
                return injectAndReturn(state);
            } else { //completed auth, status should be failure or success, allow stepLogin to return
                return finishLoginModule(state);
            }
        }

        return processError(bundle.getString("invalidLoginState"),
                "SAML2 :: injectCallbacks() : Authentication Module - invalid login state");
    }

    /**
     * Draws the next set of callbacks on to the current (externally-facing) auth module's step.
     */
    private int injectAndReturn(int state) throws AuthLoginException {
        Callback[] injectedCallbacks = authenticationContext.getRequirements();

        while (injectedCallbacks.length == 0) {
            authenticationContext.submitRequirements(injectedCallbacks);
            if (authenticationContext.hasMoreRequirements()) {
                injectedCallbacks = authenticationContext.getRequirements();
            } else { //completed auth with zero callbacks status should be failure or success, allow stepLogin to return
                return finishLoginModule(state);
            }
        }

        replaceHeader(LOGIN_STEP,
                ((PagePropertiesCallback)
                        authenticationContext.getAuthContextLocal().getLoginState().getReceivedInfo()[0]).getHeader());
        if (injectedCallbacks.length > MAX_CALLBACKS_INJECTED) {
            return processError(bundle.getString("samlLocalAuthFailed"),
                    "SAML2 :: injectAndReturn() : Local authentication failed");
        }

        if (previousLength > 0) { //reset
            for (int i = 0; i < previousLength; i++) {
                replaceCallback(LOGIN_STEP, i, DEFAULT_CALLBACK);
            }
        }

        for (int i = 0; i < injectedCallbacks.length; i++) {
            replaceCallback(LOGIN_STEP, i, injectedCallbacks[i]);
        }

        previousLength = injectedCallbacks.length;

        return LOGIN_STEP;
    }

    /**
     * Finishes a login module and then progresses to the next state.
     */
    private int finishLoginModule(int state) throws AuthLoginException {
        if (authenticationContext.getStatus().equals(AuthContext.Status.IN_PROGRESS)) {
            return processError(bundle.getString("invalidLoginState"),
                    "SAML2 :: injectCallbacks() : Authentication Module - invalid login state");
        }
        return stepLogin(null, state);
    }

    /**
     * Reads the authenticating user's SAML2 NameId from the stored map. Decrypts if necessary.
     */
    private NameID getNameId() throws SAML2Exception {
        final EncryptedID encId = assertionSubject.getEncryptedID();
        final String spName = metaManager.getEntityByMetaAlias(metaAlias);
        final Set<PrivateKey> decryptionKeys = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveValidDecryptionCredentials(realm, spName, SP);

        NameID nameId = assertionSubject.getNameID();

        if (encId != null) {
            nameId = encId.decrypt(decryptionKeys);
        }
        return nameId;
    }

    /**
     * Adds information necessary for the session to be federated completely (if attributes are being
     * drawn in, and to configure ready for SLO).
     */
    private void setSessionProperties(NameID nameId) throws AuthLoginException, SAML2Exception {
        //if we support single logout sp initiated from the auth module's resulting session
        setUserSessionProperty(SAML2Constants.SINGLE_LOGOUT, String.valueOf(singleLogoutEnabled));

        if (singleLogoutEnabled && StringUtils.isNotEmpty(sloRelayState)) { //we also need to store the relay state
            setUserSessionProperty(SAML2Constants.RELAY_STATE, sloRelayState);
            // RelayState property name is not unique and can be overwritten in session, so also store separately
            setUserSessionProperty(SAML2Constants.SINGLE_LOGOUT_URL, sloRelayState);
        }

        //we need the following for idp initiated slo as well as sp, so always include it
        if (sessionIndex != null) {
            setUserSessionProperty(SAML2Constants.SESSION_INDEX, sessionIndex);
        }
        setUserSessionProperty(SAML2Constants.IDPENTITYID, entityName);
        setUserSessionProperty(SAML2Constants.SPENTITYID, SPSSOFederate.getSPEntityId(metaAlias));
        setUserSessionProperty(SAML2Constants.METAALIAS, metaAlias);
        setUserSessionProperty(SAML2Constants.REQ_BINDING, reqBinding);
        setUserSessionProperty(SAML2Constants.NAMEID, nameId.toXMLString(true, true));
        setUserSessionProperty(Constants.IS_TRANSIENT, Boolean.toString(isTransient));
        setUserSessionProperty(Constants.REQUEST_ID, respInfo.getResponse().getInResponseTo());
        setUserSessionProperty(SAML2Constants.BINDING, binding);
        setUserSessionProperty(Constants.CACHE_KEY, storageKey);
    }

    /**
     * Performs the functions of linking attribute values that have been received from the assertion
     * by building them into appropriate strings and asking the auth service to migrate them into session
     * properties once authentication is completed.
     */
    private void linkAttributeValues(Assertion assertion, String userName)
            throws AuthLoginException, SAML2Exception {

        final String spName = metaManager.getEntityByMetaAlias(metaAlias);
        final SPSSOConfigElement spssoconfig = metaManager.getSPSSOConfig(realm, spName);
        final boolean needAssertionEncrypted =
                parseBoolean(SAML2Utils.getAttributeValueFromSPSSOConfig(spssoconfig,
                        SAML2Constants.WANT_ASSERTION_ENCRYPTED));
        final boolean needAttributeEncrypted =
                SPACSUtils.getNeedAttributeEncrypted(needAssertionEncrypted, spssoconfig);
        final Set<PrivateKey> decryptionKeys = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveValidDecryptionCredentials(realm, spName, SP);
        final List<Attribute> attrs = SPACSUtils.getSAMLAttributes(assertion, needAttributeEncrypted, decryptionKeys);

        final SPAttributeMapper attrMapper = SAML2Utils.getSPAttributeMapper(realm, spName);

        final Map<String, Set<String>> attrMap;

        try {
            attrMap = attrMapper.getAttributes(attrs, userName, spName, entityName, realm);
        }  catch (SAML2Exception se) {
            return; //no attributes
        }

        setUserAttributes(attrMap);

        for (String name : attrMap.keySet()) {
            Set<String> value = attrMap.get(name);
            StringBuilder toStore = new StringBuilder();

            if (CollectionUtils.isNotEmpty(value)) {
                // | is defined as the property value delimiter, cf FMSessionProvider#setProperty
                for (String toAdd : value) {
                    toStore.append(com.sun.identity.shared.StringUtils.getEscapedValue(toAdd))
                        .append(PROPERTY_VALUES_SEPARATOR);
                }
                toStore.deleteCharAt(toStore.length() - 1);
            }
            setUserSessionProperty(name, toStore.toString());
        }
    }

    /**
     * Links SAML2 accounts once all local auth steps have completed and we have a local principalId,
     * sets the local principal to a new SAML2Principal with that ID.
     */
    private void linkAccount(final String principalId, final NameID nameId)
            throws SAML2MetaException, AuthenticationException {

        final String spEntityId = metaManager.getEntityByMetaAlias(metaAlias);

        try {
            NameIDInfo info = new NameIDInfo(spEntityId, entityName, nameId, SAML2Constants.SP_ROLE, false);
            DEBUG.debug("SAML2 :: Local User {} Linked to Federation Account - {}", principalId, nameId.getValue());

            if (shouldPersistNameID(spEntityId)) {
                AccountUtils.setAccountFederation(info, principalId);
            }

            principal = new SAML2Principal(principalId);
        } catch (SAML2Exception e) {
            // exception logged later
            throw new AuthenticationException(BUNDLE_NAME, "localLinkError", new Object[0]);
        }
    }

    private boolean shouldPersistNameID(String spEntityId) throws SAML2Exception {
        final DefaultLibrarySPAccountMapper spAccountMapper = new DefaultLibrarySPAccountMapper();
        final String spEntityID = SPSSOFederate.getSPEntityId(metaAlias);
        final IDPSSODescriptorType idpsso = SPSSOFederate.getIDPSSOForAuthnReq(realm, entityName);
        final SPSSODescriptorType spsso = SPSSOFederate.getSPSSOForAuthnReq(realm, spEntityID);

        nameIDFormat = SAML2Utils.verifyNameIDFormat(nameIDFormat, spsso, idpsso);
        isTransient = SAML2Constants.NAMEID_TRANSIENT_FORMAT.equals(nameIDFormat);

        Object session = null;
        try {
            session = getLoginState("shouldPersistNameID").getSSOToken();
        } catch (SSOException | AuthLoginException ssoe) {
            if (DEBUG.isDebugEnabled()) {
                DEBUG.debug("SAML2 :: failed to get user's SSOToken.");
            }
        }
        boolean ignoreProfile = SAML2PluginsUtils.isIgnoredProfile(session, realm);

        return !isTransient && !ignoreProfile
                && spAccountMapper.shouldPersistNameIDFormat(realm, spEntityId, entityName, nameIDFormat);
    }

    /**
     * Writes out an error debug (if a throwable and debug message are provided) and returns a user-facing
     * error page.
     */
    private int processError(String headerMessage, String debugMessage,
                             Object... messageParameters) throws AuthLoginException {
        if (null != debugMessage) {
            DEBUG.error(debugMessage, messageParameters);
        }
        substituteHeader(STATE_ERROR, headerMessage);
        return STATE_ERROR;
    }

    /**
     * Writes out an error debug (if a throwable and debug message are provided) and returns a user-facing
     * error page.
     */
    private int processError(L10NMessageImpl e, String headerMessageCode,
                             String debugMessage, Object... messageParameters) throws AuthLoginException {

        if (null == e) {
            return processError(headerMessageCode, debugMessage, messageParameters);
        }
        String headerMessage;
        if (null == headerMessageCode) {
            headerMessage = e.getL10NMessage(getLoginLocale());
        } else {
            headerMessage = bundle.getString(headerMessageCode);
        }
        if (debugMessage != null) {
            DEBUG.error(debugMessage, messageParameters, e);
        }
        substituteHeader(STATE_ERROR, headerMessage);
        return STATE_ERROR;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }
}
