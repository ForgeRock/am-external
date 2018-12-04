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
 * Copyright 2015-2018 ForgeRock AS.
 */

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";

import AdministeredRealmsHelper from "org/forgerock/openam/ui/admin/utils/AdministeredRealmsHelper";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import RealmHelper from "org/forgerock/openam/ui/common/util/RealmHelper";

export default {
    substitute (url) {
        return function () {
            var realm = AdministeredRealmsHelper.getCurrentRealm(),
                apiUrlBase = `${Constants.host}${Constants.context}/json${
                    (realm === "/" ? "" : RealmHelper.encodeRealm(realm))
                }`;

            return url.replace("__api__", apiUrlBase)
                .replace("__host__", Constants.host)
                .replace("__context__", Constants.context)
                .replace("__username__", Configuration.loggedUser.get("username"));
        };
    }
};
