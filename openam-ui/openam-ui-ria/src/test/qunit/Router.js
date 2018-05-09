/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/Router"
], function (Router) {
    QUnit.module('Router Functions');

    QUnit.test("getLink", function () {
        var fakeRoute = {
            url: /fake-(.+)\-(.+)/,
            pattern: "fake-?-?"
        };

        QUnit.equal(Router.getLink(fakeRoute, ["simple", "value"]), "fake-simple-value");
        QUnit.equal(Router.getLink(fakeRoute, ["comp?lex", "value"]), "fake-comp?lex-value");
        QUnit.equal(Router.getLink(fakeRoute, ["part?ial"]), "fake-part?ial-");
        QUnit.equal(Router.getLink(fakeRoute, []), "fake--");

    });

});
