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

import { identity, omit } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import { TableHeaderColumn } from "react-bootstrap-table";
import PropTypes from "prop-types";
import React, { Fragment } from "react";

import dataFieldObjectPath from "components/table/cells/dataFieldObjectPath";
import dataFormatReact from "components/table/cells/dataFormatReact";
import FontAwesomeIconCell from "components/table/cells/FontAwesomeIconCell";
import List from "components/list/List";
import PageHeader from "components/PageHeader";

const ListSecretStores = (props) => {
    return (
        <Fragment>
            <PageHeader title={ t("console.secretStores.list.title") } />
            <Panel>
                <Panel.Body>
                    <List
                        { ...omit(props, "children") }
                        addButton={ {
                            title: t("console.secretStores.list.callToAction.button"),
                            href: props.newHref
                        } }
                        description={ t("console.secretStores.list.callToAction.description") }
                        onDelete={ props.onDelete }
                        onRowClick={ props.onRowClick }
                        title={ t("console.secretStores.list.callToAction.title") }
                    >
                        <TableHeaderColumn
                            columnTitle={ identity }
                            dataField="_id"
                            dataFormat={ dataFormatReact(
                                <FontAwesomeIconCell icon="eye" />
                            ) }
                            dataSort
                        >
                            { t("console.secretStores.list.grid.0") }
                        </TableHeaderColumn>
                        <TableHeaderColumn
                            dataField="_type"
                            dataFormat={ dataFieldObjectPath(identity, "name") }
                        >
                            { t("console.secretStores.list.grid.1") }
                        </TableHeaderColumn>
                    </List>
                </Panel.Body>
            </Panel>
        </Fragment>
    );
};

ListSecretStores.propTypes = {
    newHref: PropTypes.string.isRequired,
    onDelete: PropTypes.func.isRequired,
    onRowClick: PropTypes.func.isRequired
};

export default ListSecretStores;