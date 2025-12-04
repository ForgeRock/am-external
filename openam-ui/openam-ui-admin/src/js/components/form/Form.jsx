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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

import { mapValues, merge, omit, upperFirst } from "lodash";
import { t } from "i18next";
import classnames from "classnames";
import PropTypes from "prop-types";
import React, { Component } from "react";
import ReactJSONSchemaForm from "react-jsonschema-form";

import convertToDraft4PlusRequired from "./schema/convertToDraft4PlusRequired";
import fields from "components/form/fields";
import FormFieldTemplate from "components/form/templates/FormFieldTemplate";
import removePasswordsFromRequired from "./schema/removePasswordsFromRequired";
import uiSchema from "./uiSchema";
import widgets from "components/form/widgets";
import {
    convertPlaceholderSchemaToReadOnly,
    flattenPlaceholder,
    containsPlaceholder,
    revertPlaceholdersToOriginalValue
} from "org/forgerock/commons/ui/common/util/PlaceholderUtils";

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
 * `editValidationMode` `true` uses standard validation, plus the following behaviours:
 * - Password properties that are `required`, are removed from the list of `required` properties.
 *   - AM never returns passwords from it's API for security reasons.
 *   - A form that is editing an existing object will display validation errors for required
 *     password fields.
 *   - An empty password property will not update the password, so the removal of the `required`
 *     validation allows editing of an existing object without the user being prompted to supply
 *     a password that may already exist.
 * - String properties which have no input will default to `""` (empty string) rather than `undefined`.
 *   - When providing values for a string property, `undefined` signals no value and triggers
 *     validation, whereas `""` (empty string) does not.
 *   - For forms that ***create*** objects, the first behaviour is desirable so the user is prompted to enter
 *     values for all required properties.
 *   - For forms that ***edit*** objects, the user may wish to set a string property to `""` (empty string),
 *     making the second behaviour desirable.
 * @module components/form/Form
 * @see https://github.com/mozilla-services/react-jsonschema-form
 */
class Form extends Component {
    static propTypes = {
        editValidationMode: PropTypes.bool,
        formData: PropTypes.objectOf(PropTypes.any),
        horizontal: PropTypes.bool,
        schema: PropTypes.objectOf(PropTypes.any),
        uiSchema: PropTypes.objectOf(PropTypes.any)
    };

    static defaultProps = {
        editValidationMode: false,
        horizontal: false
    };

    fieldTemplate = (props) => (<FormFieldTemplate { ...props } horizontal={ this.props.horizontal } />);

    setSubmitButtonRef = (element) => {
        this.submitButton = element;
    };

    submit () {
        this.submitButton.click();
    }

    transformErrors = (errors) => {
        return errors.map((error) => {
            switch (error.name) {
                case "required": error.message = t("common.form.validation.required");
                    break;
                case "minItems":
                    error.message = t("console.common.validation.minItems", { count: error.params.limit });
                    break;
                default: error.message = upperFirst(error.message);
            }

            return error;
        });
    };

    render () {
        const { editValidationMode, schema, formData, ...props } = this.props;
        const restProps = omit(props, ["uiSchema", "onChange"]);

        // JSON Schema Transforms
        let schemaProp = convertToDraft4PlusRequired(schema);
        if (editValidationMode) {
            schemaProp = removePasswordsFromRequired(schemaProp);
        }

        /**
          * AM's API returns `null` for values that do no exist. `null` is a special type in JavaScript
          * and triggers type validation errors, thus, all `null` values are transformed to `undefined`
          * to ensure type validation errors are not displayed.
          *
          * Remove this tranform when the API returns `undefined` by default.
          * @see OPENAM-16592
          */
        let formDataNullRemoved = mapValues(formData, (value) => { return value === null ? undefined : value; });
        schemaProp = convertPlaceholderSchemaToReadOnly(formDataNullRemoved, schemaProp);
        formDataNullRemoved = flattenPlaceholder({ ...formDataNullRemoved });
        let placeholderFound = false;
        const keys = Object.keys(formDataNullRemoved);
        for (let i = 0; i < keys.length; i++) {
            if (containsPlaceholder(formDataNullRemoved[keys[i]])) {
                placeholderFound = true;
                break;
            }
        }

        // This has to be handled instead of on submit as on some forms
        // (tree node properties), they get submitted by a parent component and
        // thus the forms handleSubmit never gets called
        const onChangeWrapper = (jsonForm) => {
            if (placeholderFound) {
                const form = { ...jsonForm };
                form.formData = revertPlaceholdersToOriginalValue(form.formData, schemaProp);
                props.onChange(form);
            } else {
                props.onChange(jsonForm);
            }
        };

        return (
            <ReactJSONSchemaForm
                className={
                    classnames({
                        "form-horizontal": this.props.horizontal
                    })
                }
                fields={ fields }
                FieldTemplate={ this.fieldTemplate }
                formData={ formDataNullRemoved }
                noHtml5Validate
                onChange={ props.onChange && onChangeWrapper } // eslint-disable-line
                schema={ schemaProp }
                showErrorList={ false }
                transformErrors={ this.transformErrors }
                uiSchema={
                    placeholderFound
                        ? uiSchema(schemaProp, editValidationMode)
                        : merge(uiSchema(schema, editValidationMode), this.props.uiSchema) }
                widgets={ widgets }
                { ...restProps }
            >
                <button className="hidden" ref={ this.setSubmitButtonRef } type="submit" />
            </ReactJSONSchemaForm>
        );
    }
}

export default Form;
