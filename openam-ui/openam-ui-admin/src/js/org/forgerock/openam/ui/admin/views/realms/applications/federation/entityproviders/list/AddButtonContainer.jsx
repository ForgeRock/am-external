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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import { t } from "i18next";
import React from "react";

import AddButton from "components/AddButton";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

let AddButtonContainer = (props) => {
    const realm = props.router.params[0];
    const newHrefHosted = Router.getLink(
        Router.configuration.routes.realmsApplicationsFederationEntityProvidersNewHosted, [
            encodeURIComponent(realm)
        ]);
    const newHrefRemote = Router.getLink(
        Router.configuration.routes.realmsApplicationsFederationEntityProvidersNewRemote, [
            encodeURIComponent(realm)
        ]);

    const menuItems = [{
        href: `#${newHrefHosted}`,
        title: t("console.applications.federation.entityProviders.list.callToAction.button.hosted")
    }, {
        href: `#${newHrefRemote}`,
        title: t("console.applications.federation.entityProviders.list.callToAction.button.remote")
    }];

    return (
        <AddButton title={ t("console.applications.federation.entityProviders.list.callToAction.button.title") }>
            { menuItems }
        </AddButton>
    );
};

AddButtonContainer.propTypes = {
    router: withRouterPropType
};

AddButtonContainer = withRouter(AddButtonContainer);

export default AddButtonContainer;
