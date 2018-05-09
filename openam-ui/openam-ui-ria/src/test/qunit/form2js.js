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
