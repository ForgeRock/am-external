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

import { Button, Clearfix, Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import focusableAttributes from "components/form/hocs/focusableAttributes";
import FocusContext from "../context/FocusContext";
import Form from "components/form/Form";
import liveValidateOnSubmit from "components/form/hocs/liveValidateOnSubmit";

const EnhancedForm = liveValidateOnSubmit(focusableAttributes(Form));

class EditEntityForm extends Component {
    static propTypes = {
        formData: PropTypes.objectOf(PropTypes.any).isRequired,
        onSave: PropTypes.func.isRequired,
        schema: PropTypes.objectOf(PropTypes.any).isRequired
    };

    state = {
        formData: this.props.formData
    };

    handleChange = ({ formData }) => {
        this.setState({ formData });
    };

    handleSave = () => {
        this.form.submit();
    };

    handleSubmit = ({ formData }) => {
        this.props.onSave(formData);
    };

    setFormRef = (element) => {
        this.form = element;
    };

    render () {
        return (
            <Panel className="fr-panel-tab clearfix">
                <Panel.Body>
                    <FocusContext.Consumer>
                        {({ handleFocusComplete, path }) => (
                            <EnhancedForm
                                editValidationMode
                                focusPath={ path }
                                formData={ this.state.formData }
                                horizontal
                                onChange={ this.handleChange }
                                onFocusComplete={ handleFocusComplete }
                                onSubmit={ this.handleSubmit }
                                ref={ this.setFormRef }
                                schema={ this.props.schema }
                            />
                        )}
                    </FocusContext.Consumer>
                </Panel.Body>
                <Panel.Footer>
                    <Clearfix>
                        <div className="pull-right">
                            <div className="am-btn-action-group">
                                <Button bsStyle="primary" onClick={ this.handleSave }>
                                    { t("common.form.saveChanges") }
                                </Button>
                            </div>
                        </div>
                    </Clearfix>
                </Panel.Footer>
            </Panel>
        );
    }
}

export default EditEntityForm;
