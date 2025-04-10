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

import { Form, Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import CreateFooter from "org/forgerock/openam/ui/admin/views/realms/common/CreateFooter";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";

class NewUserService extends Component {
    componentDidUpdate () {
        if (!this.jsonSchemaView && this.props.schema && this.props.template) {
            this.jsonSchemaView = new FlatJSONSchemaView({
                schema: new JSONSchema(this.props.schema),
                values: new JSONValues(this.props.template)
            });

            if (this.jsonForm) {
                this.jsonForm.appendChild(this.jsonSchemaView.render().el);
            }
        }
    }

    handleCreate = () => {
        this.props.onCreate(this.jsonSchemaView.getData());
    };

    setRef = (element) => {
        this.jsonForm = element;
        if (this.jsonForm && this.jsonSchemaView) {
            this.jsonForm.appendChild(this.jsonSchemaView.render().el);
        }
    };

    render () {
        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else {
            content = <div ref={ this.setRef } />;
        }

        return (
            <div>
                <PageHeader
                    title={ t("console.identities.users.edit.services.new.subtitle", { type: this.props.type }) }
                />
                <Panel>
                    <Panel.Body>
                        <Form horizontal>
                            { content }
                        </Form>
                    </Panel.Body>
                    <Panel.Footer>
                        <CreateFooter
                            backRoute={ Router.configuration.routes.realmsIdentitiesUsersEdit }
                            backRouteArgs={ [this.props.id] }
                            onCreateClick={ this.handleCreate }
                        />
                    </Panel.Footer>
                </Panel>
            </div>
        );
    }
}

NewUserService.propTypes = {
    id: PropTypes.string.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onCreate: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    template: PropTypes.objectOf(PropTypes.object).isRequired,
    type: PropTypes.string.isRequired
};

export default NewUserService;
