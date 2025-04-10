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
 * Copyright 2013-2025 Ping Identity Corporation.
 */
// START CONFIGURATION...

var redirect_uri  = server + openid + "/cb-basic.html";
var state         = "af0ifjsldkj";

// ...END CONFIGURATION

/* Returns the value of the named query string parameter. */
function getParameterByName(name) {
    name        = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS  = "[\\?&]" + name + "=([^&#]*)";
    var regex   = new RegExp(regexS);
    var results = regex.exec(window.location.search);

    if (results == null) {
        return "";
    }

    return decodeURIComponent(results[1].replace(/\+/g, " "));
}

/* Returns an HTTP Basic Authentication header string. */
function authHeader(user, password) {
    var tok  = user + ':' + password;
    var hash = btoa(tok); // Default: bXlDbGllbnRJRDpwYXNzd29yZA==
    // console.log("hash: " + hash);
    return "Basic " + hash;
}
