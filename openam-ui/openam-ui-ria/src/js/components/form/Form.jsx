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

import { findKey, map, mapValues, omit, sortBy } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component, Fragment } from "react";
import ReactJSONSchemaForm from "react-jsonschema-form";

import convertToDraft4PlusRequired from "./schema/convertToDraft4PlusRequired";
import fields from "components/form/fields";
import isPassword from "./schema/isPassword";
import removePasswordsFromRequired from "./schema/removePasswordsFromRequired";
import VerticalFormFieldTemplate from "components/form/templates/VerticalFormFieldTemplate";
import widgets from "components/form/widgets";

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
 *     a password that may already exist.
 * - String properties which have no input will default to `""` (empty string) rather than `undefined`.
 *   - When providing values for a string property, `undefined` signals no value and triggers
 *     validation, whereas `""` (empty string) does not.
 *   - For forms that ***create*** objects, the first behaviour is desirable so the user is prompted to enter
 *     values for all required properties.
 *   - For forms that ***edit*** objects, the user may wish to set a string property to `""` (empty string),
 *     making the second behaviour desirable.
 *
 * # Always Validate
 * Unlike in `"edit"` mode where `"live validation" is always enabled, in `"create"` mode `"live validation"` is
 * enabled only after an attempt is made to submit the form. This behaviour necessary to prevent validation errors being
 * displayed on an untouched form.
 *
 * The `alwaysValidate` parameter allows this behaviour to be overridden and forces the form to live validate it's data
 * from the beginning. This is required in views where there is no submit button (ie tree node properties).
 *
 * @module components/form/Form
 * @param {Object} props Component properties.
 * @param {Object} props.formData JSON object containing the form data.
 * @param {Object} props.schema JSON Schema.
 * @param {string} props.validationMode=create Validation mode. Possible values are `create` or `edit`.
 * @param {boolean} props.alwaysValidate=true Forces the form to begin validation before form submission.
 * @returns {ReactElement} Form component for JSON Schema.
 * @see https://github.com/mozilla-services/react-jsonschema-form
 */
class Form extends Component {
    constructor (props) {
        super(props);

        const isEditValidationMode = this.props.validationMode === "edit";
        this.state = {
            validationEnabled: isEditValidationMode
        };

        this.handleOnSubmit = this.handleOnSubmit.bind(this);
        this.setSubmitButtonRef = this.setSubmitButtonRef.bind(this);
        this.transformErrors = this.transformErrors.bind(this);
    }

    componentDidCatch () {
        this.setState({ errorCaught: true });
    }

    handleOnSubmit ({ formData, errors }) {
        if (this.state.validationEnabled) {
            this.props.onSubmit({ formData, errors });
        } else {
            this.setState({ validationEnabled: true }, () => {
                // resubmit with validation enabled
                this.submitButton.click();
            });
        }
    }

    setSubmitButtonRef (element) {
        this.submitButton = element;
    }

    submit () {
        this.submitButton.click();
    }

    transformErrors (errors) {
        // TODO: After the update to 4.0.0 lodash version use the upperFirst function instead.
        const capitalizeFirstLetter = (message) => message && message.charAt(0).toUpperCase() + message.slice(1);

        return errors.map((error) => {
            switch (error.name) {
                case "required": error.message = t("common.form.validation.required");
                    break;
                case "minItems":
                    error.message = t("console.common.validation.minItems", { count: error.params.limit });
                    break;
                default: error.message = capitalizeFirstLetter(error.message);
            }

            return error;
        });
    }

    render () {
        if (this.state.errorCaught) {
            return (
                <Fragment>
                    <p className="text-center text-primary"><i className="fa fa-frown-o fa-4x" /></p>
                    <h4 className="text-center">{ t("console.common.error.oops") }</h4>
                </Fragment>
            );
        }

        const { schema, validationMode, alwaysValidate, ...props } = this.props;
        const restProps = omit(props, "onSubmit", "uiSchema");
        const isEditValidationMode = validationMode === "edit";
        const noValidate = !(alwaysValidate || this.state.validationEnabled);
        const orderedProperties = sortBy(this.props.schema.properties, "propertyOrder");

        const uiSchema = {
            "ui:order": map(orderedProperties, (property) => findKey(this.props.schema.properties, property)),
            ...mapValues(this.props.schema.properties, (property) => {
                const uiSchema = {};
                if (property.type === "boolean") {
                    uiSchema["ui:widget"] = "ToggleSwitchWidget";
                } else if (property.type === "object") {
                    uiSchema["ui:field"] = "KeyValueField";
                } else if (property.type === "string") {
                    if (isPassword(property)) {
                        uiSchema["ui:widget"] = "password";
                        uiSchema["ui:placeholder"] = t("common.form.passwordPlaceholder");
                    } else if (property.format === "date-time") {
                        uiSchema["ui:field"] = "DateTimeField";
                    } else if (property.enum) {
                        uiSchema["ui:widget"] = "EnumWidget";
                    } else if (isEditValidationMode) {
                        /**
                         * Properties that have resulted in an empty value due to user interaction are resolved to an empty string.
                         * This behaviour explicitly allows a user to clear the value from a property.
                         * @see https://github.com/mozilla-services/react-jsonschema-form#the-case-of-empty-strings
                         */
                        uiSchema["ui:emptyValue"] = "";
                    }
                } else if (property.type === "array") {
                    if (property.items && property.items.enum) {
                        uiSchema["ui:field"] = "EnumArrayField";
                    } else {
                        uiSchema["ui:field"] = "CreatableArrayField";
                    }
                }

                return uiSchema;
            }),
            ...this.props.uiSchema
        };

        // JSON Schema Transforms
        let schemaProp = convertToDraft4PlusRequired(schema);
        if (isEditValidationMode) {
            schemaProp = removePasswordsFromRequired(schemaProp);
        }

        return (
            <ReactJSONSchemaForm
                FieldTemplate={ VerticalFormFieldTemplate }
                fields={ fields }
                liveValidate
                noHtml5Validate
                noValidate={ noValidate }
                onSubmit={ this.handleOnSubmit }
                schema={ schemaProp }
                showErrorList={ false }
                transformErrors={ this.transformErrors }
                uiSchema={ uiSchema }
                widgets={ widgets }
                { ...restProps }
            >
                <button className="hidden" ref={ this.setSubmitButtonRef } type="submit" />
            </ReactJSONSchemaForm>
        );
    }
}

Form.defaultProps = {
    alwaysValidate: false
};

Form.propTypes = {
    alwaysValidate: PropTypes.bool,
    formData: PropTypes.objectOf(PropTypes.any),
    onSubmit: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.any),
    uiSchema: PropTypes.objectOf(PropTypes.any),
    validationMode: PropTypes.oneOf(["create", "edit"]).isRequired
};

export default Form;
