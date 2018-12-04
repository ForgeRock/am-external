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

import { omit } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import { TableHeaderColumn } from "react-bootstrap-table";
import PropTypes from "prop-types";
import React from "react";

import dataFieldObjectPath from "components/table/cells/dataFieldObjectPath";
import IconCell from "components/table/cells/IconCell";
import List from "components/list/List";
import StatusCell from "components/table/cells/StatusCell";

const ListUsers = (props) => {
    return (
        <Panel className="fr-panel-tab">
            <Panel.Body>
                <List
                    { ...omit(props, "children") }
                    addButton={ {
                        href: props.newHref,
                        title: t("console.identities.users.list.callToAction.button")
                    } }
                    description={ t("console.identities.users.list.callToAction.description") }
                    keyField="username"
                    title={ t("console.identities.users.list.callToAction.title") }
                >
                    <TableHeaderColumn dataField="username" dataFormat={ IconCell("fa-address-card") } dataSort>
                        { t("console.identities.users.list.grid.0") }
                    </TableHeaderColumn>
                    <TableHeaderColumn dataField="givenName">
                        { t("console.identities.users.list.grid.1") }
                    </TableHeaderColumn>
                    <TableHeaderColumn dataField="sn">
                        { t("console.identities.users.list.grid.2") }
                    </TableHeaderColumn>
                    <TableHeaderColumn dataField="mail">
                        { t("console.identities.users.list.grid.3") }
                    </TableHeaderColumn>
                    <TableHeaderColumn
                        dataField="inetUserStatus"
                        dataFormat={ dataFieldObjectPath(StatusCell, "[0]") }
                    >
                        { t("console.identities.users.list.grid.4") }
                    </TableHeaderColumn>
                </List>
            </Panel.Body>
        </Panel>
    );
};

ListUsers.propTypes = {
    newHref: PropTypes.string.isRequired
};

export default ListUsers;
