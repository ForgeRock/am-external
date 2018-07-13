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
import { bindActionCreators } from "redux";
import { first, forEach, map, values } from "lodash";
import { t } from "i18next";
import React, { Component, PropTypes } from "react";

import { getAll, remove } from "org/forgerock/openam/ui/admin/services/realm/authentication/TreeService";
import { remove as fromRemote, set as setTrees } from "store/modules/remote/authentication/trees/list";
import { remove as fromLocal } from "store/modules/local/authentication/trees/list";
import connectWithStore from "components/redux/connectWithStore";
import ListTrees from "./ListTrees";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListTreesContainer extends Component {
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
            this.props.setTrees(response.result);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleDelete (ids) {
        const realm = this.props.router.params[0];
        showConfirmationBeforeAction({
            message: t("console.authentication.trees.list.confirmDeleteSelected", { count: ids.length })
        }, () => {
            remove(realm, ids)
                .then((response) => map(response, first))
                .then((response) => {
                    Messages.messages.displayMessageFromConfig("changesSaved");
                    forEach(response, (tree) => {
                        this.props.removeTree.fromLocal(tree._id);
                        this.props.removeTree.fromRemote(tree._id);
                    });
                }, (reason) => {
                    Messages.addMessage({ reason, type: Messages.TYPE_DANGER });
                });
        });
    }

    handleEdit (id) {
        const realm = this.props.router.params[0];
        return Router.routeTo(Router.configuration.routes.realmsAuthenticationTreesEdit, {
            args: map([realm, id], encodeURIComponent),
            trigger: true
        });
    }

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsAuthenticationTreesNew, [
            encodeURIComponent(realm)
        ]);
        return (
            <ListTrees
                isFetching={ this.state.isFetching }
                newHref={ `#${newHref}` }
                onDelete={ this.handleDelete }
                onEdit={ this.handleEdit }
                trees={ this.props.trees }
            />
        );
    }
}

ListTreesContainer.propTypes = {
    removeTree: PropTypes.func.isRequired,
    router: withRouterPropType,
    setTrees: PropTypes.func.isRequired,
    trees: PropTypes.arrayOf(PropTypes.object)
};

ListTreesContainer = connectWithStore(ListTreesContainer,
    (state) => ({
        trees: values(state.remote.authentication.trees.list)
    }),
    (dispatch) => ({
        removeTree: bindActionCreators({ fromLocal, fromRemote }, dispatch),
        setTrees: bindActionCreators(setTrees, dispatch)
    })
);
ListTreesContainer = withRouter(ListTreesContainer);

export default ListTreesContainer;
