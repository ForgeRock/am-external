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

import { getCreatableTypes } from "org/forgerock/openam/ui/admin/services/realm/DataStoresService";
import AsyncSingleSelect from "components/inputs/select/AsyncSingleSelect";
import CreateFooter from "org/forgerock/openam/ui/admin/views/realms/common/CreateFooter";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";

class NewDataStore extends Component {
    static fetchTypes () {
        return getCreatableTypes().then(({ result }) => {
            return result;
        });
    }

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

    handleChange = ({ _id }, { action }) => {
        if (action === "select-option" || action === "remove-value") {
            this.props.onTypeChange(_id);
        }
    };

    setRef = (element) => {
        this.jsonForm = element;
    };

    render () {
        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else {
            content = <div ref={ this.setRef } />;
        }

        return (
            <Fragment>
                <PageHeader title={ t("console.datastores.new.title") } />
                <Panel>
                    <Panel.Body>
                        <Form horizontal>
                            <FormGroupInput
                                isValid={ this.props.isValidId }
                                label={ t("console.datastores.new.id") }
                                onChange={ this.props.onIdChange }
                                validationMessage={ t("console.common.validation.invalidCharacters") }
                                value={ this.props.id }
                            />
                            <FormGroup controlId="dataStoreType">
                                <Col componentClass={ ControlLabel } sm={ 4 }>
                                    { t("console.common.type") }
                                </Col>
                                <Col sm={ 6 }>
                                    <AsyncSingleSelect
                                        cacheOptions
                                        defaultOptions
                                        getOptionLabel={ property("name") }
                                        getOptionValue={ property("_id") }
                                        inputId="dataStoreType"
                                        isClearable={ false }
                                        loadOptions={ NewDataStore.fetchTypes }
                                        onChange={ this.handleChange }
                                    />
                                </Col>
                            </FormGroup>
                            { content }
                        </Form>
                    </Panel.Body>
                    <Panel.Footer>
                        <CreateFooter
                            backRoute={ Router.configuration.routes.realmsDataStores }
                            disabled={ !this.props.isCreateAllowed }
                            onCreateClick={ this.handleCreate }
                        />
                    </Panel.Footer>
                </Panel>
            </Fragment>
        );
    }
}

NewDataStore.propTypes = {
    id: PropTypes.string.isRequired,
    isCreateAllowed: PropTypes.bool.isRequired,
    isFetching: PropTypes.bool.isRequired,
    isValidId: PropTypes.bool.isRequired,
    onCreate: PropTypes.func.isRequired,
    onIdChange: PropTypes.func.isRequired,
    onTypeChange: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.any),
    template: PropTypes.objectOf(PropTypes.object)
};

export default NewDataStore;
