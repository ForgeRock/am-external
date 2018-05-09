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
 * Copyright 2018 ForgeRock AS.
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
