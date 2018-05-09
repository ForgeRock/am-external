/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/realms/humanizeRealmPath
 */

/**
 * Given the absolute path the administered realm, this function will return the last fragment of the
 * path, or for the root realm the string, "Top Level Realm".
 * @param {String} realmPath The realm path to the administered realm
 * @returns {String} The last fragment of the given realm or the "Top Level Realm" for the root realm
 * @example
 *      humanizeRealmPath("/")
 *          => "Top Level Realm"
 *      humanizeRealmPath ("/Foo")
 *          => "Foo"
 *      humanizeRealmPath ("/Foo/Bar")
 *          => "Bar"
 */

import { last } from "lodash";
import { t } from "i18next";

const humanizeRealmPath = (realmPath) => {
    return realmPath === "/" ? t("console.common.topLevelRealm") : last(realmPath.split("/"));
};

export default humanizeRealmPath;
