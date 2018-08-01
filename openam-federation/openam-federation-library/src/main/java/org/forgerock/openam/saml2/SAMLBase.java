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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.saml2;

import java.util.Map;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.plugins.IDPAuthnContextInfo;
import com.sun.identity.saml2.plugins.IDPAuthnContextMapper;
import com.sun.identity.saml2.profile.IDPSSOUtil;
import com.sun.identity.saml2.profile.ServerFaultException;

/**
 * This is an abstract class for SAML processing that provides utility methods for its subclasses.
 */
public abstract class SAMLBase {

    protected static final String INDEX = "index";
    protected static final String ACS_URL = "acsURL";
    protected static final String SP_ENTITY_ID = "spEntityID";
    protected static final String BINDING = "binding";

    protected static final String INVALID_SAML_REQUEST = "InvalidSAMLRequest";
    protected static final String METADATA_ERROR = "metaDataError";
    protected static final String SSO_OR_FEDERATION_ERROR = "UnableToDOSSOOrFederation";

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;
    protected final IDPSSOFederateRequest data;

    /**
     * Constructor to initialize the state of SAMLBase.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @param data The SAML request data.
     */
    protected SAMLBase(HttpServletRequest request, HttpServletResponse response, IDPSSOFederateRequest data) {
        this.request = request;
        this.response = response;
        this.data = data;
    }

    protected void logAccess(String logId, Level logLevel, String... data) {
        LogUtil.access(logLevel, logId, data);
    }

    protected void logError(Level logLevel, String logId, Object session, Map properties, String... data) {
        LogUtil.error(logLevel, logId, data, session, properties);
    }

    /**
     * Retrieves the authn context mapping info for the currently processed SAML authentication request.
     *
     * @return The authn context mapping info.
     * @throws ServerFaultException If the authn context mapper is not available.
     */
    protected IDPAuthnContextInfo getIdpAuthnContextInfo() throws ServerFaultException {
        String classMethod = "SAMLBase.getIdpAuthnContextInfo: ";
        IDPAuthnContextMapper idpAuthnContextMapper = null;
        try {
            idpAuthnContextMapper = IDPSSOUtil.getIDPAuthnContextMapper(data.getRealm(), data.getIdpEntityID());
        } catch (SAML2Exception sme) {
            SAML2Utils.debug.error(classMethod, sme);
        }
        if (idpAuthnContextMapper == null) {
            SAML2Utils.debug.error("{}Unable to get IDPAuthnContextMapper from meta.", classMethod);
            throw new ServerFaultException(data.getIdpAdapter(), METADATA_ERROR);
        }

        IDPAuthnContextInfo idpAuthnContextInfo = null;
        try {
            idpAuthnContextInfo = idpAuthnContextMapper.getIDPAuthnContextInfo(data.getAuthnRequest(),
                    data.getIdpEntityID(), data.getRealm());
        } catch (SAML2Exception sme) {
            SAML2Utils.debug.error(classMethod, sme);
        }
        return idpAuthnContextInfo;
    }

    /**
     * <p>
     * Iterates through the RequestedAuthnContext from the Service Provider and checks if user has already authenticated
     * with a sufficient authentication level.
     * </p>
     * <p>
     * If RequestAuthnContext is not found in the authenticated AuthnContext then session upgrade will be performed.
     * </p>
     *
     * @return true if the requester needs to re-authenticate.
     */
    protected static boolean isSessionUpgrade(IDPAuthnContextInfo idpAuthnContextInfo, Object session) {
        String classMethod = "SAMLBase.isSessionUpgrade: ";

        if (session != null) {
            // Get the Authentication Context required
            String authnClassRef = idpAuthnContextInfo.getAuthnContext().getAuthnContextClassRef();
            // Get the AuthN level associated with the Authentication Context
            int authnLevel = idpAuthnContextInfo.getAuthnLevel();

            SAML2Utils.debug.message("{}Requested AuthnContext: authnClassRef={} authnLevel={}", classMethod,
                    authnClassRef, authnLevel);

            int sessionAuthnLevel = 0;

            try {
                final String strAuthLevel = SessionManager.getProvider().getProperty(session,
                        SAML2Constants.AUTH_LEVEL)[0];
                if (strAuthLevel.contains(":")) {
                    String[] realmAuthLevel = strAuthLevel.split(":", 2);
                    sessionAuthnLevel = Integer.parseInt(realmAuthLevel[1]);
                } else {
                    sessionAuthnLevel = Integer.parseInt(strAuthLevel);
                }
                SAML2Utils.debug.message("{}Current session Authentication Level: {}", classMethod, sessionAuthnLevel);
            } catch (SessionException se) {
                SAML2Utils.debug.error("{}Couldn't get the session Auth Level", classMethod, se);
            }

            return authnLevel > sessionAuthnLevel;
        } else {
            return true;
        }
    }

    protected boolean preSingleSignOn(HttpServletRequest request, HttpServletResponse response, IDPSSOFederateRequest data) {

        try {
            SAML2Utils.debug.message("Invoking the IDP Adapter preSingleSignOn hook");
            return data.getIdpAdapter().preSingleSignOn(data.getIdpEntityID(), data.getRealm(), request, response,
                    data.getAuthnRequest(), data.getRequestID());
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("Error invoking the IDP Adapter", se);
        }

        return false;
    }

    protected boolean preSendResponse(HttpServletRequest request, HttpServletResponse response, IDPSSOFederateRequest data) {

        try {
            SAML2Utils.debug.message("Invoking the IDP Adapter preSendResponse");
            return data.getIdpAdapter().preSendResponse(data.getAuthnRequest(), data.getIdpEntityID(), data.getRealm(),
                    request, response, data.getSession(), data.getRequestID(), data.getRelayState());
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("Error invoking the IDP Adapter", se);
        }

        return false;
    }

    protected boolean preAuthenticationAdapter(HttpServletRequest request, HttpServletResponse response,
                                               IDPSSOFederateRequest data) {
        try {
            SAML2Utils.debug.message("Invoking the IDP Adapter preAuthentication hook");
            return data.getIdpAdapter().preAuthentication(
                    data.getIdpEntityID(), data.getRealm(), request, response, data.getAuthnRequest(), data.getSession(),
                    data.getRequestID(), data.getRelayState());
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("Error invoking the IDP Adapter", se);
        }

        return false;

    }

}
