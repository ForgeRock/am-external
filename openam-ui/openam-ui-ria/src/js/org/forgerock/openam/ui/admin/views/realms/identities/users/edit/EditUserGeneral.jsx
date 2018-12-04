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

import { Panel } from "react-bootstrap";
import React, { Component } from "react";
import { t } from "i18next";
import { isArray, mapValues, reduce } from "lodash";

import { get as getUser, getSchema, update }
    from "org/forgerock/openam/ui/admin/services/realm/identities/UsersService";
import EditFooter from "org/forgerock/openam/ui/admin/views/realms/common/EditFooter";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const arrayifyEmptyStringValues = (values, schema) => {
    return mapValues(values, (value, key) => {
        if (schema.properties[key].type === "string" && value === "") {
            return [];
        }
        return value;
    });
};

const flattenArrayStringValues = (values, schema) => {
    return mapValues(values, (value, key) => {
        const schemaProperty = schema.properties[key];
        if (schemaProperty && schemaProperty.type === "string" && isArray(value)) {
            return value[0];
        }
        return value;
    });
};

const removeValuesNotInSchema = (values, schema) => {
    return reduce(values, (result, value, key) => {
        const schemaProperty = schema.properties[key];
        if (schemaProperty) {
            result[key] = value;
        }
        return result;
    }, {});
};

class EditUserGeneral extends Component {
    constructor (props) {
        super(props);
        this.setRef = this.setRef.bind(this);
        this.handleSave = this.handleSave.bind(this);
    }

    componentDidMount () {
        const realm = this.props.router.params[0];
        const id = this.props.router.params[1];
        Promise.all([getSchema(realm), getUser(realm, id)]).then(([schema, values]) => {
            this.schema = new JSONSchema(schema);
            this.jsonSchemaView = new FlatJSONSchemaView({
                schema: this.schema,
                values: new JSONValues(flattenArrayStringValues(values, schema))
            });
            this.element.appendChild(this.jsonSchemaView.render().el);
        });
    }

    handleSave () {
        if (!this.jsonSchemaView.subview.isValid()) {
            Messages.addMessage({ message: t("common.form.validation.errorsNotSaved"), type: Messages.TYPE_DANGER });
            return;
        }
        const formValues = new JSONValues(this.jsonSchemaView.subview.getData());
        const valuesWithoutNullPasswords = formValues.removeNullPasswords(this.schema);
        const valuesFromSchema = removeValuesNotInSchema(valuesWithoutNullPasswords.raw, this.schema.raw);
        const values = arrayifyEmptyStringValues(valuesFromSchema, this.schema.raw);
        delete values.dn;

        const realm = this.props.router.params[0];
        const id = this.props.router.params[1];

        update(realm, values, id).then(() => {
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    setRef (element) {
        this.element = element;
    }

    render () {
        return (
            <Panel className="fr-panel-tab">
                <Panel.Body><div ref={ this.setRef } /></Panel.Body>
                <Panel.Footer>
                    <EditFooter onSaveClick={ this.handleSave } />
                </Panel.Footer>
            </Panel>
        );
    }
}

EditUserGeneral.propTypes = {
    router: withRouterPropType
};

export default withRouter(EditUserGeneral);
