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
 * Copyright 2024-2025 Ping Identity Corporation.
 */

import { identity, omit } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import dataFormatReact from "components/table/cells/dataFormatReact";
import FontAwesomeIconCell from "components/table/cells/FontAwesomeIconCell";
import List from "components/list/List";
import PageHeader from "components/PageHeader";

const ListNodes = (props) => {
    const columns = [{
        title: identity,
        dataField: "displayName",
        formatter: dataFormatReact(
            <FontAwesomeIconCell icon="pencil-square" />
        ),
        sort: true,
        text: t("console.authentication.nodes.list.grid.0")
    }];

    return (
        <div>
            <PageHeader title={ t("console.authentication.nodes.list.title") } />
            <Panel>
                <Panel.Body>
                    <List
                        { ...omit(props, "children") }
                        addButton={ {
                            href: props.newHref,
                            title: t("console.authentication.nodes.list.callToAction.button")
                        } }
                        columns={ columns }
                        description={ t("console.authentication.nodes.list.callToAction.description") }
                        onExport={ props.onExport }
                        onImport={ props.onImport }
                        showExport
                        showImport
                        title={ t("console.authentication.nodes.list.callToAction.title") }
                    />
                </Panel.Body>
            </Panel>
        </div>
    );
};

ListNodes.propTypes = {
    newHref: PropTypes.string.isRequired,
    onExport: PropTypes.func.isRequired,
    onImport: PropTypes.func.isRequired
};

export default ListNodes;
