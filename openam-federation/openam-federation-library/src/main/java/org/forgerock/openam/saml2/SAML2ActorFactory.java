/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.saml2;

import com.sun.identity.saml2.common.SAML2Utils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * The SAML2ActorFactory provides creation services for SAML IDP actors.
 */
public class SAML2ActorFactory {

    /**
     * Gets an IDPRequestValidator
     *
     * @param reqBinding the request binding.
     * @param isFromECP true indicates that the request came from an ECP.
     * @return an IDPRequestValidator for performing teh validation request.
     */
    public IDPRequestValidator getIDPRequestValidator(final String reqBinding, final boolean isFromECP) {
        return new UtilProxyIDPRequestValidator(
                reqBinding, isFromECP, SAML2Utils.debug, SAML2Utils.getSAML2MetaManager());
    }

    /**
     * Gets a SAMLAuthenticator object.
     *
     * @param reqData the details of the federation request.
     * @param request the Http request object.
     * @param response the http response object.
     * @param out the output.
     * @param isFromECP true indicates that the request came from an ECP.
     * @return a SAMLAuthenticator object.
     */
    public SAMLAuthenticator getSAMLAuthenticator(final IDPSSOFederateRequest reqData,
                                                  final HttpServletRequest request,
                                                  final HttpServletResponse response,
                                                  final PrintWriter out,
                                                  final boolean isFromECP) {
        return new UtilProxySAMLAuthenticator(reqData, request, response, out, isFromECP);
    }

    /**
     * Get a SAMLAuthenticatorLookup object.
     *
     * @param reqData the details of the federation request.
     * @param request the Http request object.
     * @param response the http response object.
     * @param out the output.
     * @return a SAMLAuthenticatorLookup object.
     */
    public SAMLAuthenticatorLookup getSAMLAuthenticatorLookup(final IDPSSOFederateRequest reqData,
                                                              final HttpServletRequest request,
                                                              final HttpServletResponse response,
                                                              final PrintWriter out) {
        return new UtilProxySAMLAuthenticatorLookup(reqData, request, response, out);
    }
}
