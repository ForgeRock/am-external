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
 * Copyright 2026 Ping Identity Corporation.
 */

/*
 * This script allows customization of SAML2 Identity Provider (IDP) adapter behavior at various points
 * in the SAML2 authentication flow.
 *
 * Next-gen bindings available in this script (depending on the method being invoked):
 *
 * hostedEntityId - String
 *      The entity ID of the hosted IDP.
 * realm - String
 *      The realm of the hosted IDP.
 * idpAdapterScriptHelper - IdpAdapterNextGenScriptHelper
 *      Helper object providing utility methods for IDP adapter operations.
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * requestHelper - HttpServletRequestHelper
 *      Helper for reading request attributes, headers, and parameters.
 * responseHelper - HttpServletResponseHelper
 *      Helper for writing response headers and redirects.
 * authnRequest - Map
 *      The SAML2 AuthnRequest object as a JSON map.
 * ssoResponse - Map
 *      The SAML2 Response object as a JSON map.
 * reqId - String
 *      The request ID used for continuation after redirect.
 * session - ScriptedSession
 *      The user's session object (when available). Supports getProperty/setProperty.
 * relayState - String
 *      The relayState value used in the redirect.
 * faultCode - String
 *      The fault code that will be returned in the SAML response.
 * faultDetail - String
 *      The fault detail that will be returned in the SAML response.
 *
 * Bindings not listed for a method will be null.
 */

/**
 * Invoked when AM receives the authentication request from the SP for the first time,
 * before any processing starts on the IDP side.
 *
 * Available bindings:
 * hostedEntityId - String
 * realm - String
 * idpAdapterScriptHelper - IdpAdapterNextGenScriptHelper
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * requestHelper - HttpServletRequestHelper
 *      Helper for reading request attributes, headers, and parameters.
 * responseHelper - HttpServletResponseHelper
 *      Helper for writing response headers and redirects.
 * authnRequest - Map
 *      The SAML2 AuthnRequest object as a JSON map.
 * reqId - String
 *
 * Return true if browser redirection is happening after processing, false otherwise.
 * Throw an exception to fail the process.
 */
function preSingleSignOn() {
    return false;
}

/**
 * Invoked when AM has processed the authentication request and is ready to redirect to authentication.
 *
 * Available bindings:
 * hostedEntityId - String
 * realm - String
 * idpAdapterScriptHelper - IdpAdapterNextGenScriptHelper
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * requestHelper - HttpServletRequestHelper
 *      Helper for reading request attributes, headers, and parameters.
 * responseHelper - HttpServletResponseHelper
 *      Helper for writing response headers and redirects.
 * authnRequest - Map
 *      The SAML2 AuthnRequest object as a JSON map.
 * reqId - String
 * session - ScriptedSession
 * relayState - String
 *
 * Return true if browser redirection is happening after processing, false otherwise.
 * Throw an exception to fail the process.
 */
function preAuthentication() {
    return false;
}

/**
 * Invoked before sending a non-error SAML2 Response, but before the response object is constructed.
 *
 * Available bindings:
 * hostedEntityId - String
 * realm - String
 * idpAdapterScriptHelper - IdpAdapterNextGenScriptHelper
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * requestHelper - HttpServletRequestHelper
 *      Helper for reading request attributes, headers, and parameters.
 * responseHelper - HttpServletResponseHelper
 *      Helper for writing response headers and redirects.
 * authnRequest - Map
 *      The SAML2 AuthnRequest object as a JSON map.
 * reqId - String
 * session - ScriptedSession
 * relayState - String
 *
 * Return true if browser redirection happened after processing, false otherwise.
 * Throw an exception to fail the process.
 */
function preSendResponse() {
    return false;
}

/**
 * Invoked after the SAML2 Response object is created, but before it is signed/encrypted.
 *
 * Available bindings:
 * hostedEntityId - String
 * realm - String
 * idpAdapterScriptHelper - IdpAdapterNextGenScriptHelper
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * requestHelper - HttpServletRequestHelper
 *      Helper for reading request attributes, headers, and parameters.
 * authnRequest - Map
 *      The SAML2 AuthnRequest object as a JSON map.
 * session - ScriptedSession
 * relayState - String
 * ssoResponse - Map
 *      The SAML2 Response object as a JSON map.
 */
function preSignResponse() {
}

/**
 * Invoked before a SAML error message is returned.
 *
 * Available bindings:
 * hostedEntityId - String
 * realm - String
 * idpAdapterScriptHelper - IdpAdapterNextGenScriptHelper
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * requestHelper - HttpServletRequestHelper
 *      Helper for reading request attributes, headers, and parameters.
 * responseHelper - HttpServletResponseHelper
 *      Helper for writing response headers and redirects.
 * faultCode - String
 * faultDetail - String
 */
function preSendFailureResponse() {
}
