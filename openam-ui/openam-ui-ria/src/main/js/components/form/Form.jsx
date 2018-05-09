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
 * Copyright 2017-2018 ForgeRock AS.
 */

import { findKey, map, mapValues, sortBy } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";
import ReactJSONSchemaForm from "react-jsonschema-form";

import fields from "components/form/fields/index";
import isPassword from "./schema/isPassword";
import removePasswordsFromRequired from "./schema/removePasswordsFromRequired";
import VerticalFormFieldTemplate from "components/form/fields/VerticalFormFieldTemplate";
import widgets from "components/form/widgets/index";

/**
 * Form component for JSON Schema. Wraps the react-jsonschema-form library.
 * # `null` vs `undefined` Values
 * AM's API returns `null` for values that do no exist.
 * <p>
 * `null` is a special type in JavaScript and triggers type validation errors, thus, all `null`
 * values are transformed to `undefined` to ensure type validation errors are not displayed.
 * 
 * # Validation Mode
 * Validation modes tailor the form validation to be optimised for either object ***creation***
 * or ***editing***.
 * <p>
 * `validationMode` `"create"` (the default) uses standard validation, as implemented by react-jsonschema-form.
 * <p>
 * `validationMode` `"edit"` uses standard validation, plus the following behaviours:
 * - Password properties that are `required`, are removed from the list of `required` properties.
 *   - AM never returns passwords from it's API for security reasons.
 *   - A form that is editing an existing object will display validation errors for required
 *     password fields.
 *   - An empty password property will not update the password, so the removal of the `required`
 *     validation allows editing of an existing object without the user being prompted to supply
 *     a passsword that may already exist.
 * - String properties which have no input will default to `""` (empty string) rather than `undefined`. 
 *   - When providing values for a string property, `undefined` signals no value and triggers
 *     validation, whereas `""` (empty string) does not.
 *   - For forms that ***create*** objects, the first behaviour is desirable so the user is prompted to enter
 *     values for all required properties.
 *   - For forms that ***edit*** objects, the user may wish to set a string property to `""` (empty string),
 *     making the second behaviour desirable.
 * @module components/form/Form
 * @param {Object} props Component properties.
 * @param {Object} props.formData JSON object containing the form data.
 * @param {Object} props.schema JSON Schema.
 * @param {string} props.validationMode=create Validation mode. Possible values are `create` or `edit`.
 * @returns {ReactElement} Form component for JSON Schema.
 * @see https://github.com/mozilla-services/react-jsonschema-form
 * @see https://bugster.forgerock.org/jira/browse/OPENAM-13005
 */
const Form = ({ formData, schema, validationMode, ...restProps }) => {
    const isEditValidationMode = validationMode === "edit";

    /**
     * AM's API returns `null` for values that do no exist. `null` is a special type in JavaScript
     * and triggers type validation errors, thus, all `null` values are transformed to `undefined`
     * to ensure type validation errors are not displayed.
     * 
     * Remove this tranform when the API returns `undefined` by default.
     * @see OPENAM-13005
     */
    formData = mapValues(formData, (value) => { return value === null ? undefined : value; });

    const orderedProperties = sortBy(schema.properties, "propertyOrder");

    const uiSchema = {
        "ui:order": map(orderedProperties, (property) => findKey(schema.properties, property)),
        ...mapValues(schema.properties, (property) => {
            const uiSchema = {};

            if (property.type === "boolean") {
                uiSchema["ui:widget"] = "ToggleSwitchWidget";
            } else if (property.type === "object") {
                uiSchema["ui:field"] = "KeyValueField";
            } else if (property.type === "string") {
                if (isPassword(property)) {
                    uiSchema["ui:widget"] = "password";
                    uiSchema["ui:placeholder"] = t("common.form.passwordPlaceholder");
                } else if (isEditValidationMode) {
                    /**
                     * Properties that have resulted in an empty value due to user interaction are resolved to an empty string.
                     * This behaviour explicitly allows a user to clear the value from a property.
                     * @see https://github.com/mozilla-services/react-jsonschema-form#the-case-of-empty-strings
                     */
                    uiSchema["ui:emptyValue"] = "";
                }
            }

            return uiSchema;
        })
    };

    if (isEditValidationMode) {
        schema = removePasswordsFromRequired(schema);
    }

    return (
        <ReactJSONSchemaForm
            FieldTemplate={ VerticalFormFieldTemplate }
            fields={ fields }
            formData={ formData }
            liveValidate
            schema={ schema }
            showErrorList={ false }
            uiSchema={ uiSchema }
            widgets={ widgets }
            { ...restProps }
        />
    );
};

Form.propTypes = {
    formData: PropTypes.objectOf(PropTypes.any),
    schema: PropTypes.objectOf(PropTypes.any),
    validationMode: PropTypes.oneOf(["create", "edit"]).isRequired
};

export default Form;
