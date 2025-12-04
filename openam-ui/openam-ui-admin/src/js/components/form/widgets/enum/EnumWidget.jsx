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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

import PropTypes from "prop-types";
import React, { Component } from "react";

import SingleSelect from "components/inputs/select/SingleSelect";

class EnumWidget extends Component {
    handleChange = (option, { action }) => {
        if (action === "select-option" || action === "remove-value") {
            this.props.onChange(option.value);
        }
    };

    render () {
        const { autofocus, disabled, options, value, id, readonly } = this.props;
        const selectedValue = options.enumOptions.filter((option) => option.value === value)[0] || null;
        return (
            <SingleSelect
                autoFocus={ autofocus } // eslint-disable-line jsx-a11y/no-autofocus
                backspaceRemovesValue={ false }
                disabled={ disabled }
                inputId={ id }
                isClearable={ false }
                onChange={ this.handleChange }
                options={ options.enumOptions }
                readonly={ readonly }
                value={ selectedValue }
            />
        );
    }
}

EnumWidget.propTypes = {
    autofocus: PropTypes.bool,
    disabled: PropTypes.bool,
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func,
    options: PropTypes.shape({
        enumOptions: PropTypes.arrayOf(PropTypes.shape({
            label: PropTypes.string,
            value: PropTypes.string
        })).isRequired
    }).isRequired,
    readonly: PropTypes.bool,
    value: PropTypes.string
};

export default EnumWidget;
