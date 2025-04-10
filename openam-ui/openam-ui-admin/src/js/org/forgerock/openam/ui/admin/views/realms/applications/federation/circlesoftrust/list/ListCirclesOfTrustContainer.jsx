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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import { bindActionCreators } from "redux";
import { forEach, map, values } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component, Fragment } from "react";

import { getAll, remove } from "org/forgerock/openam/ui/admin/services/realm/federation/CirclesOfTrustService";
import { removeInstance, setInstances } from
    "store/modules/remote/config/realm/applications/federation/circlesoftrust/instances";
import { show as showDeleteDialog } from "components/dialogs/Delete";
import connectWithStore from "components/redux/connectWithStore";
import ListCirclesOfTrust from "./ListCirclesOfTrust";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListCirclesOfTrustContainer extends Component {
    constructor () {
        super();

        this.state = { isFetching: true };
    }

    componentDidMount () {
        const realm = this.props.router.params[0];

        getAll(realm).then((response) => {
            this.props.setInstances(response.result);
        }).finally(() => {
            this.setState({ isFetching: false });
        });
    }

    handleEdit = (e, item) => {
        const id = item._id;
        const realm = this.props.router.params[0];

        Router.routeTo(Router.configuration.routes.realmsApplicationsFederationCirclesOfTrustEdit, {
            args: map([realm, id], encodeURIComponent),
            trigger: true
        });
    };

    handleDelete = (items) => {
        const ids = items.map((item) => item._id);

        showDeleteDialog({
            names: ids,
            objectName: "circleOfTrust",
            onConfirm: async () => {
                const realm = this.props.router.params[0];
                const response = await remove(realm, ids);

                forEach(response, this.props.removeInstance);
            }
        });
    };

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsApplicationsFederationCirclesOfTrustNew, [
            encodeURIComponent(realm)
        ]);

        return (
            <Fragment>
                <PageHeader title={ t("console.applications.federation.circlesoftrust.list.title") } />
                <ListCirclesOfTrust
                    isFetching={ this.state.isFetching }
                    items={ this.props.circlesoftrust }
                    newHref={ `#${newHref}` }
                    onDelete={ this.handleDelete }
                    onRowClick={ this.handleEdit }
                />
            </Fragment>
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
