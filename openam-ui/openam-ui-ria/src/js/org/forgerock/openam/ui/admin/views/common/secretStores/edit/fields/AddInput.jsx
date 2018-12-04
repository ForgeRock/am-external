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

import { Button, ControlLabel, FormControl, InputGroup } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component, Fragment } from "react";

class AddInput extends Component {
    constructor (props) {
        super(props);

        this.state = {
            value: ""
        };

        this.handleAdd = this.handleAdd.bind(this);
        this.handleKeyPress = this.handleKeyPress.bind(this);
        this.handleOnChange = this.handleOnChange.bind(this);
    }

    handleAdd () {
        const added = this.props.onAdd(this.state.value.trim());

        if (added) {
            this.setState({ value: "" });
        }
    }

    handleKeyPress (event) {
        if (event.key === "Enter") {
            // Prevent form submission
            event.preventDefault();
            this.handleAdd();
        }
    }

    handleOnChange (event) {
        this.setState({ value: event.currentTarget.value });
    }

    render () {
        return (
            <Fragment>
                <ControlLabel srOnly>{ t("console.secretStores.edit.mappings.form.aliasField.label") }</ControlLabel>
                <InputGroup>
                    <FormControl
                        id={ this.props.id }
                        onChange={ this.handleOnChange }
                        onKeyPress={ this.handleKeyPress }
                        placeholder={ t("console.secretStores.edit.mappings.form.aliasField.placeholder") }
                        value={ this.state.value }
                    />
                    <InputGroup.Button>
                        <Button
                            className="fr-btn-secondary"
                            onClick={ this.handleAdd }
                            title={ t("console.secretStores.edit.mappings.form.aliasField.label") }
                        >
                            <i className="fa fa-plus" />
                        </Button>
                    </InputGroup.Button>
                </InputGroup>
            </Fragment>
        );
    }
}

AddInput.propTypes = {
    id: PropTypes.string,
    onAdd: PropTypes.func.isRequired
};

export default AddInput;
