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
 * Copyright 2017-2022 ForgeRock AS.
 */

import { omit } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import ArrayCell from "components/table/cells/ArrayCell";
import IconCell from "components/table/cells/IconCell";
import List from "components/list/List";
import StatusCell from "components/table/cells/StatusCell";

const ListCirclesOfTrust = (props) => {
    const columns = [{
        dataField: "_id",
        formatter: IconCell("fa-circle-o-notch"),
        sort: true,
        text: t("console.applications.federation.circlesoftrust.list.grid.0")
    }, {
        dataField: "trustedProviders",
        formatter: ArrayCell,
        text: t("console.applications.federation.circlesoftrust.list.grid.1")
    }, {
        dataField: "status",
        formatter: StatusCell,
        sort: true,
        text: t("console.applications.federation.circlesoftrust.list.grid.2")
    }];
    return (
        <Panel className="fr-panel-tab">
            <Panel.Body>
                <List
                    { ...omit(props, "children") }
                    addButton={ {
                        title: t("console.applications.federation.circlesoftrust.list.callToAction.button"),
                        href: props.newHref
                    } }
                    columns={ columns }
                    description={ t("console.applications.federation.circlesoftrust.list.callToAction.description") }
                    title={ t("console.applications.federation.circlesoftrust.list.callToAction.title") }
                />
            </Panel.Body>
        </Panel>
    );
};

ListCirclesOfTrust.propTypes = {
    newHref: PropTypes.string.isRequired
};

export default ListCirclesOfTrust;
