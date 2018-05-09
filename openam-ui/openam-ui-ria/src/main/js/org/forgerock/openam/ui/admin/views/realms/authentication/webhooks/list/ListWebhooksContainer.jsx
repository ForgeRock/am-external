/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import { first, forEach, map, pluck, values } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { getAll, remove } from "org/forgerock/openam/ui/admin/services/realm/authentication/WebhookService";
import {
    remove as removeInstance,
    set as setInstance
} from "store/modules/remote/config/realm/authentication/webhooks/instances";
import connectWithStore from "components/redux/connectWithStore";
import ListWebhooks from "./ListWebhooks";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListWebhooksContainer extends Component {
    constructor () {
        super();

        this.state = { isFetching: true };

        this.handleDelete = this.handleDelete.bind(this);
        this.handleEdit = this.handleEdit.bind(this);
    }

    componentDidMount () {
        const realm = this.props.router.params[0];

        getAll(realm).then((response) => {
            this.setState({ isFetching: false });
            this.props.setInstance(response.result);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleDelete (items) {
        const ids = pluck(items, "_id");
        const realm = this.props.router.params[0];

        showConfirmationBeforeAction({
            message: t("console.authentication.webhooks.list.confirmDeleteSelected", { count: ids.length })
        }, () => {
            remove(realm, ids)
                .then((response) => map(response, first))
                .then((response) => {
                    Messages.messages.displayMessageFromConfig("changesSaved");
                    forEach(response, (webhook) => {
                        this.props.removeInstance(webhook._id);
                    });
                }, (reason) => {
                    Messages.addMessage({ reason, type: Messages.TYPE_DANGER });
                });
        });
    }

    handleEdit (item) {
        const id = item._id;
        const realm = this.props.router.params[0];

        Router.routeTo(Router.configuration.routes.realmsAuthenticationWebhooksEdit, {
            args: map([realm, id], encodeURIComponent),
            trigger: true
        });
    }

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsAuthenticationWebhooksNew, [
            encodeURIComponent(realm)
        ]);

        return (
            <ListWebhooks
                isFetching={ this.state.isFetching }
                items={ this.props.webhooks }
                newHref={ `#${newHref}` }
                onDelete={ this.handleDelete }
                onRowClick={ this.handleEdit }
            />
        );
    }
}

ListWebhooksContainer.propTypes = {
    removeInstance: PropTypes.func.isRequired,
    router: withRouterPropType,
    setInstance: PropTypes.func.isRequired,
    webhooks: PropTypes.arrayOf(PropTypes.object)
};

ListWebhooksContainer = connectWithStore(ListWebhooksContainer,
    (state) => ({
        webhooks: values(state.remote.config.realm.authentication.webhooks.instances)
    }),
    (dispatch) => ({
        removeInstance: bindActionCreators(removeInstance, dispatch),
        setInstance: bindActionCreators(setInstance, dispatch)
    })
);
ListWebhooksContainer = withRouter(ListWebhooksContainer);

export default ListWebhooksContainer;
