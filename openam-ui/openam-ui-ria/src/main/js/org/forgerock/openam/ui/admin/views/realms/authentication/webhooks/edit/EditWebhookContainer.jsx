/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import PropTypes from "prop-types";
import React, { Component } from "react";
import { add } from "store/modules/remote/config/realm/authentication/webhooks/instances";
import { update, getWithSchema, remove }
    from "org/forgerock/openam/ui/admin/services/realm/authentication/WebhookService";
import { t } from "i18next";
import { setSchema } from "store/modules/remote/config/realm/authentication/webhooks/schema";
import connectWithStore from "components/redux/connectWithStore";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import EditWebhook from "./EditWebhook";
import Router from "org/forgerock/commons/ui/common/main/Router";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class EditWebhookContainer extends Component {
    constructor () {
        super();
        this.state = {
            isFetching: true
        };
        this.handleUpdate = this.handleUpdate.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
    }

    componentDidMount () {
        const realm = this.props.router.params[0];
        const id = this.props.router.params[1];

        getWithSchema(realm, id).then(({ schema, data }) => {
            this.setState({ isFetching: false });
            this.props.setSchema(schema[0]);
            this.props.addInstance(data[0]);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleUpdate (formData) {
        const realm = this.props.router.params[0];
        const id = this.props.router.params[1];
        update(realm, formData, id).then(() => {
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleDelete (event) {
        event.preventDefault();

        FormHelper.showConfirmationBeforeDeleting({
            message: t("console.common.confirmDeleteItem")
        }, () => {
            const realm = this.props.router.params[0];
            const id = this.props.router.params[1];

            remove(realm, [id]).then(() => {
                Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });

                Router.routeTo(Router.configuration.routes.realmsAuthenticationWebhooks, {
                    args: [encodeURIComponent(realm)], trigger: true
                });
            }, (model, response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        });
    }

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
