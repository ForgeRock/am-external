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
 * Copyright 2016-2025 Ping Identity Corporation.
 */
// START CONFIGURATION...

// To avoid cross-site scripting questions,
// this demo should be in the same container
// as the OpenID Connect provider (OpenAM).
function getBaseURL() {
    var protocol = window.location.protocol;
    var hostname = window.location.hostname;
    var port     = window.location.port;
    return protocol + "//" + hostname + ":" + port;
}
var server        = getBaseURL();

// OpenAM is assumed to be deployed under /openam.
var openam        = "/openam";
var authorize     = "/oauth2/authorize";
var access        = "/oauth2/access_token";
var info          = "/oauth2/userinfo";

// This application's URI, client_id, client_secret.
var openid        = "/openid";
var client_id     = "myClientID";
var client_secret = "password";
var client_realm  = "/";

// ...END CONFIGURATION

// http://stackoverflow.com/ has lots of useful snippets...
function encodeQueryData(data) {
    var ret = [];
    for (var d in data) {
        ret.push(encodeURIComponent(d) + "="
            + encodeURIComponent(data[d]));
    }
    return ret.join("&");
}

/* Returns a map of query string parameters. */
function parseQueryString() {
    var query = {};
    var args  = document.location.search.substring(1).split('&');
    for (var arg in args) {
        var m = args[arg].split('=');
        query[decodeURIComponent(m[0])] = decodeURIComponent(m[1]);
    }

    return query;
}

/* Validates a JWS signature according to
   https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-33#section-5.2
   cheating a bit by taking the pre-encoded header and payload.
 */
function validateSignature(encodedHeader, encodedPayload, signature) {
  var signingInput   = encodedHeader + "." + encodedPayload;
  var signed         = CryptoJS.HmacSHA256(signingInput, client_secret);
  var encodedSigned  = b64tob64u(signed.toString(CryptoJS.enc.Base64));
  return encodedSigned == signature;
}

/* Returns a base64url-encoded version of the base64-encoded input string. */
function b64tob64u(string) {
    var result = string;
    result = result.replace(/\+/g, "-");
    result = result.replace(/\//g, "_");
    result = result.replace(/=/g, "");
    return result;
}
