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

import ListSoftwarePublisherAgentsContainer from "./agents/list/ListSoftwarePublisherAgentsContainer";
import ListSoftwarePublisherAgentGroupsContainer from "./groups/list/ListSoftwarePublisherAgentGroupsContainer";
import PageHeader from "components/PageHeader";

const SoftwarePublisherAgents = () => {
    return (
        <div>
            <PageHeader title={ t("console.applications.agents.softwarePublisher.title") } />
            <Tabs defaultActiveKey={ 1 } mountOnEnter unmountOnExit>
                <Tab eventKey={ 1 } title={ t("console.applications.agents.common.tabs.0") }>
                    <ListSoftwarePublisherAgentsContainer />
                </Tab>
                <Tab eventKey={ 2 } title={ t("console.applications.agents.common.tabs.1") }>
                    <ListSoftwarePublisherAgentGroupsContainer />
                </Tab>
            </Tabs>
        </div>
    );
};

export default SoftwarePublisherAgents;
