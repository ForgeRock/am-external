/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Tab, Tabs } from "react-bootstrap";
import { t } from "i18next";
import React from "react";

import ListGroupsContainer from "./groups/list/ListGroupsContainer";
import ListUsersContainer from "./users/list/ListUsersContainer";
import PageHeader from "components/PageHeader";

const Identities = () => {
    return (
        <div>
            <PageHeader title={ t("console.identities.title") } />
            <Tabs animation={ false } defaultActiveKey={ 1 } mountOnEnter unmountOnExit>
                <Tab eventKey={ 1 } title={ t("console.identities.tabs.0") }>
                    <ListUsersContainer />
                </Tab>
                <Tab eventKey={ 2 } title={ t("console.identities.tabs.1") }>
                    <ListGroupsContainer />
                </Tab>
            </Tabs>
        </div>
    );
};

export default Identities;
