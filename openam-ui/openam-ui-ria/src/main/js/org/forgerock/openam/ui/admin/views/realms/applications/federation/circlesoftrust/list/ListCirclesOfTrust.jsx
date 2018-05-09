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

import ArrayCell from "components/table/cells/ArrayCell";
import IconCell from "components/table/cells/IconCell";
import List from "components/list/List";
import StatusCell from "components/table/cells/StatusCell";

const ListCirclesOfTrust = (props) => {
    return (
        <Panel className="fr-panel-tab">
            <List
                { ...omit(props, "children") }
                addButton={ {
                    title: t("console.applications.federation.circlesoftrust.list.callToAction.button"),
                    href: props.newHref
                } }
                description={ t("console.applications.federation.circlesoftrust.list.callToAction.description") }
                title={ t("console.applications.federation.circlesoftrust.list.callToAction.title") }
            >
                <TableHeaderColumn dataField="_id" dataFormat={ IconCell("fa-circle-o-notch") } dataSort isKey>
                    { t("console.applications.federation.circlesoftrust.list.grid.0") }
                </TableHeaderColumn>
                <TableHeaderColumn
                    dataField="trustedProviders"
                    dataFormat={ ArrayCell }
                >
                    { t("console.applications.federation.circlesoftrust.list.grid.1") }
                </TableHeaderColumn>
                <TableHeaderColumn
                    dataField="status"
                    dataFormat={ StatusCell }
                    dataSort
                >
                    { t("console.applications.federation.circlesoftrust.list.grid.2") }
                </TableHeaderColumn>
            </List>
        </Panel>
    );
};

ListCirclesOfTrust.propTypes = {
    newHref: PropTypes.string.isRequired
};

export default ListCirclesOfTrust;
