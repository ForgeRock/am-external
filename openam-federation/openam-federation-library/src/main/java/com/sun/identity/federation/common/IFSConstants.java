/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IFSConstants.java,v 1.12 2008/08/29 04:57:15 exu Exp $
 *
 * Portions Copyrights 2014-2019 ForgeRock AS.
 */
package com.sun.identity.federation.common;

import org.forgerock.openam.annotations.SupportedAll;

/**
 * This interface represents a collection of common constants used by
 * the classes in Federation Service.  
 */
@SupportedAll
@Deprecated
public interface IFSConstants {
    /**
     * Status code: <code>samlp:Responder</code>  
     */
    String SAML_RESPONDER = "samlp:Responder";
    /**
     * Logout status: Success
     */
    String LOGOUT_SUCCESS="logoutSuccess";
    /**
     * Logout status: Failure
     */
    String LOGOUT_FAILURE="logoutFailure";
    /**
     * RelayState
     */
    String RELAY_STATE ="RelayState";
    /**
     * SAMLRequest query parameter name
     */
    String SAML_REQUEST = "SAMLRequest";
    /**
     * SAMLResponse query parameter name
     */
    String SAML_RESPONSE = "SAMLResponse";
    /**
     * Parameter name for SAML artifact in http request.
     */
    String SAML_ART = "SAMLart";
}

