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
 * Copyright 2017 ForgeRock AS.
 */

import { Panel } from "react-bootstrap";
import { t } from "i18next";
import React, { Component, PropTypes } from "react";

import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import NewTreeFooter from "./NewTreeFooter";
import NewTreeNameInput from "./NewTreeNameInput";
import PageHeader from "components/PageHeader";

class NewTree extends Component {
    constructor (props) {
        super(props);
        this.handleCreate = this.handleCreate.bind(this);
        this.setRef = this.setRef.bind(this);
    }

    componentDidUpdate () {
        if (!this.jsonSchemaView && this.props.template) {
            this.jsonSchemaView = new FlatJSONSchemaView({
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

    setRef (element) {
        this.jsonForm = element;
    }

    render () {
        const footer = (
            <NewTreeFooter
                disableCreate={ !this.props.isCreateAllowed }
                onCreateClick={ this.handleCreate }
            />
        );

        const content = this.props.isFetching
            ? <Loading />
            : (
                <div>
                    <NewTreeNameInput
                        onTreeNameChange={ this.props.onTreeNameChange }
                        treeName={ this.props.treeName }
                    />
                    <div ref={ this.setRef } />
                </div>
            );

        return (
            <div>
                <PageHeader title={ t("console.authentication.trees.new.title") } />
                <Panel footer={ footer }>
                    { content }
                </Panel>
            </div>
        );
    }
}

NewTree.propTypes = {
    isCreateAllowed: PropTypes.bool.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onCreate: PropTypes.func.isRequired,
    onTreeNameChange: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    template: PropTypes.objectOf(PropTypes.object).isRequired,
    treeName: PropTypes.string.isRequired
};

export default NewTree;
