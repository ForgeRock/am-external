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
import PageHeader from "components/PageHeader";

const ListDataStores = (props) => {
    return (
        <div>
            <PageHeader title={ t("console.datastores.title") } />
            <Panel>
                <List
                    { ...omit(props, "children") }
                    addButton={ {
                        title: t("console.datastores.list.callToAction.button"),
                        href: props.newHref
                    } }
                    description={ t("console.datastores.list.callToAction.description") }
                    title={ t("console.datastores.list.callToAction.title") }
                >
                    <TableHeaderColumn dataField="_id" dataFormat={ IconCell("fa-database") } dataSort isKey>
                        { t("console.datastores.list.grid.0") }
                    </TableHeaderColumn>
                    <TableHeaderColumn
                        dataField="_type"
                        dataFormat={ dataFieldObjectPath((cell) => cell, "name") }
                    >
                        { t("console.datastores.list.grid.1") }
                    </TableHeaderColumn>
                </List>
            </Panel>
        </div>
    );
};

ListDataStores.propTypes = {
    newHref: PropTypes.string.isRequired
};

export default ListDataStores;
