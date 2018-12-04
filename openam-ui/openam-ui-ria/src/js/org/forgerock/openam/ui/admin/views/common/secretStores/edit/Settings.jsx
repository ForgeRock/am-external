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
import { isEqual } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component, Fragment } from "react";

import EditFooter from "org/forgerock/openam/ui/admin/views/realms/common/EditFooter";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import Messages from "org/forgerock/commons/ui/common/components/Messages";

class Settings extends Component {
    constructor (props) {
        super(props);

        this.setRef = this.setRef.bind(this);
        this.handleSave = this.handleSave.bind(this);
    }

    componentWillReceiveProps (nextProps) {
        if (this.jsonSchemaView && !isEqual(this.props.schema, nextProps.schema)) {
            this.jsonSchemaView.destroy();
            this.jsonSchemaView = null;
        }

        if (this.jsonSchemaView) {
            this.jsonSchemaView.setData(nextProps.values);
            this.jsonSchemaView.render();
        }
    }

    componentDidUpdate () {
        if (!this.jsonSchemaView && this.form && this.props.values && this.props.schema) {
            this.jsonSchemaView = new FlatJSONSchemaView({
                schema: new JSONSchema(this.props.schema),
                values: new JSONValues(this.props.values)
            });
            this.form.appendChild(this.jsonSchemaView.render().el);
        }
    }

    handleSave () {
        if (!this.jsonSchemaView.subview.isValid()) {
            Messages.addMessage({ message: t("common.form.validation.errorsNotSaved"), type: Messages.TYPE_DANGER });
            return;
        }
        const formValues = new JSONValues(this.jsonSchemaView.subview.getData());
        const values = formValues.removeNullPasswords(new JSONSchema(this.props.schema));
        this.props.onSave(values.raw);
    }

    setRef (element) {
        this.form = element;
    }

    render () {
        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else {
            content = (
                <Fragment>
                    <Panel.Body>
                        <div ref={ this.setRef } />
                    </Panel.Body>
                    <Panel.Footer>
                        <EditFooter onSaveClick={ this.handleSave } />
                    </Panel.Footer>
                </Fragment>
            );
        }

        return content;
    }
}

Settings.propTypes = {
    isFetching: PropTypes.bool.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.any),
    values: PropTypes.objectOf(PropTypes.any)
};

export default Settings;
