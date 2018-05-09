/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "handlebars-template-loader/runtime",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function (_, Handlebars, UIUtils) {
    QUnit.module('UIUtils Functions');

    QUnit.test("Static Select", function () {
        var template = Handlebars.compile("<select>" +
            "{{#staticSelect testVal}}" +
            "<option value='1'>1</option>" +
            "<option value='2'>2</option>" +
            "<option value='text/html'>text/html</option>" +
            "<option value=\"tick'test\">tick'test</option>" +
            "<option value='less<test'>less&lt;test</option>" +
            "<option value='and&test'>and&amp;test</option>" +
            "<option value='false'>boolean&amp;test</option>" +
            "{{/staticSelect}}" +
        "</select>");

        var testHTML = template({"testVal": "2"});
        QUnit.equal($(testHTML).val(), "2", "2 option selected");

        testHTML = template({"testVal": "text/html"});
        QUnit.equal($(testHTML).val(), "text/html", "text/html option selected");

        testHTML = template({"testVal": "tick'test"});
        QUnit.equal($(testHTML).val(), "tick'test", "tick'test option selected");

        testHTML = template({"testVal": "less<test"});
        QUnit.equal($(testHTML).val(), "less<test", "less<test option selected");

        testHTML = template({"testVal": "and&test"});
        QUnit.equal($(testHTML).val(), "and&test", "and&test option selected");

        testHTML = template({"testVal": false});
        QUnit.equal($(testHTML).val(), "false", "boolean&amp;test option selected");
    });
});
