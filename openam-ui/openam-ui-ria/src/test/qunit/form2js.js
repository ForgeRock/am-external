/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "form2js/src/form2js",
    "form2js/src/js2form"
], function ($, form2js, js2form) {
    QUnit.module('form2js usage');

    QUnit.test("boolean fields", function () {
        var form = $('<form><input type="checkbox" value="true" name="testBool"></form>')

        $("#qunit-fixture").append(form);

        js2form(form[0], {testBool: true});
        QUnit.equal(form.find("[name=testBool]").prop("checked"), true);
        QUnit.equal(form2js(form[0]).testBool, true);

        js2form(form[0], {testBool: false});
        QUnit.equal(form.find("[name=testBool]").prop("checked"), false);
        QUnit.equal(form2js(form[0]).testBool, false);
    });

});
