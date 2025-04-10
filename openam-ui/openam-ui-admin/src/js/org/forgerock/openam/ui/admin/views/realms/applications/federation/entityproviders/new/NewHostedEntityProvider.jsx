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

import PropTypes from "prop-types";
import React, { Component } from "react";

import ErrorBoundary from "components/ErrorBoundary";
import CreateFooter from "org/forgerock/openam/ui/admin/views/realms/common/CreateFooter";
import Form from "components/form/Form";
import liveValidateOnSubmit from "components/form/hocs/liveValidateOnSubmit";
import Router from "org/forgerock/commons/ui/common/main/Router";

const EnhancedForm = liveValidateOnSubmit(Form);

class NewHostedEntityProvider extends Component {
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
            <ErrorBoundary>
                <EnhancedForm
                    formData={ this.state.formData }
                    horizontal
                    onChange={ this.handleOnChange }
                    onSubmit={ this.handleOnSubmit }
                    ref={ this.setFormRef }
                    schema={ this.props.schema }
                    validate={ this.props.validate }
                />
                <CreateFooter
                    backRoute={ Router.configuration.routes.realmsApplicationsFederationEntityProviders }
                    disabled={ this.props.isCreateDisabled }
                    onCreateClick={ this.handleSaveClick }
                />
            </ErrorBoundary>
        );
    }
}

NewHostedEntityProvider.propTypes = {
    isCreateDisabled: PropTypes.bool.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.any).isRequired,
    validate: PropTypes.func.isRequired
};

export default NewHostedEntityProvider;
