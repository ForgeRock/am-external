/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/util/URIUtils"
], (URIUtils) => {
    return {
        /**
         * Extracts from the URI hash fragment and returns realm which is being currently edited by administrator
         * @returns {String} current realm decoded
         */
        getCurrentRealm () {
            return decodeURIComponent(URIUtils.getCurrentFragment().split("/")[1]);
        }
    };
});
