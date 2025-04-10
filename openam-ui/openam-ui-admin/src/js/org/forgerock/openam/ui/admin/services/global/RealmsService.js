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
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/global/RealmsService
 */

import _ from "lodash";
import { CRESTv2 } from "@forgerock/crest-js";
import { t } from "i18next";

import { addRealm, removeRealm, setRealms } from "store/modules/remote/realms";
import Base64 from "org/forgerock/commons/ui/common/util/Base64";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import middleware from "api/crest/middleware";
import SMSServiceUtils from "org/forgerock/openam/ui/admin/services/SMSServiceUtils";
import spinner from "api/crest/spinner";
import store from "store";
import url from "api/crest/url";

function getRealmPath (realm) {
    if (realm.parentPath === "/") {
        return realm.parentPath + realm.name;
    } else if (realm.parentPath) {
        return `${realm.parentPath}/${realm.name}`;
    } else {
        return "/";
    }
}

/**
 * This function encodes a path. It uses base64url encoding.
 * @param {string} path the path
 * @returns {string} the encoded path
 */
function encodePath (path) {
    return Base64.encodeUTF8(path)
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/, "");
}

const resource = new CRESTv2(
    url("/global-config/realms"), {
        middleware: [middleware]
    }
);

export const create = (body) => resource.create(body);
export const get = (id) => Promise.all([
    resource.action("schema"),
    resource.get(encodePath(id))
]).then(([schema, values]) => {
    store.dispatch(addRealm({ ...values }));
    return {
        schema: SMSServiceUtils.sanitizeSchema(schema),
        values
    };
});
export const getAll = () => spinner(resource.queryFilter()).then((response) => {
    store.dispatch(setRealms(response.result));
    response.result = _(response.result).map((realm) => ({
        ...realm,
        path: getRealmPath(realm)
    })).sortBy("path").value();
    return response;
});
export const remove = (id) => {
    const promise = resource.delete(encodePath(id));

    return promise.then((response) => {
        store.dispatch(removeRealm(response));
    }, (reason) => {
        if (reason.status === 409) {
            Messages.addMessage({
                message: t("console.realms.parentRealmCannotDeleted"),
                type: Messages.TYPE_DANGER
            });
        } else {
            return promise;
        }
    });
};
export const schema = () => Promise.all([
    resource.action("schema"),
    resource.action("template")
]).then(([schema, values]) => ({
    schema: SMSServiceUtils.sanitizeSchema(schema),
    values
}));
export const update = (body) => resource.update(encodePath(getRealmPath(body)), body).then((response) => {
    store.dispatch(addRealm(response));
    return response;
});
