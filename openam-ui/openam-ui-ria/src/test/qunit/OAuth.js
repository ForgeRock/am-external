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
 * Copyright 2016-2018 ForgeRock AS.
 */

define([
    "sinon",
    "org/forgerock/commons/ui/common/util/OAuth",
    "org/forgerock/commons/ui/common/util/URIUtils"
], function (sinon, OAuth, URIUtils) {
    QUnit.module('OAuth Functions');

    QUnit.moduleStart(function() {
        sinon.stub(URIUtils, "getCurrentOrigin", function () {
            return "http://rp.com";
        });
        sinon.stub(URIUtils, "getCurrentPathName", function () {
            return "/app/index.html";
        });
    });

    QUnit.moduleDone(function() {
        URIUtils.getCurrentOrigin.restore();
        URIUtils.getCurrentPathName.restore();
    });

    QUnit.test("oAuth redirect uri", function () {
        QUnit.equal(OAuth.getRedirectURI(),"http://rp.com/app/oauthReturn.html",
            "default oAuth redirect_uri matches"
        );
        QUnit.equal(OAuth.getRedirectURI('customOAuthReturn.html'),"http://rp.com/app/customOAuthReturn.html",
            "custom oAuth redirect_uri matches"
        );
    });
    QUnit.test("oAuth request url", function () {
        sinon.stub(OAuth, "generateNonce", function () {
            return "nonceValue";
        });
        QUnit.equal(OAuth.getRequestURL(
                "http://idp.com/request",
                "myClientId",
                "openid profile email",
                "MyState1234"
            ),
            "http://idp.com/request?response_type=code&scope=openid%20profile%20email&"+
            "redirect_uri=http://rp.com/app/oauthReturn.html&state=MyState1234"+
            "&nonce=nonceValue&client_id=myClientId",
            "generated oAuth request url matches expected value"
        );
        OAuth.generateNonce.restore();
    });

});
