/*
 * Copyright 2012-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "underscore"
], function($, _) {
    var obj = {};

    obj.namePattern = new RegExp("^([A-Za'-\u0105\u0107\u0119\u0142\u00F3\u015B\u017C\u017A" +
        "\u0104\u0106\u0118\u0141\u00D3\u015A\u017B\u0179\u00C0\u00C8\u00CC\u00D2\u00D9\u00E0\u00E8\u00EC\u00F2" +
        "\u00F9\u00C1\u00C9\u00CD\u00D3\u00DA\u00DD\u00E1\u00E9\u00ED\u00F3\u00FA\u00FD\u00C2\u00CA\u00CE\u00D4" +
        "\u00DB\u00E2\u00EA\u00EE\u00F4\u00FB\u00C3\u00D1\u00D5\u00E3\u00F1\u00F5\u00C4\u00CB\u00CF\u00D6\u00DC" +
        "\u0178\u00E4\u00EB\u00EF\u00F6\u00FC\u0178\u00A1\u00BF\u00E7\u00C7\u0152\u0153\u00DF\u00D8\u00F8\u00C5" +
        "\u00E5\u00C6\u00E6\u00DE\u00FE\u00D0\u00F0\-\s])+$");
    obj.phonePattern = /^\+?([0-9\- \(\)])*$/;
    obj.emailPattern = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/;

    obj.setErrors = function(el, validatorType, msg) {
        _.each(validatorType.split(' '), function (vt) {
            _.each(el.find("span[data-for-validator=" + vt + "]"), function (input) {
                var $input = el.find(input),
                    type = $input.attr("data-for-req"),
                    span = $input.prev("span");
                if (!type) {
                    type = $input.text();
                }

                if ($.inArray(type, msg) !== -1) {
                    span.removeClass('has-success');
                    span.addClass('has-error');
                } else {
                    span.removeClass('has-error');
                    span.addClass('has-success');
                }
            });
        });
    };

    obj.hideValidation = function ($input, el) {
        $input.nextAll("span").hide();
        $input.nextAll("div.validation-message:first").hide();
        el.find("div.validation-message[for='" + $input.attr('name') + "']").hide();
    };

    obj.showValidation = function ($input, el) {
        $input.nextAll("span").show();
        $input.nextAll("div.validation-message:first").show();
        el.find("div.validation-message[for='" + $input.attr('name') + "']").show();
    };

    obj.hideBox = function(el) {
        el.find(".group-field-errors").hide();
    };

    obj.showBox = function(el) {
        el.find(".group-field-errors").show();
    };

    return obj;
});
