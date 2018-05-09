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

import { assign, cloneDeep, get, reduce } from "lodash";
import { Button, Clearfix, Panel } from "react-bootstrap";
import { t } from "i18next";
import React, { Component } from "react";

import { getAll as getAllGroups }
    from "org/forgerock/openam/ui/admin/services/realm/identities/GroupsService";
import { get as getGroups, update, getSchema }
    from "org/forgerock/openam/ui/admin/services/realm/identities/UsersGroupsService";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class EditUserGroups extends Component {
    constructor (props) {
        super(props);
        this.setRef = this.setRef.bind(this);
        this.handleSave = this.handleSave.bind(this);
    }

    componentDidMount () {
        const realm = this.props.router.params[0];
        const id = this.props.router.params[1];
        Promise.all([
            getSchema(realm, id),
            getGroups(realm, id),
            getAllGroups(realm)
        ]).then(([schema, values, allGroups]) => {
            const userGroupValues = values.map((value) => value.groupname);
            const schemaWithGroups = this.addGroupsSelectionToTheSchema(schema[0], allGroups[0].result);
            this.jsonSchemaView = new FlatJSONSchemaView({
                schema: new JSONSchema(schemaWithGroups),
                values: new JSONValues({
                    groups: {
                        value: userGroupValues
                    }
                })
            });
            this.element.appendChild(this.jsonSchemaView.render().el);
        });
    }

    addGroupsSelectionToTheSchema (schema, groups) {
        const schemaCopy = cloneDeep(schema);
        const groupsProperty = get(schemaCopy, "properties.groups.items");
        if (groupsProperty) {
            const parsedGroups = reduce(groups, (property, group) => {
                property.enum.push(group._id);
                property.options.enum_titles.push(group.cn[0]);
                return property;
            }, { "enum": [], "options": { "enum_titles": [] } });
            assign(groupsProperty, parsedGroups);
        } else {
            console.error("[EditUserGroups] Unable to add available groups to 'groups' property.");
        }
        return schemaCopy;
    }

    handleSave () {
        if (!this.jsonSchemaView.subview.isValid()) {
            Messages.addMessage({ message: t("common.form.validation.errorsNotSaved"), type: Messages.TYPE_DANGER });
            return;
        }

        const realm = this.props.router.params[0];
        const id = this.props.router.params[1];
        const formValues = new JSONValues(this.jsonSchemaView.subview.getData());
        update(realm, id, formValues.raw.groups).then(() => {
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    setRef (element) {
        this.element = element;
    }

    render () {
        const footer = (
            <Clearfix>
                <div className="pull-right">
                    <Button bsStyle="primary" onClick={ this.handleSave }>
                        { t ("common.form.saveChanges") }
                    </Button>
                </div>
            </Clearfix>
        );
        return (
            <Panel className="fr-panel-tab" footer={ footer }>
                <div ref={ this.setRef } />
            </Panel>
        );
    }
}

EditUserGroups.propTypes = {
    router: withRouterPropType
};

export default withRouter(EditUserGroups);
