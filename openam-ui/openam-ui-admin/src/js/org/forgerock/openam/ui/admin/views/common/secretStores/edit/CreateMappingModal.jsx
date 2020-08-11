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
 * Copyright 2018-2019 ForgeRock AS.
 */

import { Button, Clearfix, Modal } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import CustomAliasesField from "./fields/CustomAliasesField";
import ErrorBoundary from "components/ErrorBoundary";
import Form from "components/form/Form";
import liveValidateOnSubmit from "components/form/hocs/liveValidateOnSubmit";

const EnhancedForm = liveValidateOnSubmit(Form);

class CreateMappingModal extends Component {
    constructor (props) {
        super(props);

        this.state = {
            formData: this.props.template
        };
    }

    handleOnCreate = () => {
        this.form.submit();
    };

    handleOnChange = ({ formData }) => {
        this.setState({ formData });
    };

    handleOnSubmit = ({ formData }) => {
        this.props.onCreate(formData);
    };

    setFormRef = (element) => {
        this.form = element;
    };

    render () {
        return (
            <Modal backdrop="static" onHide={ this.props.onClose } show={ this.props.show } >
                <Modal.Header closeButton>
                    <Modal.Title>{ t ("console.secretStores.edit.mappings.new.title") }</Modal.Title>
                </Modal.Header>
                <ErrorBoundary>
                    <Modal.Body>
                        <EnhancedForm
                            formData={ this.state.formData }
                            onChange={ this.handleOnChange }
                            onSubmit={ this.handleOnSubmit }
                            ref={ this.setFormRef }
                            schema={ this.props.schema }
                            uiSchema={ { "aliases": { "ui:field": CustomAliasesField } } }
                        />
                    </Modal.Body>
                    <Modal.Footer>
                        <Clearfix>
                            <div className="pull-right">
                                <div className="am-btn-action-group">
                                    <Button onClick={ this.props.onClose } >
                                        { t ("common.form.cancel") }
                                    </Button>
                                    <Button bsStyle="primary" onClick={ this.handleOnCreate }>
                                        { t("common.form.create") }
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

CreateMappingModal.propTypes = {
    onClose: PropTypes.func.isRequired,
    onCreate: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.any).isRequired,
    show: PropTypes.bool.isRequired,
    template: PropTypes.objectOf(PropTypes.any).isRequired
};

export default CreateMappingModal;
