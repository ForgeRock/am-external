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

import { Grid, ListGroup, ListGroupItem, Panel } from "react-bootstrap";
import { identity, omit } from "lodash";
import { t } from "i18next";
import { TableHeaderColumn } from "react-bootstrap-table";
import Block from "components/Block";
import PropTypes from "prop-types";
import React from "react";

import dataFieldObjectPath from "components/table/cells/dataFieldObjectPath";
import dataFormatReact from "components/table/cells/dataFormatReact";
import FontAwesomeIconCell from "components/table/cells/FontAwesomeIconCell";
import List from "components/list/List";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";

const ListGlobalSecretStores = ({ singletonStoreTypes, ...restProps }) => {
    const coreAttributesHref = `#${Router.getLink(Router.configuration.routes.editSecretStoresCoreAttributes)}`;
    const newStoreHref = `#${Router.getLink(Router.configuration.routes.newGlobalSecretStores)}`;
    const singletonStoreListItems = singletonStoreTypes.map((storeType) => (
        <ListGroupItem
            href={ `#${Router.getLink(
                Router.configuration.routes.editGlobalSingletonSecretStores,
                [storeType._id]
            )}` }
            key={ storeType._id }
        >
            { storeType.name }
        </ListGroupItem>
    ));

    return (
        <Grid>
            <PageHeader title={ t("console.secretStores.title") } />
            <Panel>
                <Panel.Body>
                    <Block
                        description={ t("console.secretStores.core.description") }
                        header={ t("console.secretStores.core.title") }
                    >
                        <ListGroup>
                            <ListGroupItem href={ coreAttributesHref }>
                                { t("console.secretStores.core.coreAttributes") }
                            </ListGroupItem>
                        </ListGroup>
                    </Block>
                    <Block
                        description={ t("console.secretStores.singletons.description") }
                        header={ t("console.secretStores.singletons.title") }
                    >
                        <ListGroup>
                            { singletonStoreListItems }
                        </ListGroup>
                    </Block>
                    <Block
                        description={ t("console.secretStores.list.global-description") }
                        header={ t("console.secretStores.list.title") }
                    >
                        <List
                            { ...omit(restProps, "children") }
                            addButton={ {
                                title: t("console.secretStores.list.callToAction.button"),
                                href: newStoreHref
                            } }
                            description={ t("console.secretStores.list.callToAction.description") }
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
                    </Block>
                </Panel.Body>
            </Panel>
        </Grid>
    );
};

ListGlobalSecretStores.propTypes = {
    isFetching: PropTypes.bool.isRequired,
    items: PropTypes.arrayOf(PropTypes.shape({
        _id: PropTypes.string.isRequired,
        _type: PropTypes.shape({
            name: PropTypes.string.isRequired
        })
    })),
    onDelete: PropTypes.func.isRequired,
    onRowClick: PropTypes.func.isRequired,
    singletonStoreTypes: PropTypes.arrayOf(PropTypes.shape({
        _id: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired
    }))
};

export default ListGlobalSecretStores;
