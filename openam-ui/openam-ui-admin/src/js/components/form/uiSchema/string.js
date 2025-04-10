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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { t } from "i18next";

import isPassword from "../schema/isPassword";

const string = (schema, isEditValidationMode) => {
    if (isPassword(schema)) {
        return {
            "ui:placeholder": t("common.form.passwordPlaceholder"),
            "ui:widget": "password"
        };
    } else if (schema.acceptedFiles && schema.format === "file") {
        return {
            "ui:field": "UploadField",
            options: {
                instructions: t(
                    "console.applications.federation.entityProviders.new.remote.xmlUploadInstructions",
                    { fileType: schema.acceptedFiles }
                ),
                multiple: false,
                icon: "fa fa-file-code-o"
            }
        };
    } else if (schema.prefix) {
        return { "ui:widget": "PrefixWidget" };
    } else if (schema.format === "date-time") {
        return { "ui:field": "DateTimeField" };
    } else if (schema.enum) {
        return { "ui:widget": "EnumWidget" };
    } else if (isEditValidationMode) {
        /**
         * Properties that have resulted in an empty value due to user interaction are resolved to an empty string.
         * This behaviour explicitly allows a user to clear the value from a property.
         * @see https://github.com/mozilla-services/react-jsonschema-form#the-case-of-empty-strings
         */
        return { "ui:emptyValue": "" };
    }
};

export default string;
