/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash"
], (_) => {
    var obj = function FormCollection () {
        this.forms = [];
    };

    obj.prototype.add = function (form) {
        this.forms.push(form);
    };

    obj.prototype.data = function () {
        return _.reduce(this.forms, (merged, form) => {
            return _.merge(merged, form.data());
        }, this.forms[0].data());
    };

    obj.prototype.reset = function () {
        _.each(this.forms, (form) => {
            form.reset();
        });
    };

    return obj;
});
