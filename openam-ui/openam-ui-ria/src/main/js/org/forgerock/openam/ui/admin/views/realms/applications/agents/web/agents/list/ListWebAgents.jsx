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

const ListWebAgents = (props) => {
    return (
        <Panel className="fr-panel-tab">
            <List
                { ...omit(props, "children") }
                addButton={ {
                    title: t("console.applications.agents.web.agents.list.callToAction.button"),
                    href: props.newHref
                } }
                description={ t("console.applications.agents.web.agents.list.callToAction.description") }
                title={ t("console.applications.agents.web.agents.list.callToAction.title") }
            >
                <TableHeaderColumn dataField="_id" dataFormat={ IconCell("fa-male") } dataSort isKey>
                    { t("console.applications.agents.web.agents.list.grid.0") }
                </TableHeaderColumn>
                <TableHeaderColumn
                    dataField="globalWebAgentConfig"
                    dataFormat={ dataFieldObjectPath((cell) => cell, "agentgroup") }
                    dataSort
                >
                    { t("console.applications.agents.web.agents.list.grid.1") }
                </TableHeaderColumn>
                <TableHeaderColumn
                    dataField="globalWebAgentConfig"
                    dataFormat={ dataFieldObjectPath((cell) => cell, "repositoryLocation") }
                    dataSort
                >
                    { t("console.applications.agents.web.agents.list.grid.2") }
                </TableHeaderColumn>
                <TableHeaderColumn
                    dataField="globalWebAgentConfig"
                    dataFormat={ dataFieldObjectPath(StatusCell, "status") }
                    dataSort
                >
                    { t("console.applications.agents.web.agents.list.grid.3") }
                </TableHeaderColumn>
            </List>
        </Panel>
    );
};

ListWebAgents.propTypes = {
    newHref: PropTypes.string.isRequired
};

export default ListWebAgents;
