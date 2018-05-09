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

import IconCell from "components/table/cells/IconCell";
import List from "components/list/List";
import PageHeader from "components/PageHeader";

const ListTrees = (props) => {
    return (
        <div>
            <PageHeader title={ t("console.authentication.trees.list.title") } />
            <Panel>
                <List
                    { ...omit(props, "children") }
                    addButton={ {
                        title: t("console.authentication.trees.list.callToAction.button"),
                        href: props.newHref
                    } }
                    description={ t("console.authentication.trees.list.callToAction.description") }
                    title={ t("console.authentication.trees.list.callToAction.title") }
                >
                    <TableHeaderColumn dataField="_id" dataFormat={ IconCell("fa-tree") } dataSort isKey>
                        { t("console.authentication.trees.list.grid.0") }
                    </TableHeaderColumn>
                </List>
            </Panel>
        </div>
    );
};

ListTrees.propTypes = {
    newHref: PropTypes.string.isRequired
};

export default ListTrees;
