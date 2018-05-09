/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash"
], (_) => {
    const backstageDocsUrl = "https://backstage.forgerock.com/#!/docs/openam/13.5/";

    return {
        backstage: {
            authz: _.mapValues({
                policySets: "#configure-apps-with-console",
                policies: "#configure-policies-with-console",
                resourceTypes: "#configure-resource-types-with-console"
            }, (hash) => `${backstageDocsUrl}admin-guide${hash}`),
            config: {
                services : `${backstageDocsUrl}reference#chap-config-ref`
            }
        }
    };
});
