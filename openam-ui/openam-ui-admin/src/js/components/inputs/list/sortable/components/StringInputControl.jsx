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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

import { Button, FormControl, InputGroup } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component, Fragment } from "react";

class StringInputControl extends Component {
    static propTypes = {
        disabled: PropTypes.bool,
        id: PropTypes.string.isRequired,
        isEditing: PropTypes.bool,
        itemSchema: PropTypes.shape({
            minLength: PropTypes.number,
            type: PropTypes.string.isRequired
        }).isRequired,
        onAdd: PropTypes.func.isRequired,
        onCancel: PropTypes.func.isRequired,
        onChange: PropTypes.func.isRequired,
        onEdited: PropTypes.func.isRequired,
        placeholder: PropTypes.string,
        value: PropTypes.oneOfType([
            PropTypes.string,
            PropTypes.number
        ])
    };

    static defaultProps = {
        value: ""
    };

    isValid = (value) => {
        if (value.trim() === "" || value.length < this.props.itemSchema.minLength) {
            return false;
        }

        return true;
    };

    handleAdd = () => {
        const { onAdd, value } = this.props;

        if (!this.isValid(value)) {
            return;
        }

        const added = onAdd(value.trim());

        if (added) {
            const input = document.getElementById(this.props.id);
            if (input) {
                input.focus();
            }
        }
    };

    handleEdit = () => {
        if (!this.isValid(this.props.value)) {
            return;
        }

        this.props.onEdited(this.props.value);
    };

    handleKeyPress = (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            if (this.props.isEditing) {
                this.handleEdit();
            } else {
                this.handleAdd();
            }
        }
    };

    handleOnChange = (event) => {
        this.props.onChange(event.currentTarget.value);
    };

    handleFormChange = ({ formData }) => {
        this.props.onChange(formData);
    };

    render () {
        const { disabled, id, isEditing, onCancel, value, placeholder = t("common.form.addValue") } = this.props;

        const buttons = isEditing
            ? (
                <Fragment>
                    <Button
                        className="fr-btn-secondary"
                        onClick={ onCancel }
                        title={ t("common.form.cancel") }
                    >
                        { t("common.form.cancel") }
                    </Button>
                    <Button
                        bsStyle="primary"
                        onClick={ this.handleEdit }
                        title={ t("common.form.update") }
                    >
                        { t("common.form.update") }
                    </Button>
                </Fragment>
            )
            : (
                <Button
                    className="fr-btn-secondary"
                    disabled={ disabled }
                    onClick={ this.handleAdd }
                    title={ placeholder }
                >
                    { t("common.form.add") }
                </Button>
            );

        return (
            <InputGroup>
                <FormControl
                    id={ id }
                    onChange={ this.handleOnChange }
                    onKeyPress={ this.handleKeyPress }
                    placeholder={ placeholder }
                    readOnly={ disabled }
                    value={ value }
                />
                <InputGroup.Button>
                    { buttons }
                </InputGroup.Button>
            </InputGroup>
        );
    }
}

export default StringInputControl;
