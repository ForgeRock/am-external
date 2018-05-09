/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
            <List
                { ...omit(props, "children") }
                addButton={ {
                    href: props.newHref,
                    title: t("console.identities.users.list.callToAction.button")
                } }
                description={ t("console.identities.users.list.callToAction.description") }
                onPageChange={ props.onPageChange }
                title={ t("console.identities.users.list.callToAction.title") }
            >
                <TableHeaderColumn dataField="username" dataFormat={ IconCell("fa-address-card") } dataSort isKey>
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
        </Panel>
    );
};

ListUsers.propTypes = {
    newHref: PropTypes.string.isRequired,
    onPageChange: PropTypes.func.isRequired
};

export default ListUsers;
