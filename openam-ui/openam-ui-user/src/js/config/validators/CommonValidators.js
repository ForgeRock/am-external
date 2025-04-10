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
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { t } from "i18next";

const CommonValidators = {
    "required": {
        "name": "Required field",
        "validator" (el, input, callback) {
            const v = input.val();
            if (!v || v === "") {
                callback([t("common.form.validation.required")]);
            } else {
                callback();
            }
        }
    },
    "passwordConfirm": {
        "name": "Password confirmation",
        "validator" (el, input, callback) {
            const confirmValue = input.val();
            const mainInput = el.find(`:input#${input.attr("passwordField")}`);

            if (mainInput.val() !== confirmValue) {
                callback([t("common.form.validation.confirmationMatchesPassword")]);
            } else if (mainInput.attr("data-validation-status") === "error") {
                callback(mainInput.data("validation-failures"));
            } else {
                callback();
            }
        }
    },
    "minLength": {
        "name": "Minimum number of characters",
        "validator" (el, input, callback) {
            const v = input.val();
            const len = input.attr("minLength");

            if (v.length < len) {
                callback([t("common.form.validation.MIN_LENGTH", { minLength: len })]);
            } else {
                callback();
            }
        }
    },
    "atLeastXNumbers": {
        "name": "Minimum occurrence of numeric characters in string",
        "validator" (el, input, callback) {
            const v = input.val();
            const minNumbers = input.attr("atLeastXNumbers");
            const foundNumbers = v.match(/\d/g);

            if (!foundNumbers || foundNumbers.length < minNumbers) {
                callback([t("common.form.validation.AT_LEAST_X_NUMBERS", { numNums: minNumbers })]);
            } else {
                callback();
            }
        }
    },
    "atLeastXCapitalLetters": {
        "name": "Minimum occurrence of capital letter characters in string",
        "validator" (el, input, callback) {
            const v = input.val();
            const minCapitals = input.attr("atLeastXCapitalLetters");
            const foundCapitals = v.match(/[(A-Z)]/g);

            if (!foundCapitals || foundCapitals.length < minCapitals) {
                callback([t("common.form.validation.AT_LEAST_X_CAPITAL_LETTERS", { numCaps: minCapitals })]);
            } else {
                callback();
            }
        }
    }
};
export default CommonValidators;
