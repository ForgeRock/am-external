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
 * Copyright 2017 ForgeRock AS.
 */

import { findKey, get, map, mapValues, sortBy } from "lodash";
import { t } from "i18next";
import React, { PropTypes } from "react";
import ReactJSONSchemaForm from "react-jsonschema-form";

import fields from "components/form/fields/index";
import widgets from "components/form/widgets/index";
import VerticalFormFieldTemplate from "components/form/fields/VerticalFormFieldTemplate";

/**
 * Wrapping component around the react-jsonschema-form library.
 * @module components/form/Form
 * @param {Object} props Component properties.
 * @param {Object} props.schema Schema for the data.
 * @returns {ReactElement} Wrapping component around react-jsonschema-form
 * @see https://github.com/mozilla-services/react-jsonschema-form
 */
const Form = (props) => {
    const orderedProperties = sortBy(props.schema.properties, "propertyOrder");

    const uiSchema = {
        "ui:order": map(orderedProperties, (property) => findKey(props.schema.properties, property)),
        ...mapValues(props.schema.properties, (property) => {
            const uiSchema = {};

            if (property.type === "boolean") {
                uiSchema["ui:widget"] = "TitatoggleWidget";
            } else if (property.type === "object") {
                uiSchema["ui:field"] = "KeyValueField";
            }

            if (get(property, "format") === "password") {
                uiSchema["ui:placeholder"] = t("common.form.passwordPlaceholder");
            }

            return uiSchema;
        })
    };

    return (
        <ReactJSONSchemaForm
            FieldTemplate={ VerticalFormFieldTemplate }
            fields={ fields }
            liveValidate
            showErrorList={ false }
            uiSchema={ uiSchema }
            widgets={ widgets }
            { ...props }
        />
    );
};

Form.propTypes = {
    schema: PropTypes.objectOf(PropTypes.any)
};

export default Form;
