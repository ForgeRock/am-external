/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2020 ForgeRock AS.
 * Copyright 2011 Cybernetica AS.
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.authentication.modules.oauth2;

import static com.google.common.collect.Sets.intersection;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.COOKIE_ORIG_URL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_ACTIVATION;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_CODE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_OAUTH_VERIFIER;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.esapi.ESAPI;

/**
 * OAuth module specific Get2Post gateway. 
 * When using the legacy authentication UI we need to transform the incoming GET request (from the OAuth2 AS) to a POST
 * request, so that the authentication framework can remain to use the same authentication session for the corresponding
 * authentication attempt (this is because GET requests made to /UI/Login always result in a new authentication
 * session).
 * The XUI on the other hand currently does not have a way to continue an existing authentication process, hence the
 * OAuth module currently just redirects to /openam retaining the query string that was used to access the
 * OAuthProxy.jsp. Since performing a POST request against a static resource can result in HTTP 405 (e.g. on WildFly),
 * in case XUI is enabled we perform a redirect instead to "continue" the authentication process.
 */
public class OAuthProxy  {

    /*
     * These query string parameters are part of the ORIG_URI (The value set in the Cookie)
     * and hence should not be part of the request (coming from authorization server) to the redirect uri.
     */
    private static Set<String> RESERVED_QUERY_PARAMS = asSet("service", "module", "realm", "authIndexType",
            "authIndexValue", "user", "role", "authlevel", "sunamcompositeadvice");

    public static void continueAuthentication(HttpServletRequest req, HttpServletResponse res, PrintWriter out) {
        OAuthUtil.debugMessage("toPostForm: started");

        String action = OAuthUtil.findCookie(req, COOKIE_ORIG_URL);
        
        if (OAuthUtil.isEmpty(action)) {
            OAuthUtil.debugError("OAuthProxy.toPostForm: Original Url Cookie is empty");
            out.println(getError("Request not valid !"));
            return;
        }

        Map<String, String[]> params = req.getParameterMap();
        
        if (!params.containsKey(PARAM_CODE) && !params.containsKey(PARAM_ACTIVATION)
                && !params.containsKey(PARAM_OAUTH_VERIFIER)) {
            OAuthUtil.debugError("OAuthProxy.toPostForm: Parameters " + PARAM_CODE + " or " + PARAM_ACTIVATION
                    + " were not present in the request");
            out.println(getError("Request not valid, perhaps a permission problem"));
            return;
        }

        try {
            String code = req.getParameter(PARAM_CODE);
            if (code != null && !OAuthUtil.isEmpty(code)) {
                if (!ESAPI.validator().isValidInput(PARAM_CODE, code, "HTTPParameterValue", 2000, true)) {
                    OAuthUtil.debugError("OAuthProxy.toPostForm: Parameter " + PARAM_CODE
                            + " is not valid!! : " + code);
                    out.println(getError("Invalid authorization code"));
                    return;
                }
            }

            if (hasReservedParameters(req)) {
                OAuthUtil.debugError("OAuthProxy.toPostForm: Request has reserved parameters in the query string. " +
                        "Parameters: " + req.getParameterMap().keySet());
                out.println(getError("Request not valid !"));
                return;
            }

            if (action.contains("?")) {
                action += "&" + req.getQueryString();
            } else {
                action += "?" + req.getQueryString();
            }

            // OAuthProxy.jsp should be always accessed via GET, hence the querystring should contain all important
            // parameters already.
            res.sendRedirect(action);
        } catch (Exception e) {
            out.println(getError(e.getMessage()));
        }
    }

    private static boolean hasReservedParameters(HttpServletRequest req) {
        return !intersection(req.getParameterMap().keySet(), RESERVED_QUERY_PARAMS).isEmpty();
    }

    private static String getError(String message) {
        StringBuffer html = new StringBuffer();
        html.append("<html>\n").append("<body>\n")
            .append("<h1>\n").append(ESAPI.encoder().encodeForHTML(message)).append("</h1>\n")
            .append("</body>\n").append("</html>\n");
        return html.toString();
    }
}
