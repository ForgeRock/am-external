/**
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
 * $Id: IDPAuthnContextInfo.java,v 1.3 2008/06/25 05:47:51 qcheng Exp $
 *
 * Portions Copyrighted 2011-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins;

import java.util.Map;
import java.util.Set;

import org.forgerock.openam.annotations.SupportedAll;

import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.common.SAML2Utils;

/** 
 * The class <code>IDPAuthnContextInfo</code> consists of the mapping 
 * between <code>AuthnContextClassRef</code> and the actual 
 * authentication mechanism at the Identity Provider. 
 *
 */ 
@SupportedAll
public class IDPAuthnContextInfo {
    AuthnContext authnContext;
    Set authnTypeAndValues;
    Integer authnLevel;
    boolean requiresRedirectToAuth;

   /** 
    * The constructor. 
    *
    * @param authnContext The <code>AuthnContext</code> that is returned
    *  to the requester.
    * @param authnTypeAndValues The set of authentication mechanism
    * @param authnLevel The Authentication Level associated to the Authentication
    *  context
    */ 
    public IDPAuthnContextInfo(AuthnContext authnContext,
                            Set authnTypeAndValues, Integer authnLevel) {
        this(authnContext, authnTypeAndValues, authnLevel, false);
    }

    /**
     * The constructor.
     *
     * @param authnContext The <code>AuthnContext</code> that is returned
     *  to the requester.
     * @param authnTypeAndValues The set of authentication mechanism
     * @param authnLevel The Authentication Level associated to the Authentication context
     * @param requiresRedirectToAuth whether this Authentication context requires that the IDP redirect to auth
     */
    public IDPAuthnContextInfo(AuthnContext authnContext,
            Set authnTypeAndValues, Integer authnLevel, boolean requiresRedirectToAuth) {
        this.authnContext = authnContext;
        this.authnTypeAndValues = authnTypeAndValues;
        this.authnLevel = authnLevel;
        this.requiresRedirectToAuth = requiresRedirectToAuth;
    }

   /** 
    * Returns the returning <code>AuthnContext</code>
    *
    * @return the returning <code>AuthnContext</code>
    */ 
    public AuthnContext getAuthnContext() {
        return authnContext;
    }

   /** 
    * Returns the set of authentication mechanism
    *
    * @return the set of authentication mechanism
    */ 
    public Set getAuthnTypeAndValues() {
        return authnTypeAndValues;
    }

    /**
     * Returns a map of the authentication mechanism.
     */
    public Map<String, String> getAuthnTypeAndValuesAsMap() {
        return SAML2Utils.setToMap(authnTypeAndValues);
    }

    /**
    * Returns the Authentication Level
    *
    * @return the Authentication level
    */
    public Integer getAuthnLevel() {
        return authnLevel;
    }

    /**
     * Whether the AuthnContextInfo requires AM to redirect to auth service.
     *
     * This is true if we have a service set directly on the remote SP, or we have a service specified via AuthnContext
     * that has 'mustRun' set on the corresponding tree configuration.
     *
     * @return {@code true} if SAML should redirect the user to authenticate, otherwise false
     */
    public boolean requiresRedirectionToAuth() {
        return requiresRedirectToAuth;
    }
}

