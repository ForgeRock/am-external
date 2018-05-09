/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { isEmpty, omit } from "lodash";
import { ButtonToolbar, Panel } from "react-bootstrap";
import { t } from "i18next";
import { TableHeaderColumn } from "react-bootstrap-table";
import PropTypes from "prop-types";
import React from "react";

import IconCell from "components/table/cells/IconCell";
import List from "components/list/List";
import AllAuthenticatedButton
    from "org/forgerock/openam/ui/admin/views/realms/identities/groups/list/AllAuthenticatedButton";

const ListGroups = (props) => {
    const allAuthenticatedToolbar = isEmpty(props.items)
        ? (
            <ButtonToolbar className="page-toolbar">
                <AllAuthenticatedButton />
            </ButtonToolbar>
        )
        : null;

    return (
        <Panel className="fr-panel-tab">
            { allAuthenticatedToolbar }
            <List
                { ...omit(props, "children") }
                addButton={ {
                    href: props.newHref,
                    title: t("console.identities.groups.list.callToAction.button")
                } }
                additionalButtons={ <AllAuthenticatedButton /> }
                description={ t("console.identities.groups.list.callToAction.description") }
                onPageChange={ props.onPageChange }
                title={ t("console.identities.groups.list.callToAction.title") }
            >
                <TableHeaderColumn dataField="username" dataFormat={ IconCell("fa-folder") } dataSort isKey>
                    { t("console.identities.groups.list.grid.0") }
                </TableHeaderColumn>
            </List>
        </Panel>
    );
};

ListGroups.propTypes = {
    items: PropTypes.arrayOf(PropTypes.object).isRequired,
    newHref: PropTypes.string.isRequired,
    onPageChange: PropTypes.func.isRequired
};

export default ListGroups;
