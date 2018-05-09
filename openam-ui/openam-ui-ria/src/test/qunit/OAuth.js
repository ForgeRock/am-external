/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
