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
 * Copyright 2018-2019 ForgeRock AS.
 */

import { Panel } from "react-bootstrap";
import { identity, map, omit } from "lodash";
import { t } from "i18next";
import { TableHeaderColumn } from "react-bootstrap-table";
import PropTypes from "prop-types";
import React from "react";
import Router from "org/forgerock/commons/ui/common/main/Router";

import dataFormatReact from "components/table/cells/dataFormatReact";
import FontAwesomeIconCell from "components/table/cells/FontAwesomeIconCell";
import List from "components/list/List";

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
            <Panel.Body>
                <List
                    { ...omit(props, "children") }
                    addButton={ {
                        menuItems,
                        title: t("console.identities.users.edit.services.list.callToAction.button")
                    } }
                    description={ t("console.identities.users.edit.services.list.callToAction.description") }
                    title={ t("console.identities.users.edit.services.list.callToAction.title") }
                >
                    <TableHeaderColumn
                        columnTitle={ identity }
                        dataField="name"
                        dataFormat={ dataFormatReact(
                            <FontAwesomeIconCell icon="plug" />
                        ) }
                        dataSort
                    >

                        { t("console.identities.users.edit.services.list.grid.0") }
                    </TableHeaderColumn>
                </List>
            </Panel.Body>
        </Panel>
    );
};

ListUserServices.propTypes = {
    creatables: PropTypes.arrayOf(PropTypes.object).isRequired,
    realm: PropTypes.string.isRequired,
    userId: PropTypes.string.isRequired
};

export default ListUserServices;
