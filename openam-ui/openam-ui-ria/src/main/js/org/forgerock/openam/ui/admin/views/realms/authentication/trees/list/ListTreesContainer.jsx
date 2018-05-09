/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
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

import { getAll, remove } from "org/forgerock/openam/ui/admin/services/realm/authentication/TreeService";
import { remove as fromRemote, set as setTrees } from "store/modules/remote/config/realm/authentication/trees/list";
import { remove as fromLocal } from "store/modules/local/config/realm/authentication/trees/list";
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

    handleDelete (items) {
        const ids = pluck(items, "_id");
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
                }, (response) => {
                    Messages.addMessage({ response, type: Messages.TYPE_DANGER });
                });
        });
    }

    handleEdit (item) {
        const id = item._id;
        const realm = this.props.router.params[0];

        Router.routeTo(Router.configuration.routes.realmsAuthenticationTreesEdit, {
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
                items={ this.props.trees }
                newHref={ `#${newHref}` }
                onDelete={ this.handleDelete }
                onRowClick={ this.handleEdit }
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
        trees: values(state.remote.config.realm.authentication.trees.list)
    }),
    (dispatch) => ({
        removeTree: bindActionCreators({ fromLocal, fromRemote }, dispatch),
        setTrees: bindActionCreators(setTrees, dispatch)
    })
);
ListTreesContainer = withRouter(ListTreesContainer);

export default ListTreesContainer;