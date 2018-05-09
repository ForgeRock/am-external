/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
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

const ListOAuth2Clients = (props) => {
    return (
        <Panel className="fr-panel-tab">
            <List
                { ...omit(props, "children") }
                addButton={ {
                    title: t("console.applications.oauth2.clients.list.callToAction.button"),
                    href: props.newHref
                } }
                description={ t("console.applications.oauth2.clients.list.callToAction.description") }
                title={ t("console.applications.oauth2.clients.list.callToAction.title") }
            >
                <TableHeaderColumn dataField="_id" dataFormat={ IconCell("fa-list-alt") } dataSort isKey>
                    { t("console.applications.oauth2.clients.list.grid.0") }
                </TableHeaderColumn>
                <TableHeaderColumn
                    dataField="coreOAuth2ClientConfig"
                    dataFormat={ dataFieldObjectPath((cell) => cell, "agentgroup") }
                    dataSort
                >
                    { t("console.applications.oauth2.clients.list.grid.1") }
                </TableHeaderColumn>
                <TableHeaderColumn
                    dataField="coreOAuth2ClientConfig"
                    dataFormat={ dataFieldObjectPath(StatusCell, "status") }
                    dataSort
                >
                    { t("console.applications.oauth2.clients.list.grid.2") }
                </TableHeaderColumn>
            </List>
        </Panel>
    );
};

ListOAuth2Clients.propTypes = {
    newHref: PropTypes.string.isRequired
};

export default ListOAuth2Clients;
