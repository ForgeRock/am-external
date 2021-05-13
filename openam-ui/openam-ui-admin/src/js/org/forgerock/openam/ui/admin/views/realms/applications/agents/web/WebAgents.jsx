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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { Tab, Tabs } from "react-bootstrap";
import { t } from "i18next";
import React from "react";

import ListWebAgentsContainer from "./agents/list/ListWebAgentsContainer";
import ListWebAgentGroupsContainer from "./groups/list/ListWebAgentGroupsContainer";
import PageHeader from "components/PageHeader";

const WebAgents = () => {
    return (
        <div>
            <PageHeader title={ t("console.applications.agents.web.title") } />
            <Tabs defaultActiveKey={ 1 } id="webAgents" mountOnEnter unmountOnExit>
                <Tab eventKey={ 1 } title={ t("console.applications.agents.common.tabs.0") }>
                    <ListWebAgentsContainer />
                </Tab>
                <Tab eventKey={ 2 } title={ t("console.applications.agents.common.tabs.1") }>
                    <ListWebAgentGroupsContainer />
                </Tab>
            </Tabs>
        </div>
    );
};

export default WebAgents;
