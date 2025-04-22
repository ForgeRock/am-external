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
 * Copyright 2015-2024 ForgeRock AS.
 */
package org.forgerock.openam.saml2;

import static com.sun.identity.saml2.common.SAML2Constants.SAML2_REQUEST_JWT_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.saml2.assertion.AuthnContext;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.JwtClaimsSetKey;
import org.forgerock.openam.jwt.JwtClaimsValidationHandler;
import org.forgerock.openam.jwt.JwtClaimsValidationOptions;
import org.forgerock.openam.jwt.JwtDecryptionHandler;
import org.forgerock.openam.jwt.JwtEncryptionOptions;
import org.forgerock.openam.jwt.exceptions.DecryptionFailedException;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.keys.DataDecryptionKey;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.plugins.IDPAuthnContextInfo;
import com.sun.identity.saml2.profile.CacheObject;
import com.sun.identity.saml2.profile.ClientFaultException;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.IDPSSOUtil;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.profile.ServerFaultException;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * An implementation of A SAMLAuthenticatorLookup that uses the Util classes to make the federation connection.
 */
public class UtilProxySAMLAuthenticatorLookup extends SAMLBase implements SAMLAuthenticatorLookup {

    @VisibleForTesting
    static final Purpose<DataDecryptionKey> SAML_2_LOCAL_STORAGE_JWT_DECRYPTION = Purpose
            .purpose(Labels.SAML2_CLIENT_STORAGE_JWT_ENCRYPTION, DataDecryptionKey.class);
    private static final Logger logger = LoggerFactory.getLogger(UtilProxySAMLAuthenticatorLookup.class);
    private final PrintWriter out;
    private final static String SKEW_ALLOWANCE_PARAM =
            "org.forgerock.openam.saml2.authenticatorlookup.skewAllowance";
    private final long SKEW_ALLOWANCE;
    private final int SKEW_ALLOWANCE_DEFAULT = 60;

    /**
     * Creates a new UtilProxySAMLAuthenticatorLookup
     *
     * @param data the details of the federation request.
     * @param request the Http request object.
     * @param response the http response object.
     * @param out the output.
     * @param localStorageJwtEncryptionOptions options for encryption/decryption of auth request JWTs that are stored
     * in local storage on the client browser.
     */
    public UtilProxySAMLAuthenticatorLookup(IDPSSOFederateRequest data, HttpServletRequest request,
            HttpServletResponse response, PrintWriter out, JwtEncryptionOptions localStorageJwtEncryptionOptions) {
        super(request, response, data, localStorageJwtEncryptionOptions);
        this.out = out;
        this.SKEW_ALLOWANCE = TimeUnit.SECONDS.toMillis
                (SystemPropertiesManager.getAsInt((SKEW_ALLOWANCE_PARAM), SKEW_ALLOWANCE_DEFAULT));
    }

    @Override
    public void retrieveAuthenticationFromCache() throws SessionException, ServerFaultException, ClientFaultException {

        final String classMethod = "UtilProxySAMLAuthenticatorLookup.retrieveAuthenticationFromCache: ";

        // the second visit, the user has already authenticated
        // retrieve the cache authn request and relay state

        // We need the session to pass it to the IDP Adapter preSendResponse
        SessionProvider sessionProvider = SessionManager.getProvider();
        try {
            data.setSession(sessionProvider.getSession(request));
            data.getEventAuditor().setSSOTokenId(data.getSession());
        } catch (SessionException se) {
            logger.error("An error occurred while retrieving the session: " + se.getMessage());
            data.setSession(null);
        }

        // Get the cached Authentication Request and Relay State before invoking the IDP Adapter
        CacheObject cacheObj;
        try {
            String saml2Request = request.getParameter("saml2Request");
            if (StringUtils.isNotEmpty(saml2Request)) {
                logger.debug("Retrieving saml2Request from client storage");
                Jwt saml2RequestJwt = decryptLocalStorageJwt(saml2Request);
                JwtClaimsSet claimsSet = saml2RequestJwt.getClaimsSet();
                validateSaml2RequestJwt(data, claimsSet);
                ProtocolFactory protocolFactory = ProtocolFactory.getInstance();
                data.setAuthnRequest(protocolFactory.createAuthnRequest(claimsSet.get("authnRequest").asString()));
                data.setRelayState(claimsSet.get("relayState").asString());
                if (claimsSet.isDefined("authnContext")) {
                    data.setMatchingAuthnContext(AssertionFactory.getInstance().createAuthnContext(
                            claimsSet.get("authnContext").asString()));
                }
            } else {
                logger.debug("Retrieving saml2Request from server side cache");
                cacheObj = (CacheObject) IDPCache.authnRequestCache.get(data.getRequestID());
                if (cacheObj != null) {
                    data.setAuthnRequest((AuthnRequest) cacheObj.getObject());
                }

                synchronized (IDPCache.idpAuthnContextCache) {
                    cacheObj = (CacheObject) IDPCache.idpAuthnContextCache.get(data.getRequestID());
                }
                if (cacheObj != null) {
                    data.setMatchingAuthnContext((AuthnContext) cacheObj.getObject());
                }

                data.setRelayState((String) IDPCache.relayStateCache.get(data.getRequestID()));
            }
        } catch (SAML2Exception e) {
            throw new ClientFaultException(data.getIdpAdapter(), INVALID_SAML_REQUEST);
        } catch (DecryptionFailedException e) {
            throw new ServerFaultException(data.getIdpAdapter(), INVALID_SAML_REQUEST);
        }

        if (!isSessionValid(sessionProvider)) {
            return;
        }

        if (!isForceAuthValid(sessionProvider)) {
            try {
                IDPSSOUtil.redirectAuthentication(request, response, data.getAuthnRequest(), null, data.getRealm(),
                        data.getIdpEntityID(), data.getSpEntityID(), true);
            } catch (IOException | SAML2Exception ex) {
                logger.error(classMethod + "Unable to redirect to authentication.", ex);
                throw new ServerFaultException(data.getIdpAdapter(), "UnableToRedirectToAuth", ex.getMessage());
            }
            return;
        }

        // Invoke the IDP Adapter after the user has been authenticated
        if (preSendResponse(request, response, data)) {
            return;
        }
        // End of block for IDP Adapter invocation

        IDPCache.authnRequestCache.remove(data.getRequestID());
        IDPCache.idpAuthnContextCache.remove(data.getRequestID());
        IDPCache.relayStateCache.remove(data.getRequestID());

        if (data.getAuthnRequest() == null) {
            authNotAvailable();
            return;
        }

        logger.debug("{} RequestID= {}", classMethod, data.getRequestID());

        if (data.getSession() != null) {
            // call multi-federation protocol to set the protocol
            MultiProtocolUtils.addFederationProtocol(data.getSession(), SingleLogoutManager.SAML2);
        }

        // generate assertion response
        data.setSpEntityID(data.getAuthnRequest().getIssuer().getValue());
        NameIDPolicy policy = data.getAuthnRequest().getNameIDPolicy();
        String nameIDFormat = (policy == null) ? null : policy.getFormat();
        try {
            IDPSSOUtil.sendResponseToACS(request, response, out, data.getSession(), data.getAuthnRequest(),
                    data.getSpEntityID(), data.getIdpEntityID(), data.getIdpMetaAlias(), data.getRealm(), nameIDFormat,
                    data.getRelayState(), data.getMatchingAuthnContext());
        } catch (SAML2Exception se) {
            logger.error(classMethod + "Unable to do sso or federation.", se);
            throw new ServerFaultException(data.getIdpAdapter(), SSO_OR_FEDERATION_ERROR, se.getMessage());
        }

    }

    private Jwt decryptLocalStorageJwt(String saml2Request) throws DecryptionFailedException {
        EncryptedJwt encryptedJwt = new JwtReconstruction().reconstructJwt(saml2Request, EncryptedJwt.class);
        return new JwtDecryptionHandler(localStorageJwtEncryptionOptions)
                .decryptJwe(encryptedJwt, SAML_2_LOCAL_STORAGE_JWT_DECRYPTION);
    }

    @VisibleForTesting
    void validateSaml2RequestJwt(IDPSSOFederateRequest data, JwtClaimsSet claimsSet) throws ClientFaultException {
        JwtClaimsValidationOptions<ClientFaultException> validationOptions =
                new JwtClaimsValidationOptions<>((s) ->
                        new ClientFaultException(data.getIdpAdapter(), "Invalid SAML2 request jwt"))
                        .setIssuer(data.getIdpEntityID())
                        .setType(SAML2_REQUEST_JWT_TYPE)
                        .setUnreasonableLifetimeLimit(Duration.duration(SPCache.interval, TimeUnit.SECONDS))
                        .setIssuerRequired(false)
                        .addClaimValidator(JwtClaimsSetKey.ISS.value(), v -> v.isNotNull() && v.isString() &&
                                (data.getIdpEntityID().equals(v.asString()) || data.getIdpEntityID()
                                        .equals(URLDecoder.decode(v.asString(), UTF_8))));
        JwtClaimsValidationHandler<ClientFaultException> claimsValidator =
                new JwtClaimsValidationHandler<>(validationOptions, claimsSet);
        claimsValidator.validateClaims();
    }

    /**
     * Validate that the AuthN Request ForceAuthN setting is honoured
     * @param sessionProvider Session Provider
     * @return true if AuthInstant is before the system current time.
     * @throws SessionException is there is a problem getting the session property
     */
    private boolean isForceAuthValid(SessionProvider sessionProvider) throws SessionException {

        if (data.getAuthnRequest() != null && (Boolean.TRUE.equals(data.getAuthnRequest().isForceAuthn())) &&
                (!Boolean.TRUE.equals(data.getAuthnRequest().isPassive()))) {
            Date authInstant = null;
            try {
                String[] values = sessionProvider.getProperty(
                        data.getSession(), SessionProvider.AUTH_INSTANT);
                if (values != null && values.length != 0 &&
                        values[0] != null && values[0].length() != 0) {
                    authInstant = DateUtils.stringToDate(values[0]);
                }
            } catch (ParseException e) {
                logger.warn("Exception retrieving AuthInstant from the session: ", e);
            }
            if (authInstant == null) {
                logger.error("AuthInstant is null failing ForceAuth check");
                return false;
            }
            Date currentTime = Time.newDate(Time.currentTimeMillis());
            logger.debug("Verifying isForceAuthValid time check AuthInstant {} currentTime {}  " +
                            "SKEW_ALLOWANCE {} ",
                    authInstant, currentTime, SKEW_ALLOWANCE);
            authInstant = new Date(authInstant.getTime() + SKEW_ALLOWANCE);
            if (authInstant.before(currentTime)) {
                logger.debug("AuthnRequest specified ForceAuth but session is older than current system " +
                        "time and skew");
                return false;
            }
        }
        return true;
    }

    private void authNotAvailable() throws ServerFaultException {
        final String classMethod = "UtilProxySAMLAuthenticatorLookup.authNotavailable";

        //handle the case when the authn request is no longer available in the local cache. This could
        //happen for multiple reasons:
        //   - the SAML response has been already sent back for this request (e.g. browser back button)
        //   - the second visit reached a different OpenAM server, than the first and SAML SFO is disabled
        //   - the cache interval has passed
        logger.error(classMethod + "Unable to get AuthnRequest from cache, sending error response");
        try {
            logger.debug("Invoking IDP adapter preSendFailureResponse hook");
            try {
                data.getIdpAdapter().preSendFailureResponse(data.getIdpEntityID(), data.getRealm(), request, response,
                        SAML2Constants.SERVER_FAULT, "UnableToGetAuthnReq");
            } catch (SAML2Exception se2) {
                logger.error("Error invoking the IDP Adapter", se2);
            }
            Response res = SAML2Utils.getErrorResponse(null, SAML2Constants.RESPONDER, null, null,
                    data.getIdpEntityID());
            res.setInResponseTo(data.getRequestID());
            StringBuffer returnedBinding = new StringBuffer();
            String spEntityID = request.getParameter(SP_ENTITY_ID);
            String acsURL = request.getParameter(ACS_URL);
            String binding = request.getParameter(BINDING);
            Integer index;
            try {
                index = Integer.valueOf(request.getParameter(INDEX));
            } catch (NumberFormatException nfe) {
                index = null;
            }
            acsURL = IDPSSOUtil.getACSurl(spEntityID, data.getRealm(), null, acsURL, binding, index, returnedBinding);
            String acsBinding = returnedBinding.toString();
            IDPSSOUtil.sendResponse(request, response, out, acsBinding, spEntityID, data.getIdpEntityID(),
                    data.getIdpMetaAlias(), data.getRealm(), data.getRelayState(), acsURL, res, data.getSession());
        } catch (SAML2Exception sme) {
            logger.error(classMethod + "an error occurred while sending error response", sme);
            throw new ServerFaultException(data.getIdpAdapter(), "UnableToGetAuthnReq");
        }

    }

    @VisibleForTesting
    boolean isSessionValid(SessionProvider sessionProvider) throws ServerFaultException,
            ClientFaultException, SessionException {

        final String classMethod = "UtilProxySAMLAuthenticatorLookup.isSessionValid: ";

        // Let's verify if the session belongs to the proper realm
        boolean isValidSessionInRealm = data.getSession() != null &&
                IDPSSOUtil.isValidSessionInRealm(data.getRealm(), data.getSession());

        // There should be a session on the second pass. If this is not the case then provide an error message
        // If there is a session then it must belong to the proper realm
        if (!isValidSessionInRealm) {
            if (data.getAuthnRequest() != null && Boolean.TRUE.equals(data.getAuthnRequest().isPassive())) {
                // Send an appropriate response to the passive request
                data.setSpEntityID(data.getAuthnRequest().getIssuer().getValue());
                try {
                    IDPSSOUtil.sendResponseWithStatus(request, response, out, data.getIdpMetaAlias(),
                            data.getIdpEntityID(), data.getRealm(), data.getAuthnRequest(), data.getRelayState(),
                            data.getSpEntityID(), SAML2Constants.RESPONDER, SAML2Constants.NOPASSIVE);
                    return false;
                } catch (SAML2Exception sme) {
                    logger.error(classMethod, sme);
                    throw new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
                }
            } else {
                // No attempt to authenticate now, since it is assumed that that has already been tried
                String ipAddress = request.getRemoteAddr();
                String authnReqString = "";
                try {
                    authnReqString = data.getAuthnRequest() == null ? "" : data.getAuthnRequest().toXMLString();
                } catch (SAML2Exception ex) {
                    logger.error(classMethod + "Could not obtain the AuthnReq to be logged");
                }

                if (data.getSession() == null) {
                    logger.error(classMethod + "The IdP has not been able to create a session");
                    logError(Level.INFO, LogUtil.SSO_NOT_FOUND, null, null, "null", data.getRealm(),
                            data.getIdpEntityID(), ipAddress, authnReqString);
                } else {
                    logger.error(classMethod + "The realm of the session does not correspond to that " +
                            "of the IdP");
                    logError(Level.INFO, LogUtil.INVALID_REALM_FOR_SESSION, data.getSession(), null,
                            sessionProvider.getProperty(data.getSession(), SAML2Constants.ORGANIZATION)[0],
                            data.getRealm(), data.getIdpEntityID(), ipAddress, authnReqString);
                }

                throw new ClientFaultException(data.getIdpAdapter(), SSO_OR_FEDERATION_ERROR);
            }
        }

        IDPAuthnContextInfo idpAuthnContextInfo = getIdpAuthnContextInfo();
        if (isSessionUpgrade(idpAuthnContextInfo, data.getSession())) {
            logger.debug("{}The session upgrade requested by the SP was not completed by the end user {}",
                    classMethod, sessionProvider.getPrincipalName(data.getSession()));
            throw new ClientFaultException(data.getIdpAdapter(), SSO_OR_FEDERATION_ERROR);
        }

        return true;
    }
}
