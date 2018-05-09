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
 * Copyright 2017-2018 ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import { first, forEach, map, pluck, values } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { getAll, remove } from "org/forgerock/openam/ui/admin/services/realm/federation/CirclesOfTrustService";
import {
    removeInstance,
    setInstances
} from "store/modules/remote/config/realm/applications/federation/circlesoftrust/instances";
import connectWithStore from "components/redux/connectWithStore";
import ListCirclesOfTrust from "./ListCirclesOfTrust";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListCirclesOfTrustContainer extends Component {
    constructor () {
        super();

        this.state = { isFetching: true };

        this.handleEdit = this.handleEdit.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
    }

    componentDidMount () {
        const realm = this.props.router.params[0];

        getAll(realm).then((response) => {
            this.setState({ isFetching: false });
            this.props.setInstances(response.result);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleEdit (item) {
        const id = item._id;
        const realm = this.props.router.params[0];

        Router.routeTo(Router.configuration.routes.realmsApplicationsFederationCirclesOfTrustEdit, {
            args: map([realm, id], encodeURIComponent),
            trigger: true
        });
    }

    handleDelete (items) {
        const ids = pluck(items, "_id");
        const realm = this.props.router.params[0];

        showConfirmationBeforeAction({
            message: t("console.applications.federation.circlesoftrust.confirmDeleteSelected", { count: ids.length })
        }, () => {
            remove(realm, ids)
                .then((response) => map(response, first))
                .then((response) => {
                    Messages.messages.displayMessageFromConfig("changesSaved");
                    forEach(response, this.props.removeInstance);
                }, (response) => {
                    Messages.addMessage({ response, type: Messages.TYPE_DANGER });
                });
        });
    }

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsApplicationsFederationCirclesOfTrustNew, [
            encodeURIComponent(realm)
        ]);

        return (
            <ListCirclesOfTrust
                isFetching={ this.state.isFetching }
                items={ this.props.circlesoftrust }
                newHref={ `#${newHref}` }
                onDelete={ this.handleDelete }
                onRowClick={ this.handleEdit }
            />
        );
    }
}

ListCirclesOfTrustContainer.propTypes = {
    circlesoftrust: PropTypes.arrayOf(PropTypes.object),
    removeInstance: PropTypes.func.isRequired,
    router: withRouterPropType,
    setInstances: PropTypes.func.isRequired
};

ListCirclesOfTrustContainer = connectWithStore(ListCirclesOfTrustContainer,
    (state) => ({
        circlesoftrust: values(state.remote.config.realm.applications.federation.circlesoftrust.instances)
    }),
    (dispatch) => ({
        removeInstance: bindActionCreators(removeInstance, dispatch),
        setInstances: bindActionCreators(setInstances, dispatch)
    })
);
ListCirclesOfTrustContainer = withRouter(ListCirclesOfTrustContainer);

export default ListCirclesOfTrustContainer;
