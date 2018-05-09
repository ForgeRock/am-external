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

import EditUserGeneral from "./EditUserGeneral";
import EditUserGroups from "./EditUserGroups";
import ListUserServicesContainer from "./services/list/ListUserServicesContainer";

import PageHeader from "components/PageHeader";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const EditUser = ({ router }) => {
    const identity = decodeURIComponent(router.params[1]);
    return (
        <div>
            <PageHeader icon="address-card" title={ identity } type={ t("console.identities.users.edit.type") } />
            <Tabs animation defaultActiveKey={ 1 } mountOnEnter unmountOnExit >
                <Tab eventKey={ 1 } title={ t("console.identities.users.edit.tabs.0") }>
                    <EditUserGeneral />
                </Tab>
                <Tab eventKey={ 2 } title={ t("console.identities.users.edit.tabs.1") }>
                    <ListUserServicesContainer />
                </Tab>
                <Tab eventKey={ 3 } title={ t("console.identities.users.edit.tabs.2") }>
                    <EditUserGroups />
                </Tab>
            </Tabs>
        </div>
    );
};

EditUser.propTypes = {
    router: withRouterPropType
};

export default withRouter(EditUser);
