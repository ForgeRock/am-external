/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Alert, Panel, Tab, Tabs } from "react-bootstrap";
import { t } from "i18next";
import React from "react";

import Constants from "org/forgerock/commons/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import ListCirclesOfTrustContainer from "./circlesoftrust/list/ListCirclesOfTrustContainer";
import PageHeader from "components/PageHeader";

const onTabSelect = (eventKey) => {
    if (eventKey === 2) {
        EventManager.sendEvent(Constants.EVENT_REDIRECT_TO_JATO_FEDERATION);
    }
};

const Federation = () => {
    return (
        <div>
            <PageHeader title={ t("console.applications.federation.title") } />
            <Tabs animation={ false } defaultActiveKey={ 1 } onSelect={ onTabSelect }>
                <Tab eventKey={ 1 } title={ t("console.applications.federation.tabs.0") }>
                    <ListCirclesOfTrustContainer />
                </Tab>
                <Tab eventKey={ 2 } title={ t("console.applications.federation.tabs.1") }>
                    <Panel className="fr-panel-tab">
                        <Alert bsStyle="info">
                            <p>{ t("common.form.redirecting") }</p>
                        </Alert>
                    </Panel>
                </Tab>
            </Tabs>
        </div>
    );
};

export default Federation;
