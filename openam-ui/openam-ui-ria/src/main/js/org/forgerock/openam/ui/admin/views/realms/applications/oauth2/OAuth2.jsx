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

import ListOAuth2ClientsContainer from "./clients/list/ListOAuth2ClientsContainer";
import ListOAuth2GroupsContainer from "./groups/list/ListOAuth2GroupsContainer";
import PageHeader from "components/PageHeader";

const OAuth2 = () => {
    return (
        <div>
            <PageHeader title={ t("console.applications.oauth2.title") } />
            <Tabs defaultActiveKey={ 1 } mountOnEnter unmountOnExit>
                <Tab eventKey={ 1 } title={ t("console.applications.oauth2.tabs.0") }>
                    <ListOAuth2ClientsContainer />
                </Tab>
                <Tab eventKey={ 2 } title={ t("console.applications.oauth2.tabs.1") }>
                    <ListOAuth2GroupsContainer />
                </Tab>
            </Tabs>
        </div>
    );
};

export default OAuth2;
