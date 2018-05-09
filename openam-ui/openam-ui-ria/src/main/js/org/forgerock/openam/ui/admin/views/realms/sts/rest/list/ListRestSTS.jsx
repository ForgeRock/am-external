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

import IconCell from "components/table/cells/IconCell";
import List from "components/list/List";

const ListRestSTS = (props) => {
    return (
        <Panel className="fr-panel-tab">
            <List
                { ...omit(props, "children") }
                addButton={ {
                    title: t("console.sts.rest.list.callToAction.button"),
                    href: props.newHref
                } }
                description={ t("console.sts.rest.list.callToAction.description") }
                title={ t("console.sts.rest.list.callToAction.title") }
            >
                <TableHeaderColumn dataField="_id" dataFormat={ IconCell("fa-credit-card") } dataSort isKey>
                    { t("console.sts.rest.list.grid.0") }
                </TableHeaderColumn>
            </List>
        </Panel>
    );
};

ListRestSTS.propTypes = {
    newHref: PropTypes.string.isRequired
};

export default ListRestSTS;
