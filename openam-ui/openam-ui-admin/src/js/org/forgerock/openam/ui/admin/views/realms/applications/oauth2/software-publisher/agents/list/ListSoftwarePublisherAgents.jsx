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

import { identity, omit } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import dataFormatReact from "components/table/cells/dataFormatReact";
import FontAwesomeIconCell from "components/table/cells/FontAwesomeIconCell";
import List from "components/list/List";

const ListSoftwarePublisherAgents = (props) => {
    const columns = [{
        title: identity,
        dataField: "_id",
        formatter: dataFormatReact(
            <FontAwesomeIconCell icon="male" />
        ),
        sort: true,
        text: t("console.applications.oauth2.softwarePublisher.agents.list.grid.0")
    }, {
        dataField: "agentgroup",
        sort: true,
        text: t("console.applications.oauth2.softwarePublisher.agents.list.grid.1")
    }];
    return (
        <Panel className="fr-panel-tab">
            <Panel.Body>
                <List
                    { ...omit(props, "children") }
                    addButton={ {
                        title: t("console.applications.oauth2.softwarePublisher.agents.list.callToAction.button"),
                        href: props.newHref
                    } }
                    columns={ columns }
                    description={
                        t("console.applications.oauth2.softwarePublisher.agents.list.callToAction.description")
                    }
                    title={ t("console.applications.oauth2.softwarePublisher.agents.list.callToAction.title") }
                />
            </Panel.Body>
        </Panel>
    );
};

ListSoftwarePublisherAgents.propTypes = {
    newHref: PropTypes.string.isRequired
};

export default ListSoftwarePublisherAgents;
