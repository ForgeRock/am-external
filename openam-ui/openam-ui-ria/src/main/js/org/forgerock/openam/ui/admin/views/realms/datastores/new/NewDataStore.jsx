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
 * Copyright 2018 ForgeRock AS.
 */

import { Col, ControlLabel, Form, FormGroup, Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";
import Select from "react-select";

import { getCreatableTypes } from "org/forgerock/openam/ui/admin/services/realm/DataStoresService";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import Footer from "org/forgerock/openam/ui/admin/views/realms/common/new/Footer";
import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";

class NewDataStore extends Component {
    static fetchTypes () {
        return getCreatableTypes().then((types) => {
            return { options: types.result };
        });
    }

    constructor (props) {
        super(props);

        this.handleCreate = this.handleCreate.bind(this);
        this.handleTypeChange = this.handleTypeChange.bind(this);
        this.setRef = this.setRef.bind(this);
    }

    componentWillReceiveProps (nextProps) {
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

    handleCreate () {
        this.props.onCreate(this.jsonSchemaView.getData());
    }

    handleTypeChange (selectedType) {
        this.props.onTypeChange(selectedType);
    }

    setRef (element) {
        this.jsonForm = element;
    }

    render () {
        const footer = (
            <Footer
                backRoute={ Router.configuration.routes.realmsDataStores }
                isCreateAllowed={ this.props.isCreateAllowed }
                onCreateClick={ this.handleCreate }
            />
        );

        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else {
            content = <div ref={ this.setRef } />;
        }

        return (
            <div>
                <PageHeader title={ t("console.datastores.new.title") } />
                <Panel footer={ footer }>
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
                                { t("console.datastores.new.type") }
                            </Col>
                            <Col sm={ 6 }>
                                <Select.Async
                                    backspaceRemoves={ false }
                                    clearable={ false }
                                    inputProps={ { id: "dataStoreType" } }
                                    labelKey="name"
                                    loadOptions={ NewDataStore.fetchTypes }
                                    onChange={ this.handleTypeChange }
                                    placeholder={ t("common.form.select") }
                                    simpleValue
                                    value={ this.props.type }
                                    valueKey="_id"
                                />
                            </Col>
                        </FormGroup>
                        { content }
                    </Form>
                </Panel>
            </div>
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
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    template: PropTypes.objectOf(PropTypes.object).isRequired,
    type: PropTypes.string.isRequired
};

export default NewDataStore;
