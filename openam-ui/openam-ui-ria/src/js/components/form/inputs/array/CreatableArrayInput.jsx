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
import { Creatable } from "react-select";
import { map } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

class CreatableArrayInput extends Component {
    constructor (props) {
        super(props);

        this.handleOnChange = this.handleOnChange.bind(this);
        this.onShouldKeyDownEventCreateNewOption = this.onShouldKeyDownEventCreateNewOption.bind(this);
    }

    handleOnChange (value) {
        this.props.onChange(map(value, "value"));
    }

    onShouldKeyDownEventCreateNewOption ({ keyCode }) {
        switch (keyCode) {
            case 9: // TAB
            case 13: // ENTER
                return true;
        }
        return false;
    }

    render () {
        const { autoFocus, id, value } = this.props;
        const values = map(value, (data) => ({ label: data, value: data }));

        return (
            <Creatable
                autoFocus={ autoFocus } // eslint-disable-line jsx-a11y/no-autofocus
                clearable={ false }
                id={ id }
                multi
                onChange={ this.handleOnChange }
                shouldKeyDownEventCreateNewOption={ this.onShouldKeyDownEventCreateNewOption }
                value={ values }
            />
        );
    }
}

CreatableArrayInput.propTypes = {
    autoFocus: PropTypes.bool,
    id: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.arrayOf(PropTypes.string)
};

export default CreatableArrayInput;
