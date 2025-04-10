/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: FedletAdapter.java,v 1.2 2009/06/17 03:09:13 exu Exp $
 *
 * Portions Copyrighted 2022-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.saml2.plugins;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.annotations.EvolvingAll;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;

/**
 * The <code>FedletAdapterPlugin</code> abstract class provides methods
 * that could be extended to perform user specific logics during SAMLv2
 * protocol processing on the Service Provider side. The implementation class
 * could be configured on a per service provider basis in the extended
 * metadata configuration.
 * <p>
 * A singleton instance of this <code>FedletAdapterPlugin</code>
 * class will be used per Service Provider during runtime, so make sure
 * implementation of the methods are thread safe.
 */
@EvolvingAll
public interface FedletAdapter extends InitializablePlugin {


    /**
     * Invokes after Fedlet receives SLO request from IDP. It does the work
     * of logout the user.
     * @param request servlet request
     * @param response servlet response
     * @param hostedEntityID entity ID for the fedlet
     * @param idpEntityID entity id for the IDP to which the request is
     *          received from.
     * @param siList List of SessionIndex whose session to be logged out
     * @param nameIDValue nameID value whose session to be logged out
     * @param binding Single Logout binding used,
     *      one of following values:
     *          <code>SAML2Constants.SOAP</code>,
     *          <code>SAML2Constants.HTTP_POST</code>,
     *          <code>SAML2Constants.HTTP_REDIRECT</code>
     * @return <code>true</code> if user is logged out successfully;
     *          <code>false</code> otherwise.
     * @exception SAML2Exception if user want to fail the process.
     */
    default boolean doFedletSLO(HttpServletRequest request, HttpServletResponse response, LogoutRequest logoutReq,
            String hostedEntityID, String idpEntityID, List siList, String nameIDValue, String binding)
            throws SAML2Exception {
        return true;
    }

    /**
     * Invokes after Fedlet receives SLO response from IDP and the SLO status
     * is success.
     * @param request servlet request
     * @param response servlet response
     * @param logoutReq SAML2 <code>LogoutRequest</code> object
     * @param logoutRes SAML2 <code>LogoutResponse</code> object
     * @param hostedEntityID entity ID for the fedlet
     * @param idpEntityID entity id for the IDP to which the logout response
     *          is received from.
     * @param binding Single Logout binding used,
     *      one of following values:
     *          <code>SAML2Constants.SOAP</code>,
     *          <code>SAML2Constants.HTTP_POST</code>,
     *          <code>SAML2Constants.HTTP_REDIRECT</code>
     * @exception SAML2Exception if user want to fail the process.
     */
    default void onFedletSLOSuccess(HttpServletRequest request, HttpServletResponse response, LogoutRequest logoutReq,
            LogoutResponse logoutRes, String hostedEntityID, String idpEntityID, String binding)
            throws SAML2Exception {
    }

    /**
     * Invokes after Fedlet receives SLO response from IDP and the SLO status
     * is not success.
     * @param request servlet request
     * @param response servlet response
     * @param logoutReq SAML2 <code>LogoutRequest</code> object
     * @param logoutRes SAML2 <code>LogoutResponse</code> object
     * @param hostedEntityID entity ID for the fedlet
     * @param idpEntityID entity id for the IDP to which the logout response
     *          is received from.
     * @param binding Single Logout binding used,
     *      one of following values:
     *          <code>SAML2Constants.SOAP</code>,
     *          <code>SAML2Constants.HTTP_POST</code>,
     *          <code>SAML2Constants.HTTP_REDIRECT</code>
     * @exception SAML2Exception if user want to fail the process.
     */
    default void onFedletSLOFailure(HttpServletRequest request, HttpServletResponse response, LogoutRequest logoutReq,
            LogoutResponse logoutRes, String hostedEntityID, String idpEntityID, String binding)
            throws SAML2Exception {
    }
}
