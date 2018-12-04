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

import PropTypes from "prop-types";
import React, { Component } from "react";

import ToggleSwitch from "components/form/inputs/boolean/ToggleSwitch";

class ToggleSwitchWidget extends Component {
    constructor (props) {
        super(props);

        this.handleOnChange = this.handleOnChange.bind(this);
    }

    handleOnChange (event) {
        this.props.onChange(event.target.checked);
    }

    render () {
        return (
            <ToggleSwitch
                checked={ this.props.value }
                id={ this.props.id }
                onChange={ this.handleOnChange }
            />
        );
    }
}

ToggleSwitchWidget.propTypes = {
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.bool.isRequired
};

export default ToggleSwitchWidget;
