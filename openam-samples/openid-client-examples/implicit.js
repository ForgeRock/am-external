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

// client_secret is not used in the implicit profile
var redirect_uri = server + openid + "/cb-implicit.html";
var state        = "af0ifjsldkj";
var nonce        = "n-0S6_WzA2Mj";

// ...END CONFIGURATION

/* Returns a map of parameters present in the document fragment. */
function getParamsFromFragment() {
    var params   = {};
    var postBody = location.hash.substring(1);
    var regex    = /([^&=]+)=([^&]*)/g, m;

    while (m = regex.exec(postBody)) {
        params[decodeURIComponent(m[1])] = decodeURIComponent(m[2]);
    }

    return params;
}
