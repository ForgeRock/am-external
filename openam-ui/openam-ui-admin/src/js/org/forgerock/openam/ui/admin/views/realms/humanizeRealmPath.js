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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { last } from "lodash";
import { t } from "i18next";

/**
 * @module org/forgerock/openam/ui/admin/views/realms/humanizeRealmPath
 */

/**
 * Given the absolute path the administered realm, this function will return the last fragment of the
 * path, or for the root realm the string, "Top Level Realm".
 * @param {string} realmPath The realm path to the administered realm
 * @returns {string} The last fragment of the given realm or the "Top Level Realm" for the root realm
 * @example
 *      humanizeRealmPath("/")
 *          => "Top Level Realm"
 *      humanizeRealmPath ("/Foo")
 *          => "Foo"
 *      humanizeRealmPath ("/Foo/Bar")
 *          => "Bar"
 */
const humanizeRealmPath = (realmPath) => {
    return realmPath === "/" ? t("console.common.topLevelRealm") : last(realmPath.split("/"));
};

export default humanizeRealmPath;