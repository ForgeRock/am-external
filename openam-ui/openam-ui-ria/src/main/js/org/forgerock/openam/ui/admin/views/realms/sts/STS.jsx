/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Tab, Tabs } from "react-bootstrap";
import { t } from "i18next";
import React from "react";

import ListRestSTSContainer from "./rest/list/ListRestSTSContainer";
import ListSoapSTSContainer from "./soap/list/ListSoapSTSContainer";
import PageHeader from "components/PageHeader";

const STS = () => {
    return (
        <div>
            <PageHeader title={ t("console.sts.title") } />
            <Tabs animation={ false } defaultActiveKey={ 1 }>
                <Tab eventKey={ 1 } title={ t("console.sts.tabs.0") }>
                    <ListRestSTSContainer />
                </Tab>
                <Tab eventKey={ 2 } title={ t("console.sts.tabs.1") }>
                    <ListSoapSTSContainer />
                </Tab>
            </Tabs>
        </div>
    );
};

export default STS;
