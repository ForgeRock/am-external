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
import { bindActionCreators } from "redux";
import PropTypes from "prop-types";
import React, { Component } from "react";
import { add } from "store/modules/remote/config/realm/authentication/webhooks/instances";
import { update, getWithSchema, remove }
    from "org/forgerock/openam/ui/admin/services/realm/authentication/WebhookService";
import { t } from "i18next";
import { setSchema } from "store/modules/remote/config/realm/authentication/webhooks/schema";
import { show as showDeleteDialog } from "components/dialogs/Delete";
import connectWithStore from "components/redux/connectWithStore";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import EditWebhook from "./EditWebhook";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";
import { revertPlaceholdersToOriginalValue } from "org/forgerock/commons/ui/common/util/PlaceholderUtils";

class EditWebhookContainer extends Component {
    constructor () {
        super();
        this.state = {
            isFetching: true
        };
    }

    componentDidMount () {
        const realm = this.props.router.params[0];
        const id = this.props.router.params[1];

        getWithSchema(realm, id).then(({ schema, data }) => {
            this.setState({ isFetching: false });
            this.props.setSchema(schema);
            this.props.addInstance(data);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleUpdate = (formData) => {
        const realm = this.props.router.params[0];
        const id = this.props.router.params[1];

        const values = revertPlaceholdersToOriginalValue(formData, this.props.schema);

        update(realm, values, id).then(() => {
            Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    };

    handleDelete = (event) => {
        event.preventDefault();

        const realm = this.props.router.params[0];
        const id = this.props.router.params[1];

        showDeleteDialog({
            names: [id],
            objectName: "webhook",
            onConfirm: async () => {
                await remove(this.props.router.params[0], [id]);
                Router.routeTo(Router.configuration.routes.realmsAuthenticationWebhooks, {
                    args: [encodeURIComponent(realm)], trigger: true
                });
            }
        });
    };

    render () {
        return (
            <EditWebhook
                data={ this.props.data }
                id={ this.props.router.params[1] }
                isFetching={ this.state.isFetching }
                onDelete={ this.handleDelete }
                onSave={ this.handleUpdate }
                schema={ this.props.schema }
            />
        );
    }
}

EditWebhookContainer.propTypes = {
    addInstance: PropTypes.func.isRequired,
    data: PropTypes.objectOf(PropTypes.object),
    router: withRouterPropType,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    setSchema: PropTypes.func.isRequired
};

EditWebhookContainer = connectWithStore(EditWebhookContainer,
    (state, ownProps) => {
        const webhookId = ownProps.router.params[1];
        return {
            data: state.remote.config.realm.authentication.webhooks.instances[webhookId],
            schema: state.remote.config.realm.authentication.webhooks.schema
        };
    },
    (dispatch) => ({
        addInstance: bindActionCreators(add, dispatch),
        setSchema: bindActionCreators(setSchema, dispatch)
    })
);
EditWebhookContainer = withRouter(EditWebhookContainer);

export default EditWebhookContainer;
