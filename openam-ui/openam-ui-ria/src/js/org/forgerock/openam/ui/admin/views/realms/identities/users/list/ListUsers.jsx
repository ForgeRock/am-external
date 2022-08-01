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
 * Copyright 2018-2022 ForgeRock AS.
 */

import { omit } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import dataFieldObjectPath from "components/table/cells/dataFieldObjectPath";
import IconCell from "components/table/cells/IconCell";
import List from "components/list/List";
import StatusCell from "components/table/cells/StatusCell";

const ListUsers = (props) => {
    const columns = [{
        dataField: "username",
        formatter: IconCell("fa-address-card"),
        sort: true,
        text: t("console.identities.users.list.grid.0")
    }, {
        dataField: "givenName",
        text: t("console.identities.users.list.grid.1")
    }, {
        dataField: "sn",
        text: t("console.identities.users.list.grid.2")
    }, {
        dataField: "mail",
        text: t("console.identities.users.list.grid.3")
    }, {
        dataField: "inetUserStatus",
        formatter: dataFieldObjectPath(StatusCell, "[0]"),
        text: t("console.identities.users.list.grid.4")
    }];
    return (
        <Panel className="fr-panel-tab">
            <Panel.Body>
                <List
                    { ...omit(props, "children") }
                    addButton={ {
                        href: props.newHref,
                        title: t("console.identities.users.list.callToAction.button")
                    } }
                    columns={ columns }
                    description={ t("console.identities.users.list.callToAction.description") }
                    keyField="username"
                    title={ t("console.identities.users.list.callToAction.title") }
                />
            </Panel.Body>
        </Panel>
    );
};

ListUsers.propTypes = {
    newHref: PropTypes.string.isRequired
};

export default ListUsers;
