/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";

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
