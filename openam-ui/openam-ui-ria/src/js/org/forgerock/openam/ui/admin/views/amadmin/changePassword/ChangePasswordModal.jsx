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

import { Button, Clearfix, Modal } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import Form from "components/form/Form";

class ChangePasswordModal extends Component {
    constructor (props) {
        super(props);

        this.handleSaveClick = this.handleSaveClick.bind(this);
        this.handleOnChange = this.handleOnChange.bind(this);
        this.handleOnSubmit = this.handleOnSubmit.bind(this);
        this.setFormRef = this.setFormRef.bind(this);

        this.state = {
            formData: { }
        };
    }

    handleSaveClick () {
        this.form.submit();
    }

    handleOnChange ({ formData }) {
        this.setState({ formData });
    }

    handleOnSubmit ({ formData }) {
        this.props.onSave(formData);
    }

    setFormRef (element) {
        this.form = element;
    }

    render () {
        return (
            <Modal
                backdrop="static"
                onExited={ this.props.onExited }
                onHide={ this.props.onClose }
                show={ this.props.show }
            >
                <Modal.Header closeButton={ this.props.isFormEnabled }>
                    <Modal.Title>{ t("common.user.changePassword") }</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form
                        formData={ this.state.formData }
                        onChange={ this.handleOnChange }
                        onSubmit={ this.handleOnSubmit }
                        ref={ this.setFormRef }
                        schema={ this.props.schema }
                        validate={ this.props.validate }
                        validationMode="create"
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Clearfix>
                        <div className="pull-right">
                            <div className="am-btn-action-group">
                                <Button
                                    disabled={ !this.props.isFormEnabled }
                                    onClick={ this.props.onClose }
                                >
                                    { t("common.form.cancel") }
                                </Button>
                                <Button
                                    bsStyle="primary"
                                    disabled={ !this.props.isFormEnabled }
                                    onClick={ this.handleSaveClick }
                                >
                                    { t("common.form.save") }
                                </Button>
                            </div>
                        </div>
                    </Clearfix>
                </Modal.Footer>
            </Modal>
        );
    }
}

ChangePasswordModal.propTypes = {
    isFormEnabled: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    onExited: PropTypes.func.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.any).isRequired,
    show: PropTypes.bool.isRequired,
    validate: PropTypes.func.isRequired
};

export default ChangePasswordModal;
