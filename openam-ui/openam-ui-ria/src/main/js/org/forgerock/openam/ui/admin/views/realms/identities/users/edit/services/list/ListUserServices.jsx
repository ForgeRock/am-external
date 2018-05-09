/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Panel } from "react-bootstrap";
import { map, omit } from "lodash";
import { t } from "i18next";
import { TableHeaderColumn } from "react-bootstrap-table";
import PropTypes from "prop-types";
import React from "react";
import Router from "org/forgerock/commons/ui/common/main/Router";

import IconCell from "components/table/cells/IconCell";
import List from "components/list/List";

const nameFormatter = (cell, row) => IconCell("fa-plug")(row.name);
const ListUserServices = (props) => {
    const menuItems = map(props.creatables, (creatable) => ({
        href: `#${Router.getLink(
            Router.configuration.routes.realmsIdentitiesUsersServicesNew,
            map([props.realm, props.userId, creatable._id], encodeURIComponent)
        )}`,
        title: creatable.name
    }));
    return (
        <Panel className="fr-panel-tab">
            <List
                { ...omit(props, "children") }
                addButton={ {
                    menuItems,
                    title: t("console.identities.users.edit.services.list.callToAction.button")
                } }
                description={ t("console.identities.users.edit.services.list.callToAction.description") }
                title={ t("console.identities.users.edit.services.list.callToAction.title") }
            >
                <TableHeaderColumn dataField="_id" dataFormat={ nameFormatter } dataSort isKey>
                    { t("console.identities.users.edit.services.list.grid.0") }
                </TableHeaderColumn>
            </List>
        </Panel>
    );
};

ListUserServices.propTypes = {
    creatables: PropTypes.arrayOf(PropTypes.object).isRequired,
    realm: PropTypes.string.isRequired,
    userId: PropTypes.string.isRequired
};

export default ListUserServices;
