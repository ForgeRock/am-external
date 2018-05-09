/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
            <Tabs defaultActiveKey={ 1 } mountOnEnter unmountOnExit>
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
