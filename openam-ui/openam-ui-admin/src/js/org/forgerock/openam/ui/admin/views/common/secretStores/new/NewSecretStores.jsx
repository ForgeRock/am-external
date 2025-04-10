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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { Col, ControlLabel, Form, FormGroup, Panel } from "react-bootstrap";
import { property } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component, Fragment } from "react";

import CreateFooter from "org/forgerock/openam/ui/admin/views/realms/common/CreateFooter";
import SingleSelect from "components/inputs/select/SingleSelect";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import PageHeader from "components/PageHeader";

class NewSecretStores extends Component {
    UNSAFE_componentWillReceiveProps (nextProps) {
        if (this.props.template !== nextProps.template) {
            if (this.jsonSchemaView) {
                this.jsonSchemaView.destroy();
                this.jsonSchemaView = null;
            }
        }
    }

    componentDidUpdate () {
        if (this.jsonForm && !this.jsonSchemaView && this.props.template) {
            this.jsonSchemaView = new FlatJSONSchemaView({
                hideInheritance: true,
                schema: new JSONSchema(this.props.schema),
                values: new JSONValues(this.props.template),
                showOnlyRequiredAndEmpty: true
            });
            this.jsonForm.appendChild(this.jsonSchemaView.render().el);
        }
    }

    handleCreate = () => {
        if (this.jsonSchemaView.isValid()) {
            this.props.onCreate(this.jsonSchemaView.getData());
        } else {
            Messages.addMessage({
                message: t("common.form.validation.errorsNotSaved"),
                type: Messages.TYPE_DANGER
            });
        }
    };

    setRef = (element) => {
        this.jsonForm = element;
    };

    handleChange = ({ _id }, { action }) => {
        if (action === "select-option" || action === "remove-value") {
            this.props.onTypeChange(_id);
        }
    };

    getOptionLabel = ({ name }) => name;
    getOptionValue = ({ _id }) => _id;

    render () {
        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else {
            content = (
                <Form horizontal>
                    <FormGroupInput
                        isValid={ this.props.isValidId }
                        label={ t("console.secretStores.new.id") }
                        onChange={ this.props.onIdChange }
                        validationMessage={ t("console.common.validation.invalidCharacters") }
                        value={ this.props.id }
                    />
                    <FormGroup controlId="secretStoresType">
                        <Col componentClass={ ControlLabel } sm={ 4 }>
                            { t("console.secretStores.new.type") }
                        </Col>
                        <Col sm={ 6 }>
                            <SingleSelect
                                backspaceRemovesValue={ false }
                                getOptionLabel={ property("name") }
                                getOptionValue={ property("_id") }
                                inputId="secretStoresType"
                                isClearable={ false }
                                onChange={ this.handleChange }
                                options={ this.props.types }
                                value={ this.props.types.filter(({ _id }) => _id === this.props.selectedType) }
                            />
                        </Col>
                    </FormGroup>
                    <div ref={ this.setRef } />
                </Form>
            );
        }

        return (
            <Fragment>
                <PageHeader title={ t("console.secretStores.new.title") } />
                <Panel>
                    <Panel.Body>{ content }</Panel.Body>
                    <Panel.Footer>
                        <CreateFooter
                            backRoute={ this.props.listRoute }
                            disabled={ !this.props.isCreateAllowed }
                            onCreateClick={ this.handleCreate }
                        />
                    </Panel.Footer>
                </Panel>
            </Fragment>
        );
    }
}

NewSecretStores.propTypes = {
    id: PropTypes.string.isRequired,
    isCreateAllowed: PropTypes.bool.isRequired,
    isFetching: PropTypes.bool.isRequired,
    isValidId: PropTypes.bool.isRequired,
    listRoute: PropTypes.objectOf(PropTypes.any).isRequired,
    onCreate: PropTypes.func.isRequired,
    onIdChange: PropTypes.func.isRequired,
    onTypeChange: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.any),
    selectedType: PropTypes.string,
    template: PropTypes.objectOf(PropTypes.any),
    types: PropTypes.arrayOf(PropTypes.shape({
        _id: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired
    })).isRequired
};

export default NewSecretStores;
