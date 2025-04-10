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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.saml2;

import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.openam.jwt.JwtEncryptionOptions;
import org.forgerock.openam.secrets.SecretException;
import org.forgerock.openam.secrets.Secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.profile.ServerFaultException;

/**
 * The SAML2ActorFactory provides creation services for SAML IDP actors.
 */
public class SAML2ActorFactory {

    private static final Logger logger = LoggerFactory.getLogger(SAML2ActorFactory.class);

    /**
     * Gets an IDPRequestValidator
     *
     * @param reqBinding the request binding.
     * @param isFromECP true indicates that the request came from an ECP.
     * @return an IDPRequestValidator for performing teh validation request.
     */
    public IDPRequestValidator getIDPRequestValidator(final String reqBinding, final boolean isFromECP) {
        return new UtilProxyIDPRequestValidator(reqBinding, isFromECP, SAML2Utils.getSAML2MetaManager());
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
                                                  final boolean isFromECP) throws ServerFaultException {
        return new UtilProxySAMLAuthenticator(reqData, request, response, out, isFromECP,
                getLocalStorageJwtEncryptionOptions());
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
                                                              final PrintWriter out) throws ServerFaultException {
        return new UtilProxySAMLAuthenticatorLookup(reqData, request, response, out,
                getLocalStorageJwtEncryptionOptions());
    }

    private JwtEncryptionOptions getLocalStorageJwtEncryptionOptions() throws ServerFaultException {
        try {
            return new JwtEncryptionOptions(
                    InjectorHolder.getInstance(Secrets.class).getGlobalSecrets(),
                    JweAlgorithm.DIRECT, EncryptionMethod.A256GCM, CompressionAlgorithm.DEF);
        } catch (SecretException e) {
            logger.error("Error initialising secret store.", e);
            throw new ServerFaultException("secretStoreInitFailure");
        }
    }
}
