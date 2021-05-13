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
 * Copyright 2015-2019 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.saml2;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.openam.utils.Time.newDate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.am.saml2.impl.Saml2ResponseData;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.utils.StringUtils;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.EncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.impl.NameIDImplWithoutSPNameQualifier;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.SAML2ServiceProviderAdapter;
import com.sun.identity.saml2.plugins.SPAdapterHttpServletResponseWrapper;
import com.sun.identity.saml2.profile.CacheObject;
import com.sun.identity.saml2.profile.IDPProxyUtil;
import com.sun.identity.saml2.profile.LogoutUtil;
import com.sun.identity.saml2.profile.ResponseInfo;
import com.sun.identity.saml2.profile.SPACSUtils;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.DNMapper;

/**
 * Plugin that gets activated for SLO for the SAML2 auth module. Supports HTTP-Redirect
 * for logout-sending messages only.
 */
public class SAML2PostAuthenticationPlugin implements AMPostAuthProcessInterface {

    private static final Logger DEBUG = LoggerFactory.getLogger(SAML2PostAuthenticationPlugin.class);
    private static final SAML2MetaManager META_MANAGER = SAML2Utils.getSAML2MetaManager();

    /**
     * Default Constructor.
     */
    public SAML2PostAuthenticationPlugin() {

    }

    /**
     * If enabled, performs the first-stage of SLO - by recording the currently logged in user.
     * The information relating to a remote user is stored alongside their local information, and upon
     * active-logout is used to trigger a call to the IdP requesting their logout.
     *
     * @param requestParamsMap map containing <code>HttpServletRequest</code>
     *        parameters
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     * @param ssoToken authenticated user's single sign token.
     */
    @Override
    public void onLoginSuccess(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response,
                               SSOToken ssoToken) {

        try {
            final String spEntityId = ssoToken.getProperty(SAML2Constants.SPENTITYID);
            final SessionProvider sessionProvider = SessionManager.getProvider();
            final String cacheKey = ssoToken.getProperty(Constants.CACHE_KEY);
            final String realm =
                    DNMapper.orgNameToRealmName(ssoToken.getProperty(com.sun.identity.shared.Constants.ORGANIZATION));

            Saml2ResponseData data = (Saml2ResponseData) SAML2FailoverUtils.retrieveSAML2Token(cacheKey);

            if (data == null) {
                throw new SAML2Exception("Unable to retrieve response map from data cache.");
            }

            configurePostSSO(spEntityId, realm, request, response, ssoToken, sessionProvider, data, cacheKey);
        } catch (SAML2Exception | SessionException | SSOException | SAML2TokenRepositoryException e) {
            //debug warning and fall through
            DEBUG.warn("Error saving SAML assertion information in memory. SLO not configured for this session.", e);
        }
    }

    private void configurePostSSO(String spEntityId, String realm, HttpServletRequest request,
                                  HttpServletResponse response, SSOToken session, SessionProvider sessionProvider,
                                  Saml2ResponseData responseData, String cacheKey)
            throws SAML2Exception {

        ResponseInfo responseInfo = responseData.getResponseInfo();
        AuthnRequest authnRequest = responseData.getAuthnRequest();
        boolean writeFedInfo = Boolean.parseBoolean((String) SPCache.fedAccountHash.get(cacheKey));

        final SAML2ServiceProviderAdapter spAdapter = SAML2Utils.getSPAdapterClass(spEntityId, realm);
        if (spAdapter != null) {
            final boolean redirected = spAdapter.postSingleSignOnSuccess(spEntityId, realm, request,
                    new SPAdapterHttpServletResponseWrapper(response), null, session, authnRequest,
                    responseInfo.getResponse(), responseInfo.getProfileBinding(), writeFedInfo);
            final String[] value = new String[] { String.valueOf(redirected) };
            try {
                sessionProvider.setProperty(session, SAML2Constants.RESPONSE_REDIRECTED, value);
            } catch (SessionException | UnsupportedOperationException ex) {
                DEBUG.warn("SAML2PostAuthenticationPlugin.configurePostSSO :: failed to set properties in session.",
                        ex);
            }
        }

        SPCache.fedAccountHash.remove(cacheKey);
    }

    private void configureIdpInitSLO(SessionProvider sessionProvider, SSOToken session, String sessionIndex,
                                     String metaAlias, NameIDInfo info, boolean isTransient, String requestID)
            throws SAML2Exception {
        SPACSUtils.saveInfoInMemory(sessionProvider, session, sessionIndex, metaAlias,
                info, IDPProxyUtil.isIDPProxyEnabled(requestID), isTransient);
    }

    private String setupSingleLogOut(SSOToken ssoToken, String metaAlias, String sessionIndex, String spEntityId,
        String idpEntityId, NameID nameId, HttpServletRequest servletRequest)
            throws SSOException, SAML2Exception, SessionException {
        final SAML2MetaManager sm = new SAML2MetaManager();
        final String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        final String relayState = ssoToken.getProperty(SAML2Constants.RELAY_STATE);
        final String binding = SAML2Constants.HTTP_REDIRECT;
        final IDPSSODescriptorType idpsso = sm.getIDPSSODescriptor(realm, idpEntityId);

        final List<EndpointType> slosList = idpsso.getSingleLogoutService();

        EndpointType logoutEndpoint = null;
        for (EndpointType endpoint : slosList) {
            if (binding.equals(endpoint.getBinding())) {
                logoutEndpoint = endpoint;
                break;
            }
        }

        if (logoutEndpoint == null) {
            DEBUG.warn("Unable to determine SLO endpoint. Aborting SLO attempt. Please note this PAP "
                + "only supports HTTP-Redirect as a valid binding.");
            return null;
        }

        final LogoutRequest logoutReq = createLogoutRequest(metaAlias, realm, idpEntityId,
                logoutEndpoint, nameId, sessionIndex);

        final String sloRequestXMLString = logoutReq.toXMLString(true, true);
        String sloRelayState = relayState;
        // If the end user has included a goto param on the logout url, then the logout processing will either
        // have set this as a request attribute or in the session
        String gotoParam = (String) servletRequest.getAttribute(ISAuthConstants.GOTO_PARAM);
        if (StringUtils.isEmpty(gotoParam)) {
            gotoParam = ssoToken.getProperty(ISAuthConstants.GOTO_PARAM);
        }
        if (StringUtils.isNotEmpty(gotoParam)) {
            sloRelayState = gotoParam;
        }
        final String redirect = getRedirectURL(sloRequestXMLString, sloRelayState, realm, idpEntityId,
                logoutEndpoint.getLocation(), spEntityId);

        try {
            // Cache survival time is 10 mins
            final long sessionExpireTime = currentTimeMillis() / 1000 + SPCache.interval; //counted in seconds
            SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(logoutReq.getID(), logoutReq, sessionExpireTime);
        } catch (SAML2TokenRepositoryException e) {
            DEBUG.warn("Unable to set SLO redirect location. Aborting SLO attempt.");
            return null;
        }

        final StringBuilder logoutLocation = new StringBuilder();
        if (StringUtils.isNotEmpty(logoutEndpoint.getLocation())) {
            logoutLocation.append(logoutEndpoint.getLocation());
        }
        if (StringUtils.isNotEmpty(redirect)) {
            try {
                logoutLocation.append(ESAPI.encoder().encodeForURL(redirect));
            } catch (EncodingException e) {
                DEBUG.warn("Failed to encode redirect url for SLO, using unencoded instead.", e);
                logoutLocation.append(redirect);
            }
        }
        if (StringUtils.isEmpty(logoutLocation.toString())) {
            // IdP SLO not possible, but can still redirect to SAML Authn module Single Logout URL, if set
            DEBUG.error("SAML2 PAP :: Unable to perform single logout, enabled but no IdP SLO endpoint set");
            String singleLogoutURL = ssoToken.getProperty(SAML2Constants.SINGLE_LOGOUT_URL);
            if (StringUtils.isNotEmpty(singleLogoutURL)) {
                logoutLocation.append(singleLogoutURL);
            }
        }
        return logoutLocation.toString();
    }

    /**
     * Clears the session of all the temp data we passed to set up SLO.
     */
    private void clearSession(SSOToken ssoToken) throws SSOException {
        ssoToken.setProperty(SAML2Constants.RELAY_STATE, "");
        ssoToken.setProperty(SAML2Constants.SESSION_INDEX, "");
        ssoToken.setProperty(SAML2Constants.IDPENTITYID, "");
        ssoToken.setProperty(SAML2Constants.SPENTITYID, "");
        ssoToken.setProperty(SAML2Constants.METAALIAS, "");
        ssoToken.setProperty(SAML2Constants.REQ_BINDING, "");
        ssoToken.setProperty(SAML2Constants.NAMEID, "");
        ssoToken.setProperty(Constants.IS_TRANSIENT, "");
        ssoToken.setProperty(Constants.REQUEST_ID, "");
        ssoToken.setProperty(Constants.CACHE_KEY, "");
    }

    @Override
    public void onLoginFailure(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response) {
        // This section intentionally left blank.
    }

    @Override
    public void onLogout(HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) {
        try {
            final String ssOutEnabled = ssoToken.getProperty(SAML2Constants.SINGLE_LOGOUT);
            final String metaAlias = ssoToken.getProperty(SAML2Constants.METAALIAS);
            final String sessionIndex = ssoToken.getProperty(SAML2Constants.SESSION_INDEX);
            final String spEntityId = ssoToken.getProperty(SAML2Constants.SPENTITYID);
            final String idpEntityId = ssoToken.getProperty(SAML2Constants.IDPENTITYID);
            final boolean isTransient = Boolean.parseBoolean(ssoToken.getProperty(Constants.IS_TRANSIENT));
            final String requestId = ssoToken.getProperty(Constants.REQUEST_ID);
            final String nameIdXML = ssoToken.getProperty(SAML2Constants.NAMEID);
            final NameID nameId = new NameIDImplWithoutSPNameQualifier(nameIdXML);
            final NameIDInfo info = new NameIDInfo(spEntityId, idpEntityId, nameId, SP_ROLE, false);
            final SessionProvider sessionProvider = SessionManager.getProvider();

            if (Boolean.parseBoolean(ssOutEnabled)) {
                String sloURL = setupSingleLogOut(ssoToken, metaAlias, sessionIndex, spEntityId, idpEntityId,
                    nameId, request);
                if (StringUtils.isNotEmpty(sloURL)) {
                    request.setAttribute(AMPostAuthProcessInterface.POST_PROCESS_LOGOUT_URL, sloURL);
                }
            }
            configureIdpInitSLO(sessionProvider, ssoToken, sessionIndex, metaAlias, info, isTransient, requestId);
            clearSession(ssoToken);
        } catch (SSOException | SessionException | SAML2Exception e) {
            //debug warning and fall through
            DEBUG.warn("Error loading SAML assertion information in memory. SLO failed for this session.", e);
        }
    }

    private LogoutRequest createLogoutRequest(String metaAlias, String realm, String idpEntityId,
                                              EndpointType logoutEndpoint, NameID nameId, String sessionIndex)
            throws SAML2Exception, SessionException {

        // generate unique request ID
        final String requestID = SAML2Utils.generateID();
        if ((requestID == null) || (requestID.length() == 0)) {
            DEBUG.warn("SAML2 PAP :: Unable to perform single logout, unable to generate request ID - {}",
                    SAML2Utils.bundle.getString("cannotGenerateID"));
            throw new SAML2Exception(SAML2Utils.BUNDLE_NAME, "cannotGenerateID", new Object[0]);
        }

        final String spEntityID = META_MANAGER.getEntityByMetaAlias(metaAlias);
        final Issuer issuer = SAML2Utils.createIssuer(spEntityID);

        final LogoutRequest logoutReq = ProtocolFactory.getInstance().createLogoutRequest();
        logoutReq.setID(requestID);
        logoutReq.setVersion(SAML2Constants.VERSION_2_0);
        logoutReq.setIssueInstant(newDate());
        logoutReq.setIssuer(issuer);

        if (sessionIndex != null) {
            logoutReq.setSessionIndex(Collections.singletonList(sessionIndex));
        }

        String location = logoutEndpoint.getLocation();
        logoutReq.setDestination(XMLUtils.escapeSpecialCharacters(location));

        LogoutUtil.setNameIDForSLORequest(logoutReq, nameId, realm, spEntityID, SP_ROLE, idpEntityId);

        return logoutReq;
    }

    private String getRedirectURL(String sloRequestXMLString, String relayState, String realm,
                               String idpEntityId, String sloURL, String hostEntity) throws SAML2Exception {

        // encode the xml string
        String encodedXML = SAML2Utils.encodeForRedirect(sloRequestXMLString);

        StringBuilder queryString = new StringBuilder()
                .append(SAML2Constants.SAML_REQUEST)
                .append(SAML2Constants.EQUAL)
                .append(encodedXML);

        if ((relayState != null) && (relayState.length() > 0)) {
            String tmp = SAML2Utils.generateID();
            SPCache.relayStateHash.put(tmp, new CacheObject(relayState));
            queryString.append("&").append(SAML2Constants.RELAY_STATE).append("=").append(tmp);
        }

        boolean needToSign = SAML2Utils.getWantLogoutRequestSigned(realm, idpEntityId, IDP_ROLE);

        String signedQueryString = queryString.toString();
        if (needToSign) {
            signedQueryString = SAML2Utils.signQueryString(signedQueryString, realm, hostEntity,
                    SP_ROLE, idpEntityId, IDP_ROLE);
        }

        return (sloURL.contains("?") ? "&" : "?") + signedQueryString;
    }
}
