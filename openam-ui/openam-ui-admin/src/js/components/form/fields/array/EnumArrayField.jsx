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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import PropTypes from "prop-types";
import React, { Component } from "react";

import MultiSelect from "components/inputs/select/MultiSelect";

const mapEnumNames = (items) => {
    const hasEnumNames = items.enumNames;
    return items.enum.map((label, index) => {
        return { label, value: hasEnumNames ? items.enumNames[index] : label };
    });
};

class EnumArrayField extends Component {
    handleChange = (data, { action }) => {
        if (action === "select-option" || action === "remove-value") {
            this.props.onChange(data.map(({ label }) => label));
        }
    };

    render () {
        const { autofocus, disabled, formData, readonly, idSchema, schema } = this.props;
        const options = mapEnumNames(schema.items);
        const value = formData ? formData.map((value) => ({ label: value, value })) : undefined;

        return (
            <MultiSelect
                autoFocus={ autofocus } // eslint-disable-line jsx-a11y/no-autofocus
                disabled={ disabled }
                inputId={ idSchema.$id }
                isClearable={ false }
                onChange={ this.handleChange }
                options={ options }
                readonly={ readonly }
                value={ value }
            />
        );
    }
}

EnumArrayField.propTypes = {
    autofocus: PropTypes.bool,
    disabled: PropTypes.bool,
    formData: PropTypes.arrayOf(PropTypes.string).isRequired,
    idSchema: PropTypes.shape({
        $id: PropTypes.string.isRequired
    }),
    onChange: PropTypes.func,
    readonly: PropTypes.bool,
    schema: PropTypes.shape({
        items: PropTypes.objectOf(PropTypes.any).isRequired
    }).isRequired
};

export default EnumArrayField;
