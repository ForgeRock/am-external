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
package org.forgerock.am.saml2.impl;

import org.forgerock.util.Reject;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.profile.ResponseInfo;
import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * Response data from SAML2 IDP, combined here for ease of access.
 */
public class Saml2ResponseData {

    private Subject subject;
    private Assertion assertion;
    private String sessionIndex;
    private ResponseInfo responseInfo;
    private AuthnRequest authnRequest;

    /**
     * Dummy creator, used by databinder to generate this POJO.
     */
    public Saml2ResponseData() {
    }

    /**
     * Combine a new set of SAML2 response data as a single object.
     *
     * @param sessionIndex Session index used for this authentication.
     *                     SAML2 specs allow it to be optional (null).
     * @param subject Subject about whom this authentication provides information.
     * @param assertion Assertion for this subject's authentication.
     * @param responseInfo Response Information pertaining to the authentication.
     * @param authnRequest The SAML2 authentication request that was used to initiate SAML.
     */
    public Saml2ResponseData(String sessionIndex, Subject subject, Assertion assertion, ResponseInfo responseInfo,
            AuthnRequest authnRequest) {
        Reject.ifNull(subject, assertion, responseInfo);

        this.subject = subject;
        this.assertion = assertion;
        this.sessionIndex = sessionIndex;
        this.responseInfo = responseInfo;
        this.authnRequest = authnRequest;
    }

    /**
     * Sets the subject value.
     *
     * @param subject value of the subject.
     */
    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    /**
     * Sets the assertion.
     *
     * @param assertion value of the assertion.
     */
    public void setAssertion(Assertion assertion) {
        this.assertion = assertion;
    }

    /**
     * Sets the sessionIndex value.
     *
     * @param sessionIndex value of the sessionIndex.
     */
    public void setSessionIndex(String sessionIndex) {
        this.sessionIndex = sessionIndex;
    }

    /**
     * Sets the responseInfo value.
     *
     * @param responseInfo value of the responseInfo.
     */
    public void setResponseInfo(ResponseInfo responseInfo) {
        this.responseInfo = responseInfo;
    }

    /**
     * Get the subject. Will not be null.
     * @return the subject.
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * Get the assertion. Will not be null.
     * @return the assertion.
     */
    public Assertion getAssertion() {
        return assertion;
    }

    /**
     * Get the session index. Will not be null.
     * @return the session index.
     */
    public String getSessionIndex() {
        return sessionIndex;
    }

    /**
     * Get the response info. Will not be null.
     * @return the response info.
     */
    public ResponseInfo getResponseInfo() {
        return responseInfo;
    }

    /**
     * Get the original authentication request.
     *
     * @return The original authentication request.
     */
    public AuthnRequest getAuthnRequest() {
        return authnRequest;
    }
}
