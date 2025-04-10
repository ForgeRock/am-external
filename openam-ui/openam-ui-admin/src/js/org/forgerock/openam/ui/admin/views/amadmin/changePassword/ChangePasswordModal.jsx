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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { Button, Clearfix, Modal } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import ErrorBoundary from "components/ErrorBoundary";
import Form from "components/form/Form";
import liveValidateOnSubmit from "components/form/hocs/liveValidateOnSubmit";

const EnhancedForm = liveValidateOnSubmit(Form);

class ChangePasswordModal extends Component {
    state = { formData: {} };

    handleSaveClick = () => {
        this.form.submit();
    };

    handleOnChange = ({ formData }) => {
        this.setState({ formData });
    };

    handleOnSubmit = ({ formData }) => {
        this.props.onSave(formData);
    };

    setFormRef = (element) => {
        this.form = element;
    };

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
                <ErrorBoundary>
                    <Modal.Body>
                        <EnhancedForm
                            formData={ this.state.formData }
                            onChange={ this.handleOnChange }
                            onSubmit={ this.handleOnSubmit }
                            ref={ this.setFormRef }
                            schema={ this.props.schema }
                            validate={ this.props.validate }
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
                </ErrorBoundary>
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
