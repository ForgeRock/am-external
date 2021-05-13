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

import { Form, Button, Panel } from "react-bootstrap";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { t } from "i18next";
import EditFooter from "org/forgerock/openam/ui/admin/views/realms/common/EditFooter";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";

class EditWebhook extends Component {
    UNSAFE_componentWillReceiveProps (nextProps) {
        if (this.jsonSchemaView) {
            this.jsonSchemaView.setData(nextProps.values);
        }
    }

    componentDidUpdate () {
        if (!this.jsonSchemaView && this.props.data && this.props.schema) {
            this.jsonSchemaView = new FlatJSONSchemaView({
                schema: new JSONSchema(this.props.schema),
                values: new JSONValues(this.props.data)
            });
            this.jsonForm.appendChild(this.jsonSchemaView.render().el);
        }
    }

    setRef = (element) => {
        this.jsonForm = element;
    };

    handleSave = () => {
        this.props.onSave(this.jsonSchemaView.getData());
    };

    render () {
        const content = this.props.isFetching
            ? <Loading />
            : (
                <Form>
                    <div ref={ this.setRef } />
                </Form>
            );

        return (
            <div>
                <PageHeader
                    icon="anchor"
                    subtitle={ t("console.authentication.webhooks.edit.type") }
                    title={ this.props.id }
                >
                    <Button onClick={ this.props.onDelete }>
                        <i className="fa fa-times" /> { t("common.form.delete") }
                    </Button>
                </PageHeader>
                <Panel>
                    <Panel.Body>{ content }</Panel.Body>
                    <Panel.Footer>
                        <EditFooter onSaveClick={ this.handleSave } />
                    </Panel.Footer>
                </Panel>
            </div>
        );
    }
}

EditWebhook.propTypes = {
    data: PropTypes.objectOf(PropTypes.object).isRequired,
    id: PropTypes.string.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onDelete: PropTypes.func.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    values: PropTypes.objectOf(PropTypes.object).isRequired
};

export default EditWebhook;
