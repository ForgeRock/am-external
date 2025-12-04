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
 * Copyright 2025 Ping Identity Corporation.
 */

import PropTypes from "prop-types";
import React, { Component } from "react";
import { Button, Clearfix } from "react-bootstrap";
import { t } from "i18next";

import ErrorBoundary from "components/ErrorBoundary";
import Form from "components/form/Form";
import liveValidateOnSubmit from "components/form/hocs/liveValidateOnSubmit";

const EnhancedForm = liveValidateOnSubmit(Form);

class ImportNodes extends Component {
    state = { formData: {} };

    handleImportClick = () => {
        this.form.submit();
    };

    handleOnChange = ({ formData }) => {
        this.setState({ formData });
    };

    handleOnSubmit = ({ formData }) => {
        this.props.onImport(formData);
    };

    setFormRef = (element) => {
        this.form = element;
    };

    render () {
        return (
            <ErrorBoundary>
                <EnhancedForm
                    formData={ this.state.formData }
                    horizontal
                    onChange={ this.handleOnChange }
                    onSubmit={ this.handleOnSubmit }
                    ref={ this.setFormRef }
                    schema={ this.props.schema }
                />
                <Clearfix>
                    <div className="pull-right">
                        <div className="am-btn-action-group">
                            <Button href={ this.props.backLink } >
                                { t ("common.form.cancel") }
                            </Button>
                            <Button
                                bsStyle="primary"
                                disabled={ this.props.isImportDisabled }
                                onClick={ this.handleImportClick }
                            >
                                { t("common.form.import") }
                            </Button>
                        </div>
                    </div>
                </Clearfix>
            </ErrorBoundary>
        );
    }
}

ImportNodes.propTypes = {
    backLink: PropTypes.string.isRequired,
    isImportDisabled: PropTypes.bool.isRequired,
    onImport: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.any).isRequired
};

export default ImportNodes;
