/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.saml2;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.profile.ResponseInfo;
import org.forgerock.util.Reject;

/**
 * Response data from SAML2 IDP, combined here for ease of access.
 */
public class SAML2ResponseData {

    private Subject subject;
    private Assertion assertion;
    private String sessionIndex;
    private ResponseInfo responseInfo;

    /**
     * Dummy creator, used by databinder to generate this POJO.
     */
    public SAML2ResponseData() {

    }

    /**
     * Combine a new set of SAML2 response data as a single object.
     *
     * @param sessionIndex Session index used for this authentication.
     *                     SAML2 specs allow it to be optional (null).
     * @param subject Subject about whom this authentication provides information.
     * @param assertion Assertion for this subject's authentication.
     * @param responseInfo Response Information pertaining to the authentication.
     */
    public SAML2ResponseData(String sessionIndex, Subject subject, Assertion assertion, ResponseInfo responseInfo) {
        Reject.ifNull(subject, assertion, responseInfo);

        this.subject = subject;
        this.assertion = assertion;
        this.sessionIndex = sessionIndex;
        this.responseInfo = responseInfo;
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
}
