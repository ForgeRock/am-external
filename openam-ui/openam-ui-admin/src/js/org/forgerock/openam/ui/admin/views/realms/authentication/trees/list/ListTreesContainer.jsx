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
 * Copyright 2017-2022 ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import { forEach, map, values } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { getAll, remove } from "org/forgerock/openam/ui/admin/services/realm/authentication/TreeService";
import { remove as fromRemote, set as setTrees } from "store/modules/remote/config/realm/authentication/trees/list";
import { remove as fromLocal } from "store/modules/local/config/realm/authentication/trees/list";
import { show as showDeleteDialog } from "components/dialogs/Delete";
import connectWithStore from "components/redux/connectWithStore";
import ListTrees from "./ListTrees";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListTreesContainer extends Component {
    constructor () {
        super();
        this.state = { isFetching: true };
    }

    componentDidMount () {
        const realm = this.props.router.params[0];

        getAll(realm).then((response) => {
            this.setState({ isFetching: false });
            this.props.setTrees(response.result);
        }).finally(() => {
            this.setState({ isFetching: false });
        });
    }

    handleDelete = (items) => {
        const ids = items.map((item) => item._id);

        showDeleteDialog({
            names: ids,
            objectName: "tree",
            onConfirm: async () => {
                const realm = this.props.router.params[0];
                const response = await remove(realm, ids);

                forEach(response, (tree) => {
                    this.props.removeTree.fromLocal(tree._id);
                    this.props.removeTree.fromRemote(tree._id);
                });
            }
        });
    };

    handleEdit = (e, item) => {
        const id = item._id;
        const realm = this.props.router.params[0];

        Router.routeTo(Router.configuration.routes.realmsAuthenticationTreesEdit, {
            args: map([realm, id], encodeURIComponent),
            trigger: true
        });
    };

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsAuthenticationTreesNew, [
            encodeURIComponent(realm)
        ]);

        return (
            <ListTrees
                isFetching={ this.state.isFetching }
                items={ this.props.trees }
                newHref={ `#${newHref}` }
                onDelete={ this.handleDelete }
                onRowClick={ this.handleEdit }
            />
        );
    }
}

ListTreesContainer.propTypes = {
    removeTree: PropTypes.shape({
        fromLocal: PropTypes.func.isRequired,
        fromRemote: PropTypes.func.isRequired
    }).isRequired,
    router: withRouterPropType,
    setTrees: PropTypes.func.isRequired,
    trees: PropTypes.arrayOf(PropTypes.object)
};

ListTreesContainer = connectWithStore(ListTreesContainer,
    (state) => ({
        trees: values(state.remote.config.realm.authentication.trees.list)
    }),
    (dispatch) => ({
        removeTree: bindActionCreators({ fromLocal, fromRemote }, dispatch),
        setTrees: bindActionCreators(setTrees, dispatch)
    })
);
ListTreesContainer = withRouter(ListTreesContainer);

export default ListTreesContainer;
