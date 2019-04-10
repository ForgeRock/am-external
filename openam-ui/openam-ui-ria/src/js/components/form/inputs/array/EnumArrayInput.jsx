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
 * Copyright 2018 ForgeRock AS.
 */
import { map } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";
import Select from "react-select";

import mapEnumNames from "../mapEnumNames";

class EnumArrayInput extends Component {
    constructor (props) {
        super(props);

        this.state = {
            value: this.props.value
        };

        this.handleOnBlur = this.handleOnBlur.bind(this);
        this.handleOnChange = this.handleOnChange.bind(this);
    }

    handleOnBlur () {
        this.props.onChange(this.state.value);
    }

    handleOnChange (value) {
        this.setState({
            value: map(value, "key")
        }, () => {
            if (this.props.updateOnChange) {
                this.props.onChange(this.state.value);
            }
        });
    }

    render () {
        const { autoFocus, id, placeholder, schema } = this.props;
        const options = mapEnumNames(schema.items);

        return (
            <Select
                autoFocus={ autoFocus } // eslint-disable-line jsx-a11y/no-autofocus
                clearable={ false }
                closeOnSelect={ false }
                id={ id }
                labelKey="value"
                multi
                onBlur={ this.handleOnBlur }
                onChange={ this.handleOnChange }
                options={ options }
                placeholder={ placeholder }
                stayOpen
                value={ this.state.value }
                valueKey="key"
            />
        );
    }
}

EnumArrayInput.defaultProps = {
    placeholder: t("common.form.select"),
    updateOnChange: true
};

EnumArrayInput.propTypes = {
    autoFocus: PropTypes.bool,
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    placeholder: PropTypes.string,
    schema: PropTypes.shape({
        "items": PropTypes.arrayOf(PropTypes.string)
    }).isRequired,
    updateOnChange: PropTypes.bool,
    value: PropTypes.arrayOf(PropTypes.string)
};

export default EnumArrayInput;
