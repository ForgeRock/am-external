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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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
            <PageHeader title={ t("console.applications.oauth2.softwarePublisher.title") } />
            <Tabs defaultActiveKey={ 1 } id="softwarePublisherAgents" mountOnEnter unmountOnExit>
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
