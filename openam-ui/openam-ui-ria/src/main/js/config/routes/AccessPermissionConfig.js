/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([], () => {
    return {
        "unauthorized": {
            view: () => import("org/forgerock/commons/ui/common/UnauthorizedView.js"),
            url: ""
        },
        "forbidden": {
            view: () => import("org/forgerock/openam/ui/common/views/error/ForbiddenView.js"),
            url: /.*/
        }
    };
});
