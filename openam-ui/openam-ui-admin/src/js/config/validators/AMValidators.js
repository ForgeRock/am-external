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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import $ from "jquery";

const AMValidators = {
    "validPhoneFormat": {
        "name": "Valid Phone Number",
        "validator": (el, input, callback) => {
            const phonePattern = /^\+?([0-9\- ()])*$/;
            const value = input.val();
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
            const emailPattern = /^[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;
            const value = input.val();
            if (typeof value === "string" && value.length && !emailPattern.test(value)) {
                callback([$.t("common.form.validation.VALID_EMAIL_ADDRESS_FORMAT")]);
            } else {
                callback();
            }
        }
    }
};

export default AMValidators;
