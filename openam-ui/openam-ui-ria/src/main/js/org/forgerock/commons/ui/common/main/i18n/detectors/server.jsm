/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import _ from "lodash";

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";

export default {
    name: "server",
    lookup () {
        return _.get(Configuration, "globalData.lang");
    }
};
