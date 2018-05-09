/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery"
], ($) => {
    var obj = {
        "validPhoneFormat": {
            "name": "Valid Phone Number",
            "validator": (el, input, callback) => {
                var phonePattern = /^\+?([0-9\- ()])*$/,
                    value = input.val();
                if (typeof value === "string" && value.length && !phonePattern.test(value)) {
                    callback([$.t("common.form.validation.VALID_PHONE_FORMAT")]);
                } else {
                    callback();
                }
            }
        },
        "validEmailAddressFormat": {
            "name": "Valid Email Address",
            "validator": (el, input, callback) => {
                //https://www.w3.org/TR/2012/WD-html-markup-20120320/datatypes.html#form.data.emailaddress
                var emailPattern = /^[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/,
                    value = input.val();
                if (typeof value === "string" && value.length && !emailPattern.test(value)) {
                    callback([$.t("common.form.validation.VALID_EMAIL_ADDRESS_FORMAT")]);
                } else {
                    callback();
                }
            }
        }
    };
    return obj;
});
