/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface to describe an object that can check a cookie and then perform SAML2 Redirect based on the result.
 */
public interface FederateCookieRedirector {

    /**
     * Establishes whether or not the SAML2 cookie is set.
     *
     * @param request  the SAML2 request
     * @param response the saml2 Response
     * @param isIDP    whether this request was from an IDP
     * @return true if the cookie is set.
     */
    boolean isCookieSet(
            HttpServletRequest request, HttpServletResponse response,
            boolean isIDP);

    /**
     * Sets the cookie for the SAML2 Request and redirects the request.
     *
     * @param request the SAML2 Request object
     * @param response the SAML2 Resposne object
     * @param isIDP whether this request was from and idp
     * @throws UnableToRedirectException if there was a problem preforming the redirect.
     */
    void setCookieAndRedirect(HttpServletRequest request, HttpServletResponse response,
                              boolean isIDP) throws UnableToRedirectException;

    /**
     * Sets the cookie for the SAML2 Request and redirects the request.
     *
     * @param request the SAML2 Request object
     * @param response the SAML2 Resposne object
     * @param isIDP whether this request was from and idp
     */
    boolean ifNoCookieIsSetThenSetTheCookieThenRedirectToANewRequestAndReturnTrue(
            HttpServletRequest request, HttpServletResponse response,
            boolean isIDP);

    /**
     * Sets the cookie if required and then redirects the SAML2 request.  Returns a boolean to indicate whether the
     * redirect action was taken.
     *
     * @param request the SAML2 Request object
     * @param response the SAML2 Resposne object
     * @param isIDP whether this request was from and idp
     * @return true if the redirect action was performed
     */
    boolean needSetLBCookieAndRedirect(
            HttpServletRequest request, HttpServletResponse response,
            boolean isIDP);
}
