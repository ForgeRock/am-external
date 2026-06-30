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
 * This script allows customization of SAML2 Service Provider (SP) adapter behavior at various points
 * in the SAML2 authentication flow.
 *
 * Next-gen bindings available in this script (depending on the method being invoked):
 *
 * idpEntityId - String
 *      The entity ID of the Identity Provider.
 * profile - String
 *      The SAML2 profile being used (e.g., "urn:oasis:names:tc:SAML:2.0:profiles:SSO:browser").
 * spAdapterScriptHelper - SpAdapterScriptHelper
 *      Helper object providing utility methods for SP adapter operations.
 * requestHelper - HttpServletRequestHelper
 *      Helper object for accessing and manipulating HttpServletRequest properties (attributes, headers, parameters).
 * responseHelper - HttpServletResponseHelper
 *      Helper object for accessing and manipulating HttpServletResponse properties (headers, redirects).
 * isFederation - Boolean
 *      True if this is a federation scenario, false otherwise.
 * userId - String
 *      The universal ID of the user.
 * failureCode - Integer
 *      The failure code indicating the type of SSO failure (see SPAdapter constants).
 * ssoResponse - Map
 *      The SAML2 Response object from the IDP as a JSON map.
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * authnRequest - Map
 *      The SAML2 AuthnRequest object as a JSON map.
 * authnRequestHelper - AuthnRequestHelper
 *      Helper object for retrieving and updating properties of the AuthnRequest.
 * session - ScriptedSession
 *      The user's session object (when available).
 * realm - String
 *      The realm of the hosted SP.
 * hostedEntityId - String
 *      The entity ID of the hosted SP.
 * binding - String
 *      The SAML2 binding being used (e.g., "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST").
 */

/**
 * Invoked before AM sends the Single-Sign-On request to IDP.
 *
 * Available bindings:
 * idpEntityId - String
 *      The entity ID of the Identity Provider.
 * hostedEntityId - String
 *      The entity ID of the hosted SP.
 * realm - String
 *      The realm of the hosted SP.
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * authnRequest - Map
 *      The SAML2 AuthnRequest object as a JSON map.
 * authnRequestHelper - AuthnRequestHelper
 *      Helper object for retrieving and updating properties of the AuthnRequest.
 * spAdapterScriptHelper - SpAdapterScriptHelper
 *      Helper object providing utility methods for SP adapter operations.
 * requestHelper - HttpServletRequestHelper
 *      Helper object for accessing and manipulating HttpServletRequest properties (attributes, headers, parameters).
 * responseHelper - HttpServletResponseHelper
 *      Helper object for accessing and manipulating HttpServletResponse properties (headers, redirects).
 *
 * Throw an exception to fail the process.
 */
function preSingleSignOnRequest() {
}

/**
 * Invoked when AM receives the Single-Sign-On response from the IDP,
 * before any processing starts on the SP side.
 *
 * Available bindings:
 * hostedEntityId - String
 *      The entity ID of the hosted SP.
 * realm - String
 *      The realm of the hosted SP.
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * authnRequest - Map
 *      The SAML2 AuthnRequest object as a JSON map.
 * authnRequestHelper - AuthnRequestHelper
 *      Helper object for retrieving and updating properties of the AuthnRequest.
 * ssoResponse - Map
 *      The SAML2 Response object from the IDP as a JSON map.
 * profile - String
 *      The SAML2 profile being used (e.g., "urn:oasis:names:tc:SAML:2.0:profiles:SSO:browser").
 * spAdapterScriptHelper - SpAdapterScriptHelper
 *      Helper object providing utility methods for SP adapter operations.
 * requestHelper - HttpServletRequestHelper
 *      Helper object for accessing and manipulating HttpServletRequest properties (attributes, headers, parameters).
 * responseHelper - HttpServletResponseHelper
 *      Helper object for accessing and manipulating HttpServletResponse properties (headers, redirects).
 *
 * Throw an exception to fail the process.
 */
function preSingleSignOnProcess() {
}

/**
 * Invoked after Single-Sign-On processing succeeds.
 *
 * Available bindings:
 * hostedEntityId - String
 *      The entity ID of the hosted SP.
 * realm - String
 *      The realm of the hosted SP.
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * session - ScriptedSession
 *      The user's session object.
 * authnRequest - Map
 *      The SAML2 AuthnRequest object as a JSON map.
 * authnRequestHelper - AuthnRequestHelper
 *      Helper object for retrieving and updating properties of the AuthnRequest.
 * ssoResponse - Map
 *      The SAML2 Response object from the IDP as a JSON map.
 * profile - String
 *      The SAML2 profile being used (e.g., "urn:oasis:names:tc:SAML:2.0:profiles:SSO:browser").
 * isFederation - Boolean
 *      True if this is a federation scenario, false otherwise.
 * spAdapterScriptHelper - SpAdapterScriptHelper
 *      Helper object providing utility methods for SP adapter operations.
 * requestHelper - HttpServletRequestHelper
 *      Helper object for accessing and manipulating HttpServletRequest properties (attributes, headers, parameters).
 * responseHelper - HttpServletResponseHelper
 *      Helper object for accessing and manipulating HttpServletResponse properties (headers, redirects).
 *
 * Return true if browser redirection occurred after processing, false otherwise.
 * Throw an exception to fail the process.
 */
function postSingleSignOnSuccess() {
    return false;
}

/**
 * Invoked after Single-Sign-On processing fails.
 *
 * Available bindings:
 * hostedEntityId - String
 *      The entity ID of the hosted SP.
 * realm - String
 *      The realm of the hosted SP.
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * authnRequest - Map
 *      The SAML2 AuthnRequest object as a JSON map.
 * authnRequestHelper - AuthnRequestHelper
 *      Helper object for retrieving and updating properties of the AuthnRequest.
 * ssoResponse - Map
 *      The SAML2 Response object from the IDP as a JSON map.
 * profile - String
 *      The SAML2 profile being used (e.g., "urn:oasis:names:tc:SAML:2.0:profiles:SSO:browser").
 * failureCode - Integer
 *      The failure code indicating the type of SSO failure (see SPAdapter constants).
 * spAdapterScriptHelper - SpAdapterScriptHelper
 *      Helper object providing utility methods for SP adapter operations.
 * requestHelper - HttpServletRequestHelper
 *      Helper object for accessing and manipulating HttpServletRequest properties (attributes, headers, parameters).
 * responseHelper - HttpServletResponseHelper
 *      Helper object for accessing and manipulating HttpServletResponse properties (headers, redirects).
 *
 * Return true if browser redirection occurred, false otherwise.
 */
function postSingleSignOnFailure() {
    return false;
}

/**
 * Invoked after new Name Identifier processing succeeds.
 *
 * Available bindings:
 * hostedEntityId - String
 *      The entity ID of the hosted SP.
 * realm - String
 *      The realm of the hosted SP.
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * userId - String
 *      The universal ID of the user.
 * binding - String
 *      The SAML2 binding being used (e.g., "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST").
 * spAdapterScriptHelper - SpAdapterScriptHelper
 *      Helper object providing utility methods for SP adapter operations.
 * requestHelper - HttpServletRequestHelper
 *      Helper object for accessing and manipulating HttpServletRequest properties (attributes, headers, parameters).
 * responseHelper - HttpServletResponseHelper
 *      Helper object for accessing and manipulating HttpServletResponse properties (headers, redirects).
 */
function postNewNameIDSuccess() {
}

/**
 * Invoked after Terminate Name Identifier processing succeeds.
 *
 * Available bindings:
 * hostedEntityId - String
 *      The entity ID of the hosted SP.
 * realm - String
 *      The realm of the hosted SP.
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * userId - String
 *      The universal ID of the user.
 * binding - String
 *      The SAML2 binding being used (e.g., "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST").
 * spAdapterScriptHelper - SpAdapterScriptHelper
 *      Helper object providing utility methods for SP adapter operations.
 * requestHelper - HttpServletRequestHelper
 *      Helper object for accessing and manipulating HttpServletRequest properties (attributes, headers, parameters).
 * responseHelper - HttpServletResponseHelper
 *      Helper object for accessing and manipulating HttpServletResponse properties (headers, redirects).
 */
function postTerminateNameIDSuccess() {
}

/**
 * Invoked before single logout process starts on SP side.
 * This is called before the user session is invalidated on the service provider side.
 *
 * Available bindings:
 * hostedEntityId - String
 *      The entity ID of the hosted SP.
 * realm - String
 *      The realm of the hosted SP.
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * userId - String
 *      The universal ID of the user.
 * binding - String
 *      The SAML2 binding being used (e.g., "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST").
 * spAdapterScriptHelper - SpAdapterScriptHelper
 *      Helper object providing utility methods for SP adapter operations.
 * requestHelper - HttpServletRequestHelper
 *      Helper object for accessing and manipulating HttpServletRequest properties (attributes, headers, parameters).
 * responseHelper - HttpServletResponseHelper
 *      Helper object for accessing and manipulating HttpServletResponse properties (headers, redirects).
 *
 * Throw an exception to fail the process.
 */
function preSingleLogoutProcess() {
}

/**
 * Invoked after single logout process succeeds, i.e., user session has been invalidated.
 *
 * Available bindings:
 * hostedEntityId - String
 *      The entity ID of the hosted SP.
 * realm - String
 *      The realm of the hosted SP.
 * request - Map
 *      The HttpServletRequest object as a JSON map.
 * response - Map
 *      The HttpServletResponse object as a JSON map.
 * userId - String
 *      The universal ID of the user.
 * binding - String
 *      The SAML2 binding being used (e.g., "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST").
 * spAdapterScriptHelper - SpAdapterScriptHelper
 *      Helper object providing utility methods for SP adapter operations.
 * requestHelper - HttpServletRequestHelper
 *      Helper object for accessing and manipulating HttpServletRequest properties (attributes, headers, parameters).
 * responseHelper - HttpServletResponseHelper
 *      Helper object for accessing and manipulating HttpServletResponse properties (headers, redirects).
 */
function postSingleLogoutSuccess() {
}
