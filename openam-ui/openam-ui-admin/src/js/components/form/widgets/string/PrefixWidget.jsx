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
 * Copyright 2020 ForgeRock AS.
 */

import PropTypes from "prop-types";
import React, { Component } from "react";

import { FormControl, InputGroup } from "react-bootstrap";

class PrefixWidget extends Component {
    handleChange = (event) => {
        this.props.onChange(event.target.value);
    };

    render () {
        const { disabled, value, id, readonly, schema } = this.props;
        return (
            <InputGroup>
                <InputGroup.Addon>{ schema.prefix }</InputGroup.Addon>
                <FormControl
                    disabled={ disabled }
                    id={ id }
                    onChange={ this.handleChange }
                    readOnly={ readonly }
                    type="text"
                    value={ value }
                />
            </InputGroup>
        );
    }
}

PrefixWidget.propTypes = {
    disabled: PropTypes.bool,
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    readonly: PropTypes.bool,
    schema: PropTypes.shape({
        prefix: PropTypes.string.isRequired
    }).isRequired,
    value: PropTypes.string
};

export default PrefixWidget;
