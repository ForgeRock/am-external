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
import { bindActionCreators } from "redux";
import { findWhere, forEach, get, map, pluck, result } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { getAllInstances, getAllTypes, getCreatables, remove }
    from "org/forgerock/openam/ui/admin/services/realm/identities/UsersServicesService";
import { removeInstance, setInstances } from "store/modules/remote/config/realm/identities/users/services/instances";
import { setCreatables } from "store/modules/remote/config/realm/identities/users/services/creatables";
import { setTypes } from "store/modules/remote/config/realm/identities/users/services/types";
import connectWithStore from "components/redux/connectWithStore";
import ListUserServices from "./ListUserServices";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import Router from "org/forgerock/commons/ui/common/main/Router";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListUserServicesContainer extends Component {
    constructor () {
        super();

        this.state = {
            isFetching: true
        };

        this.handleEdit = this.handleEdit.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
    }

    componentDidMount () {
        const realm = this.props.router.params[0];
        const userId = this.props.router.params[1];

        Promise.all([
            getAllInstances(realm, userId),
            getAllTypes(realm, userId),
            getCreatables(realm, userId)
        ]).then(([instances, types, creatables]) => {
            this.setState({ isFetching: false });
            this.props.setInstances(instances, userId);
            this.props.setTypes(types, userId);
            this.props.setCreatables(creatables, userId);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleEdit (item) {
        const id = item._id;
        const realm = this.props.router.params[0];
        const userId = this.props.router.params[1];

        Router.routeTo(Router.configuration.routes.realmsIdentitiesUsersServicesEdit, {
            args: map([realm, userId, id], encodeURIComponent),
            trigger: true
        });
    }

    handleDelete (items) {
        const types = pluck(items, "_id");
        const realm = this.props.router.params[0];
        const userId = this.props.router.params[1];

        showConfirmationBeforeAction({
            message: t("console.identities.users.edit.services.confirmDeleteSelected", { count: types.length })
        }, () => {
            remove(realm, userId, types)
                .then(() => {
                    Messages.messages.displayMessageFromConfig("changesSaved");
                    forEach(types, (id) => {
                        this.props.removeInstance(id, userId);
                    });

                    getCreatables(realm, userId).then((creatables) => {
                        this.props.setCreatables(creatables, userId);
                    }, (response) => {
                        Messages.addMessage({ response, type: Messages.TYPE_DANGER });
                    });
                }, (response) => {
                    Messages.addMessage({ response, type: Messages.TYPE_DANGER });
                });
        });
    }

    render () {
        const realm = this.props.router.params[0];
        const userId = this.props.router.params[1];

        return (
            <ListUserServices
                creatables={ this.props.creatables }
                isFetching={ this.state.isFetching }
                items={ this.props.instances }
                onDelete={ this.handleDelete }
                onRowClick={ this.handleEdit }
                realm={ realm }
                userId={ userId }
            />
        );
    }
}

ListUserServicesContainer.propTypes = {
    creatables: PropTypes.arrayOf(PropTypes.object),
    instances: PropTypes.arrayOf(PropTypes.object),
    removeInstance: PropTypes.func.isRequired,
    router: withRouterPropType,
    setCreatables: PropTypes.func.isRequired,
    setInstances: PropTypes.func.isRequired,
    setTypes: PropTypes.func.isRequired
};

ListUserServicesContainer = connectWithStore(ListUserServicesContainer,
    (state, props) => {
        const userId = props.router.params[1];
        const types = get(state.remote.config.realm.identities.users.services.types, userId);
        const nextDescendants = get(state.remote.config.realm.identities.users.services.instances, userId);
        const instances = map(nextDescendants, (instance) => {
            instance.name = result(findWhere(types, { "_id": instance._id }), "name");
            return instance;
        });

        return {
            creatables: get(state.remote.config.realm.identities.users.services.creatables, userId),
            instances
        };
    },
    (dispatch) => ({
        removeInstance: bindActionCreators(removeInstance, dispatch),
        setCreatables: bindActionCreators(setCreatables, dispatch),
        setInstances: bindActionCreators(setInstances, dispatch),
        setTypes: bindActionCreators(setTypes, dispatch)
    })
);
ListUserServicesContainer = withRouter(ListUserServicesContainer);

export default ListUserServicesContainer;
