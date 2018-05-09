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
 * Copyright 2017 ForgeRock AS.
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
