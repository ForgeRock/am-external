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
 * Copyright 2019 ForgeRock AS.
 */

import { Button, Well } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";
import Form from "components/form/Form";

class ObjectInputControl extends Component {
    static propTypes = {
        disabled: PropTypes.bool,
        id: PropTypes.string.isRequired,
        isEditing: PropTypes.bool,
        itemSchema: PropTypes.shape({
            properties: PropTypes.objectOf(PropTypes.any),
            type: PropTypes.string.isRequired
        }).isRequired,
        onAdd: PropTypes.func.isRequired,
        onCancel: PropTypes.func.isRequired,
        onChange: PropTypes.func.isRequired,
        onEdited: PropTypes.func.isRequired,
        placeholder: PropTypes.string,
        value: PropTypes.oneOfType([
            PropTypes.string,
            PropTypes.number,
            PropTypes.objectOf(PropTypes.any)
        ])
    };

    handleAdd = () => {
        this.props.onAdd(this.props.value);
    };

    handleEdit = () => {
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
        const { disabled, isEditing, id, onCancel, placeholder = t("common.form.addValue") } = this.props;

        const buttons = isEditing
            ? (
                <div className="float-right" style={ { marginRight: "-15px" } }>
                    <Button
                        className="fr-btn-secondary"
                        onClick={ onCancel }
                        title={ t("common.form.cancel") }
                    >
                        { t("common.form.cancel") }
                    </Button>
                    { " " }
                    <Button
                        bsStyle="primary"
                        onClick={ this.handleEdit }
                        title={ t("common.form.update") }
                    >
                        { t("common.form.update") }
                    </Button>
                </div>
            )
            : (
                <Button
                    className="float-right fr-btn-secondary"
                    disabled={ disabled }
                    onClick={ this.handleAdd }
                    style={ { marginRight: "-15px" } }
                    title={ placeholder }
                >
                    { t("common.form.add") }
                </Button>
            );

        // disableCollapsible is used to prevent the Form from displaying this object within a collapsible panel
        const uiSchema = { "ui:options": { disableCollapsible: true } };

        return (
            <Well style={ { background: "none", padding: "15px 30px" } }>
                <div className="clearfix">
                    <Form
                        formData={ this.props.value }
                        idPrefix={ id }
                        onChange={ this.handleFormChange }
                        schema={ this.props.itemSchema }
                        tagName="div"
                        uiSchema={ uiSchema }
                    />
                    { buttons }
                </div>
            </Well>
        );
    }
}

export default ObjectInputControl;
