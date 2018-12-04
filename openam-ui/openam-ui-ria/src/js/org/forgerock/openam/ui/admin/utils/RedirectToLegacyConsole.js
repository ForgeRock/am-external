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
 * Copyright 2017-2018 ForgeRock AS.
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";

const service = new AbstractDelegate(`${Constants.host}${Constants.context}`);

const getJATOSession = (realm) => service.serviceCall({
    url: `/realm/RMRealm?RMRealm.tblDataActionHref=${realm}&requester=XUI`,
    dataType: "html"
}).then((data) => {
    const session = data.match(/jato.pageSession=(.*?)"/)[1];

    if (!session) {
        window.location.href = `${Constants.context}/UI/Login?service=adminconsoleservice`;
    }

    return session;
});
const redirectToGlobalTab = (tabIndex) => getJATOSession("/").then((session) => {
    window.location.href = `${Constants.context}/task/Home?Home.tabCommon.TabHref=${
        tabIndex}&jato.pageSession=${session}&requester=XUI`;
});

export const realm = {
};

export const global = {
    federation: () => redirectToGlobalTab(2)
};

export function commonTasks (realm, link) {
    const query = link.indexOf("?") === -1 ? "?" : "&";
    window.location.href = `${Constants.context}/${link}${query}realm=${encodeURIComponent(realm)}`;
}
