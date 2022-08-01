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
 * Copyright 2015-2022 ForgeRock AS.
 */
package org.forgerock.openam.saml2;

import static com.sun.identity.saml2.common.SAML2Constants.HTTP_POST;
import static com.sun.identity.saml2.common.SAML2Constants.HTTP_REDIRECT;
import static com.sun.identity.saml2.common.SAML2Constants.SAML2_REQUEST_JWT_TYPE;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.forgerock.json.JsonValue.fieldIfNotNull;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;

import com.sun.identity.saml2.profile.CacheObject;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.jwt.JwtEncryptionHandler;
import org.forgerock.openam.jwt.JwtEncryptionOptions;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.keys.DataEncryptionKey;
import org.forgerock.util.annotations.VisibleForTesting;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml2.common.QuerySignatureUtil;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.plugins.IDPAuthnContextInfo;
import com.sun.identity.saml2.plugins.IDPECPSessionMapper;
import com.sun.identity.saml2.profile.ClientFaultException;
import com.sun.identity.saml2.profile.FederatedSSOException;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.IDPProxyUtil;
import com.sun.identity.saml2.profile.IDPSSOUtil;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.profile.SPSSOFederate;
import com.sun.identity.saml2.profile.ServerFaultException;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * An implementation of A SAMLAuthenticator that uses the Util classes to make the federation connection.
 */
public class UtilProxySAMLAuthenticator extends SAMLBase implements SAMLAuthenticator {

    @VisibleForTesting
    static final Purpose<DataEncryptionKey> SAML_2_LOCAL_STORAGE_JWT_ENCRYPTION = Purpose
            .purpose(Labels.SAML2_CLIENT_STORAGE_JWT_ENCRYPTION, DataEncryptionKey.class);
    private final PrintWriter out;
    private final boolean isFromECP;

    /**
     * Creates a new UtilProxySAMLAuthenticator using the detail provided.
     *
     * @param data the request containing the details of the Federate request.
     * @param request the Http request object.
     * @param response the Http response object.
     * @param out the print out.
     * @param isFromECP true if this request was made by an ECP.
     * @param localStorageJwtEncryptionOptions options for encryption/decryption of auth request JWTs that are stored
     * in local storage on the client browser.
     */
    public UtilProxySAMLAuthenticator(IDPSSOFederateRequest data, HttpServletRequest request,
            HttpServletResponse response, PrintWriter out, boolean isFromECP,
            JwtEncryptionOptions localStorageJwtEncryptionOptions) {
        super(request, response, data, localStorageJwtEncryptionOptions);
        this.out = out;
        this.isFromECP = isFromECP;
    }

    @Override
    public void authenticate() throws FederatedSSOException, IOException {

        final String classMethod = "UtilProxySAMLAuthenticator.authenticate: ";

        SPSSODescriptorType spSSODescriptor = null;
        String preferredIDP;

        // There is no reqID, this is the first time that we pass here.
        String binding = SAML2Constants.HTTP_REDIRECT;
        if (request.getMethod().equals("POST")) {
            binding = HTTP_POST;
        }

        AuthnRequest authnRequest = getAuthnRequest(request, isFromECP, binding);
        if (authnRequest == null) {
            throw new ClientFaultException(data.getIdpAdapter(), INVALID_SAML_REQUEST);
        }
        data.setAuthnRequest(authnRequest);
        data.getEventAuditor().setRequestId(data.getRequestID());

        data.setSpEntityID(authnRequest.getIssuer().getValue());

        try {
            logAccess(isFromECP ? LogUtil.RECEIVED_AUTHN_REQUEST_ECP : LogUtil.RECEIVED_AUTHN_REQUEST, Level.INFO,
                    data.getSpEntityID(), data.getIdpMetaAlias(), authnRequest.toXMLString());
        } catch (SAML2Exception saml2ex) {
            SAML2Utils.debug.error("Unable to serialize the authentication request", saml2ex);
            throw new ClientFaultException(data.getIdpAdapter(), INVALID_SAML_REQUEST, saml2ex.getMessage());
        }

        if (!SAML2Utils.isSourceSiteValid(authnRequest.getIssuer(), data.getRealm(), data.getIdpEntityID())) {
            SAML2Utils.debug.warning("{} Issuer in Request is not valid.", classMethod);
            throw new ClientFaultException(data.getIdpAdapter(), INVALID_SAML_REQUEST);
        }

        // verify the signature of the query string if applicable
        IDPSSODescriptorType idpSSODescriptor;
        try {
            idpSSODescriptor = IDPSSOUtil.metaManager.getIDPSSODescriptor(data.getRealm(), data.getIdpEntityID());
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod + "Unable to get IDP SSO Descriptor from meta.");
            throw new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
        }

        // need to verify the query string containing authnRequest
        if (StringUtils.isBlank(data.getSpEntityID())) {
            throw new ClientFaultException(data.getIdpAdapter(), INVALID_SAML_REQUEST);
        }

        try {
            spSSODescriptor = IDPSSOUtil.metaManager.getSPSSODescriptor(data.getRealm(), data.getSpEntityID());
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod + "Unable to get SP SSO Descriptor from meta.");
            SAML2Utils.debug.error(classMethod, sme);
        }

        if (spSSODescriptor == null) {
            SAML2Utils.debug.error("Unable to get SP SSO Descriptor from meta.");
            throw new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
        }

        //only verify signature based on whether this setting is enabled
        if (idpSSODescriptor.isWantAuthnRequestsSigned() || spSSODescriptor.isAuthnRequestsSigned()) {
            if ((HTTP_POST.equals(binding) && authnRequest.isSigned())
                || (HTTP_REDIRECT.equals(binding) && isNotEmpty(request.getParameter(SAML2Constants.SIGNATURE)))) {
            Set<X509Certificate> certificates = KeyUtil.getVerificationCerts(spSSODescriptor, data.getSpEntityID(),
                    SAML2Constants.SP_ROLE, data.getRealm());
            try {
                boolean isSignatureOK;
                if (isFromECP) {
                    isSignatureOK = authnRequest.isSignatureValid(certificates);
                } else {
                    if ("POST".equals(request.getMethod())) {
                        isSignatureOK = authnRequest.isSignatureValid(certificates);
                    } else {
                        isSignatureOK = QuerySignatureUtil.verify(request.getQueryString(), certificates);
                    }
                }
                if (!isSignatureOK) {
                    SAML2Utils.debug.error(classMethod + "authn request verification failed.");
                    throw new ClientFaultException(data.getIdpAdapter(), "invalidSignInRequest");
                }

                // In ECP profile, sp doesn't know idp.
                if (!isFromECP) {
                    // verify Destination
                    List<EndpointType> ssoServiceList = idpSSODescriptor.getSingleSignOnService();
                    EndpointType endPoint = SPSSOFederate.getSingleSignOnServiceEndpoint(ssoServiceList, binding);
                    if (endPoint == null || StringUtils.isEmpty(endPoint.getLocation())) {
                        SAML2Utils.debug
                                .error("{} authn request unable to get endpoint location for IdpEntity: {}  MetaAlias: {} ",
                                        classMethod, data.getIdpEntityID(), data.getIdpMetaAlias());
                        throw new ClientFaultException(data.getIdpAdapter(), "invalidDestination");
                    }
                    if (!SAML2Utils.verifyDestination(authnRequest.getDestination(), endPoint.getLocation())) {
                        SAML2Utils.debug.error("authn request destination verification failed for IdpEntity: {}  MetaAlias: {} "
                                        + "Destination: {}  Location: {}", data.getIdpEntityID(),
                                data.getIdpMetaAlias(), authnRequest.getDestination(), endPoint.getLocation());
                        throw new ClientFaultException(data.getIdpAdapter(), "invalidDestination");
                    }
                }
            } catch (SAML2Exception se) {
                SAML2Utils.debug.error(classMethod + "authn request verification failed.", se);
                throw new ClientFaultException(data.getIdpAdapter(), "invalidSignInRequest");
            }

                SAML2Utils.debug.message("authn request signature verification is successful.");
            } else {
                SAML2Utils.debug.error("The SAML authentication request issued by {} was not signed.", data.getSpEntityID());
                throw new ClientFaultException(data.getIdpAdapter(), "invalidSignInRequest");
            }
        } else {
            SAML2Utils.debug.message("SAML signature verification disabled");
        }

        SAML2Utils.debug.message("SAML Authentication Request id= {}", data.getRequestID());

        if (data.getRequestID() == null) {
            SAML2Utils.debug.error(classMethod + "Request id is null");
            throw new ClientFaultException(data.getIdpAdapter(), "InvalidSAMLRequestID");
        }

        if (isFromECP) {
            try {
                IDPECPSessionMapper idpECPSessonMapper =
                        IDPSSOUtil.getIDPECPSessionMapper(data.getRealm(), data.getIdpEntityID());
                data.setSession(idpECPSessonMapper.getSession(request, response));
            } catch (SAML2Exception se) {
                SAML2Utils.debug.message("Unable to retrieve user session.", classMethod);
            }
        } else {
            // get the user sso session from the request
            try {
                data.setSession(SessionManager.getProvider().getSession(request));
            } catch (SessionException se) {
                SAML2Utils.debug.message("{} Unable to retrieve user session.", classMethod);
            }
        }
        if (null != data.getSession()) {
            data.getEventAuditor().setAuthTokenId(data.getSession());
        }

        // preSingleSignOn adapter hook
        // NB: This method is not called in IDPSSOUtil.doSSOFederate(...) so proxy requests or idp init sso
        // will not trigger this adapter call
        if (preSingleSignOn(request, response, data)) {
            return;
        }
        // End of adapter invocation

        IDPAuthnContextInfo idpAuthnContextInfo = getIdpAuthnContextInfo();
        if (idpAuthnContextInfo == null) {
            SAML2Utils.debug.message("{} Unable to find valid AuthnContext. Sending error Response.", classMethod);
            try {
                Response res = SAML2Utils.getErrorResponse(authnRequest, SAML2Constants.REQUESTER,
                        SAML2Constants.NO_AUTHN_CONTEXT, null, data.getIdpEntityID());
                StringBuffer returnedBinding = new StringBuffer();
                String acsURL = IDPSSOUtil.getACSurl(data.getSpEntityID(), data.getRealm(), authnRequest, request,
                        returnedBinding);
                String acsBinding = returnedBinding.toString();
                IDPSSOUtil.sendResponse(request, response, out, acsBinding, data.getSpEntityID(), data.getIdpEntityID(),
                        data.getIdpMetaAlias(), data.getRealm(), data.getRelayState(), acsURL, res, data.getSession());
            } catch (SAML2Exception sme) {
                SAML2Utils.debug.error(classMethod, sme);
                throw new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
            }
            return;
        }

        // get the relay state query parameter from the request
        data.setRelayState(request.getParameter(SAML2Constants.RELAY_STATE));
        data.setMatchingAuthnContext(idpAuthnContextInfo.getAuthnContext());

        if (data.getSession() == null) {
            // the user has not logged in yet, redirect to auth
            redirectToAuth(spSSODescriptor, binding, idpAuthnContextInfo, data);
        } else {
            SAML2Utils.debug.message("{} There is an existing session", classMethod);

            // Let's verify that the realm is the same for the user and the IdP
            boolean isValidSessionInRealm = IDPSSOUtil.isValidSessionInRealm(data.getRealm(), data.getSession());
            boolean sessionUpgrade = false;
            if (isValidSessionInRealm) {
                sessionUpgrade = isSessionUpgrade(idpAuthnContextInfo, data.getSession());
                SAML2Utils.debug.message("{} IDP Session Upgrade is : {}", classMethod, sessionUpgrade);
            }
            // Holder for any exception encountered while redirecting for authentication:
            FederatedSSOException redirectException = null;
            if (sessionUpgrade || !isValidSessionInRealm ||
                    ((Boolean.TRUE.equals(authnRequest.isForceAuthn())) &&
                            (!Boolean.TRUE.equals(authnRequest.isPassive())))) {

                saveAuthenticationRequestInfoInIdpCache(data);

                //IDP Proxy: Initiate proxying when session upgrade is requested
                // Session upgrade could be requested by asking a greater AuthnContext
                if (isValidSessionInRealm) {
                    try {
                        boolean isProxy = IDPProxyUtil.isIDPProxyEnabled(authnRequest, data.getRealm());
                        if (isProxy) {
                            preferredIDP = IDPProxyUtil.getPreferredIDP(authnRequest, data.getIdpEntityID(),
                                    data.getRealm(), request, response);
                            if (preferredIDP != null) {
                                if ((SPCache.reqParamHash != null)
                                        && (!(SPCache.reqParamHash.containsKey(preferredIDP)))) {
                                    // IDP Proxy with configured proxy list
                                    SAML2Utils.debug.message("{} IDP to be proxied {}", classMethod, preferredIDP);
                                    IDPProxyUtil.sendProxyAuthnRequest(authnRequest, preferredIDP, spSSODescriptor,
                                            data.getIdpEntityID(), request, response, data.getRealm(),
                                            data.getRelayState(), binding);
                                    return;
                                } else {
                                    // IDP proxy with introduction cookie
                                    Map paramsMap = (Map) SPCache.reqParamHash.get(preferredIDP);
                                    paramsMap.put("authnReq", authnRequest);
                                    paramsMap.put("spSSODescriptor", spSSODescriptor);
                                    paramsMap.put("idpEntityID", data.getIdpEntityID());
                                    paramsMap.put("realm", data.getRealm());
                                    paramsMap.put("relayState", data.getRelayState());
                                    paramsMap.put("binding", binding);
                                    SPCache.reqParamHash.put(preferredIDP, paramsMap);
                                    return;
                                }
                            }
                        }
                        //else continue for the local authentication.
                    } catch (SAML2Exception re) {
                        SAML2Utils.debug.message("{} Redirecting for the proxy handling error: {}", classMethod,
                                re.getMessage());
                        redirectException = new ServerFaultException(data.getIdpAdapter(),
                                "UnableToRedirectToPreferredIDP", re.getMessage());
                    }
                    // End of IDP Proxy: Initiate proxying when session upgrade is requested

                }

                // Invoke the IDP Adapter before redirecting to authn
                if (preAuthenticationAdapter(request, response, data)) {
                    return;
                }
                // End of block for IDP Adapter invocation

                //we don't have a session
                try { //and they want to authenticate
                    if (!Boolean.TRUE.equals(authnRequest.isPassive())) {
                        redirectAuthentication(request, response, idpAuthnContextInfo, data, true);
                        return;
                    } else {
                        try { //and they want to get into the system with passive auth - response no passive
                            IDPSSOUtil.sendResponseWithStatus(request, response, out, data.getIdpMetaAlias(),
                                    data.getIdpEntityID(), data.getRealm(), authnRequest,
                                    data.getRelayState(), data.getSpEntityID(), SAML2Constants.RESPONDER,
                                    SAML2Constants.NOPASSIVE);
                        } catch (SAML2Exception sme) {
                            SAML2Utils.debug.error(classMethod, sme);
                            redirectException = new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
                        }
                    }
                } catch (IOException e) {
                    SAML2Utils.debug.error(classMethod + "Unable to redirect to authentication.", e);
                    sessionUpgrade = false;
                    cleanUpCache(data.getRequestID());
                    redirectException = new ServerFaultException(data.getIdpAdapter(), "UnableToRedirectToAuth",
                            e.getMessage());
                }
            }

            // comes here if either no session upgrade or error redirecting to authentication url.
            // generate assertion response
            if (!sessionUpgrade && isValidSessionInRealm) {
                generateAssertionResponse(data);
            }

            if (redirectException != null) {
                throw redirectException;
            }
        }
    }

    private void generateAssertionResponse(IDPSSOFederateRequest data) throws ServerFaultException {

        final String classMethod = "UtilProxySAMLAuthenticator.generateAssertionResponse";

        // IDP Adapter invocation, to be sure that we can execute the logic
        // even if there is a new request with the same session
        saveAuthenticationRequestInfoInIdpCache(data);

        if (preSendResponse(request, response, data)) {
            return;
        }
        // preSendResponse IDP adapter invocation ended

        // call multi-federation protocol to set the protocol
        MultiProtocolUtils.addFederationProtocol(data.getSession(), SingleLogoutManager.SAML2);
        NameIDPolicy policy = data.getAuthnRequest().getNameIDPolicy();
        String nameIDFormat = (policy == null) ? null : policy.getFormat();
        try {
            IDPSSOUtil.sendResponseToACS(request, response, out, data.getSession(), data.getAuthnRequest(),
                    data.getSpEntityID(), data.getIdpEntityID(), data.getIdpMetaAlias(), data.getRealm(),
                    nameIDFormat, data.getRelayState(), data.getMatchingAuthnContext());
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error(classMethod + "Unable to do sso or federation.", se);
            throw new ServerFaultException(data.getIdpAdapter(), SSO_OR_FEDERATION_ERROR, se.getMessage());
        } finally {
            IDPCache.authnRequestCache.remove(data.getRequestID());
            IDPCache.idpAuthnContextCache.remove(data.getRequestID());
            if (StringUtils.isNotBlank(data.getRelayState())) {
                IDPCache.relayStateCache.remove(data.getRequestID());
            }
        }
    }

    private void redirectToAuth(SPSSODescriptorType spSSODescriptor, String binding,
                                IDPAuthnContextInfo idpAuthnContextInfo, IDPSSOFederateRequest data)
            throws IOException, ServerFaultException {

        String classMethod = "UtilProxySAMLAuthenticator.redirectToAuth";
        String preferredIDP;

        // TODO: need to verify the signature of the AuthnRequest

        saveAuthenticationRequestInfoInIdpCache(data);

        //IDP Proxy: Initiate proxying
        try {
            boolean isProxy = IDPProxyUtil.isIDPProxyEnabled(data.getAuthnRequest(), data.getRealm());
            if (isProxy) {
                preferredIDP = IDPProxyUtil.getPreferredIDP(data.getAuthnRequest(), data.getIdpEntityID(),
                        data.getRealm(), request, response);
                if (preferredIDP != null) {
                    if ((SPCache.reqParamHash != null) && (!(SPCache.reqParamHash.containsKey(preferredIDP)))) {
                        // IDP Proxy with configured proxy list
                        SAML2Utils.debug.message("{} IDP to be proxied {} ", classMethod, preferredIDP);
                        IDPProxyUtil.sendProxyAuthnRequest(data.getAuthnRequest(), preferredIDP, spSSODescriptor,
                                data.getIdpEntityID(), request, response, data.getRealm(),
                                data.getRelayState(), binding);
                        return;
                    } else {
                        // IDP proxy with introduction cookie
                        Map paramsMap = (Map) SPCache.reqParamHash.get(preferredIDP);
                        paramsMap.put("authnReq", data.getAuthnRequest());
                        paramsMap.put("spSSODescriptor", spSSODescriptor);
                        paramsMap.put("idpEntityID", data.getIdpEntityID());
                        paramsMap.put("realm", data.getRealm());
                        paramsMap.put("relayState", data.getRelayState());
                        paramsMap.put("binding", binding);
                        SPCache.reqParamHash.put(preferredIDP, paramsMap);
                        return;
                    }
                }
            }
            //else continue for the local authentication.
        } catch (SAML2Exception re) {
            SAML2Utils.debug.message("{} Redirecting for the proxy handling error: {}", classMethod, re.getMessage());
            throw new ServerFaultException(data.getIdpAdapter(), "UnableToRedirectToPreferredIDP", re.getMessage());
        }

        // preAuthentication adapter hook
        if (preAuthenticationAdapter(request, response, data)) {
            return;
        }
        // End of adapter invocation

        // redirect to the authentication service
        try {
            if (!Boolean.TRUE.equals(data.getAuthnRequest().isPassive())) {
                redirectAuthentication(request, response, idpAuthnContextInfo, data, false);
            } else {
                try {
                    IDPSSOUtil.sendResponseWithStatus(request, response, out, data.getIdpMetaAlias(),
                            data.getIdpEntityID(), data.getRealm(), data.getAuthnRequest(), data.getRelayState(),
                            data.getSpEntityID(), SAML2Constants.RESPONDER, SAML2Constants.NOPASSIVE);
                } catch (SAML2Exception sme) {
                    SAML2Utils.debug.error(classMethod, sme);
                    throw new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
                }
            }
        } catch (IOException e) {
            SAML2Utils.debug.error(classMethod + "Unable to redirect to authentication.", e);
            throw new ServerFaultException(data.getIdpAdapter(), "UnableToRedirectToAuth", e.getMessage());
        }

    }

    /**
     * Returns the <code>AuthnRequest</code> from saml request string
     */
    private static AuthnRequest getAuthnRequest(String compressedReq) {

        AuthnRequest authnReq = null;
        String outputString = SAML2Utils.decodeFromRedirect(compressedReq);
        if (outputString != null) {
            try {
                authnReq = ProtocolFactory.getInstance().createAuthnRequest(outputString);
            } catch (SAML2Exception se) {
                SAML2Utils.debug.error("UtilProxySAMLAuthenticator.getAuthnRequest(): cannot construct a AuthnRequest "
                        + "object from the SAMLRequest value:", se);
            }
        }
        return authnReq;
    }

    /**
     * Returns the <code>AuthnRequest</code> from HttpServletRequest
     */
    private static AuthnRequest getAuthnRequest(HttpServletRequest request, boolean isFromECP, String binding) {

        if (isFromECP) {
            try {
                SOAPMessage msg = SOAPCommunicator.getInstance().getSOAPMessage(request);
                Element elem = SOAPCommunicator.getInstance().getSamlpElement(msg, SAML2Constants.AUTHNREQUEST);
                return ProtocolFactory.getInstance().createAuthnRequest(elem);
            } catch (Exception ex) {
                SAML2Utils.debug.error("UtilProxySAMLAuthenticator.getAuthnRequest:", ex);
            }
            return null;
        } else {
            String samlRequest = request.getParameter(SAML2Constants.SAML_REQUEST);
            if (samlRequest == null) {
                SAML2Utils.debug.error("UtilProxySAMLAuthenticator.getAuthnRequest: SAMLRequest is null");
                return null;
            }
            if (binding.equals(SAML2Constants.HTTP_REDIRECT)) {
                SAML2Utils.debug.message("UtilProxySAMLAuthenticator.getAuthnRequest: saml request = {}", samlRequest);
                return getAuthnRequest(samlRequest);
            } else if (binding.equals(HTTP_POST)) {
                ByteArrayInputStream bis = null;
                AuthnRequest authnRequest = null;
                try {
                    byte[] raw = Base64.decode(samlRequest);
                    if (raw != null) {
                        bis = new ByteArrayInputStream(raw);
                        Document doc = XMLUtils.toDOMDocument(bis, SAML2Utils.debug);
                        if (doc != null) {
                            SAML2Utils.debug.message("UtilProxySAMLAuthenticator.getAuthnRequest: decoded SAML2 Authn "
                                    + "Request: {}",
                                    XMLUtils.print(doc.getDocumentElement()));
                            authnRequest = ProtocolFactory.getInstance().createAuthnRequest(doc.getDocumentElement());
                        } else {
                            SAML2Utils.debug.error("UtilProxySAMLAuthenticator.getAuthnRequest: Unable to parse "
                                    + "SAMLRequest: " + samlRequest);
                        }
                    }
                } catch (Exception ex) {
                    SAML2Utils.debug.error("UtilProxySAMLAuthenticator.getAuthnRequest:", ex);
                    return null;
                } finally {
                    IOUtils.closeIfNotNull(bis);
                }
                return authnRequest;
            }
            return null;
        }
    }

    private static StringBuilder getAppliRootUrl(HttpServletRequest request) {
        return new StringBuilder(request.getScheme()).append("://")
                .append(request.getServerName()).append(":")
                .append(request.getServerPort()).append(request.getContextPath());
    }

    private static String getRelativePath(String absUrl, String appliRootUrl) {
        return absUrl.substring(appliRootUrl.length(), absUrl.length());
    }

    /**
     * Redirect to authenticate service
     * If authentication service and federation code are
     * is the same j2ee container do a forward instead of
     * a redirection
     */
    private void redirectAuthentication(HttpServletRequest request, HttpServletResponse response,
            IDPAuthnContextInfo info, IDPSSOFederateRequest data, boolean isSessionUpgrade)
            throws IOException, ServerFaultException {
        // get the authentication service url
        String authService = IDPSSOUtil.getAuthenticationServiceURL(data.getRealm(), data.getIdpEntityID(), request);
        StringBuilder appliRootUrl = getAppliRootUrl(request);
        StringBuilder loginUrl = new StringBuilder(authService);

        // Pass spEntityID to IdP Auth Module
        if (data.getSpEntityID() != null) {
            if (loginUrl.indexOf("?") == -1) {
                loginUrl.append("?");
            } else {
                loginUrl.append("&");
            }

            loginUrl.append(SAML2Constants.SPENTITYID)
                  .append("=")
                  .append(urlEncodeQueryParameterNameOrValue(data.getSpEntityID()));
        }

        Set<String> authnTypeAndValues = info.getAuthnTypeAndValues();
        if (CollectionUtils.isNotEmpty(authnTypeAndValues)) {
            boolean isFirst = true;
            StringBuilder loginParameters = new StringBuilder();

            for (String authnTypeAndValue : authnTypeAndValues) {
                int index = authnTypeAndValue.indexOf("=");
                if (index != -1) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        loginParameters.append("&");
                    }
                    loginParameters.append(authnTypeAndValue.substring(0, index + 1))
                          .append(urlEncodeQueryParameterNameOrValue(authnTypeAndValue.substring(index + 1)));
                }
            }

            if (loginUrl.indexOf("?") == -1) {
                loginUrl.append("?");
            } else {
                loginUrl.append("&");
            }

            loginUrl.append(loginParameters.toString());

            SAML2Utils.debug.message("login URL parameters= {}", loginParameters.toString());
        }

        if (loginUrl.indexOf("?") == -1) {
            if (isSessionUpgrade) {
                loginUrl.append("?ForceAuth=true&goto=");
            } else {
                loginUrl.append("?goto=");
            }

        } else {
            if (isSessionUpgrade) {
                loginUrl.append("&ForceAuth=true");
            }
            loginUrl.append("&goto=");
        }

        // compute gotoURL differently in case of forward or in case
        // of redirection, forward needs a relative URI.
        StringBuilder secondVisitUrl;
        String rpUrl = IDPSSOUtil.getAttributeValueFromIDPSSOConfig(data.getRealm(),
                data.getIdpEntityID(), SAML2Constants.RP_URL);
        if (isNotEmpty(rpUrl)) {
            secondVisitUrl = new StringBuilder(rpUrl);
            secondVisitUrl.append(getRelativePath(request.getRequestURI(), request.getContextPath()));
        } else {
            secondVisitUrl = new StringBuilder(request.getRequestURI());
        }

        //adding these extra parameters will ensure that we can send back SAML error response to the SP even when the
        //originally received AuthnRequest gets lost.
        StringBuilder secondVisitUrlNoClientStorage = new StringBuilder(secondVisitUrl.toString());
        secondVisitUrlNoClientStorage.append("?ReqID=").append(data.getAuthnRequest().getID());
        secondVisitUrl.append("?ReqID=").append(urlEncodeQueryParameterNameOrValue(data.getAuthnRequest().getID())).append('&')
                .append(INDEX).append('=').append(data.getAuthnRequest().getAssertionConsumerServiceIndex()).append('&')
                .append(ACS_URL).append('=')
                .append(urlEncodeQueryParameterNameOrValue(data.getAuthnRequest().getAssertionConsumerServiceURL()))
                .append('&')
                .append(SP_ENTITY_ID).append('=')
                .append(urlEncodeQueryParameterNameOrValue(data.getAuthnRequest().getIssuer().getValue())).append('&')
                .append(BINDING).append('=')
                .append(urlEncodeQueryParameterNameOrValue(data.getAuthnRequest().getProtocolBinding()));

        StringBuilder saml2ContinueUrl = new StringBuilder(appliRootUrl)
                .append("/saml2/continue/metaAlias")
                .append(data.getIdpMetaAlias())
                .append("?secondVisitUrl=")
                .append(urlEncodeQueryParameterNameOrValue(secondVisitUrlNoClientStorage.toString()));

        loginUrl.append(urlEncodeQueryParameterNameOrValue(saml2ContinueUrl.toString()));

        SAML2Utils.debug.message("New URL for authentication: {}", loginUrl.toString());

        loginUrl.append('&').append(SystemPropertiesManager.get(Constants.AM_AUTH_COOKIE_NAME, "AMAuthCookie"));
        loginUrl.append('=');

        SAML2Utils.debug.message("Displaying local storage updating page, then sending the user to {}", loginUrl.toString());
        try {
            request.setAttribute("saml2Request", createSaml2RequestJwt(data));
            request.setAttribute("secondVisitUrl", secondVisitUrl.toString());
            request.setAttribute("loginUrl", loginUrl.toString());
            request.setAttribute("realm", data.getRealm());
            request.setAttribute("idpEntityID", data.getIdpEntityID());
            request.setAttribute("spEntityID", data.getSpEntityID());
            request.getRequestDispatcher("/WEB-INF/saml2/jsp/idpWriteToStorage.jsp").forward(request, response);
        } catch (SAML2Exception | NoSuchSecretException e) {
            SAML2Utils.debug.error("Fail to redirect authentication", e);
            throw new ServerFaultException(data.getIdpAdapter(), INVALID_SAML_REQUEST);
        } catch (ServletException ex) {
            SAML2Utils.debug.error("Exception Bad Forward URL: {}", loginUrl.toString());
        }
    }

    @VisibleForTesting
    String createSaml2RequestJwt(IDPSSOFederateRequest data) throws SAML2Exception, NoSuchSecretException {
        String authnRequest = data.getAuthnRequest().toXMLString(true, true);
        String authnContext = data.getMatchingAuthnContext() == null ? null
                : data.getMatchingAuthnContext().toXMLString(true, true);
        JsonValue requestData = json(object(
                fieldIfNotNull("authnRequest", authnRequest),
                fieldIfNotNull("authnContext", authnContext),
                fieldIfNotNull("relayState", data.getRelayState())
        ));
        JwtClaimsSet claimsSet = new JwtClaimsSet(requestData.asMap());
        claimsSet.setIssuer(data.getIdpEntityID());
        claimsSet.setExpirationTime(Time.newDate(Time.currentTimeMillis()
                + TimeUnit.SECONDS.toMillis(SPCache.interval)));
        claimsSet.setType(SAML2_REQUEST_JWT_TYPE);
        return new JwtEncryptionHandler(localStorageJwtEncryptionOptions)
                .encryptJwt(claimsSet, SAML_2_LOCAL_STORAGE_JWT_ENCRYPTION).build();
    }

    /**
     * Save the authentication request, authentication context and relay state in the IDPCache for subsequent retrieval.
     * Only needed if client side HTML 5 storage is disabled/not available
     *
     * @param data The IDPSSOFederate request that includes the information to be cached.
     */
    private void saveAuthenticationRequestInfoInIdpCache(IDPSSOFederateRequest data) {
        // save the AuthnRequest in the IDPCache so that it can be
        // retrieved later when the user successfully authenticates
        synchronized (IDPCache.authnRequestCache) {
            IDPCache.authnRequestCache.put(data.getRequestID(), new CacheObject(data.getAuthnRequest()));
        }

        // save the AuthnContext in the IDPCache so that it can be
        // retrieved later when the user successfully authenticates
        synchronized (IDPCache.idpAuthnContextCache) {
            IDPCache.idpAuthnContextCache.put(data.getRequestID(), new CacheObject(data.getMatchingAuthnContext()));
        }

        // save the relay state in the IDPCache so that it can be
        // retrieved later when the user successfully authenticates
        if (StringUtils.isNotBlank(data.getRelayState())) {
            IDPCache.relayStateCache.put(data.getRequestID(), data.getRelayState());
        }
    }

    /**
     * clean up the cache created for session upgrade.
     */
    private static void cleanUpCache(String reqID) {
        IDPCache.authnRequestCache.remove(reqID);
        IDPCache.idpAuthnContextCache.remove(reqID);
    }
}
