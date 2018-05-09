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
