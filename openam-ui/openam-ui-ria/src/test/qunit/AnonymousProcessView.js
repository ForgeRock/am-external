/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, AnonymousProcessView, UIUtils) {
    QUnit.module('AnonymousProcessView Functions');

    QUnit.asyncTest("buildQueryFilter", function () {
        var el = $("#qunit-fixture #wrapper");

        UIUtils.renderTemplate(
            "templates/user/process/reset/userQuery-initial.html",
            el,
            {},
            function () {
                el.find(":input[name=userName]").val("bjensen");

                QUnit.equal(
                    AnonymousProcessView.prototype.walkTreeForFilterStrings(el.find("#filterContainer")),
                    'userName eq "bjensen"',
                    "Simple query filter generated from template matches expected input"
                );

                el.find(":input[name=userName]").val("bjensen");
                el.find(":input[name=mail]").val("bjensen@example.com");
                el.find(":input[name=givenName]").val("Barbara");
                el.find(":input[name=sn]").val("Jensen");

                QUnit.equal(
                    AnonymousProcessView.prototype.walkTreeForFilterStrings(el.find("#filterContainer")),
                    '(userName eq "bjensen" OR mail eq "bjensen@example.com" OR (givenName eq "Barbara" AND sn eq "Jensen"))',
                    "Complex query filter generated from template matches expected input"
                );

                QUnit.start();
            }
        );
    });
});
