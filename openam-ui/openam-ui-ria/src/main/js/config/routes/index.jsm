/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import AccessPermissionConfig from "./AccessPermissionConfig";
import AMRoutesConfig from "./AMRoutesConfig";
import CommonRoutesConfig from "./CommonRoutesConfig";
import GlobalRoutes from "./admin/GlobalRoutes";
import RealmsRoutes from "./admin/RealmsRoutes";
import UMARoutes from "./user/UMARoutes";
import UserRoutesConfig from "./UserRoutesConfig";

export default {
    ...AccessPermissionConfig,
    ...AMRoutesConfig,
    ...CommonRoutesConfig,
    ...GlobalRoutes,
    ...RealmsRoutes,
    ...UMARoutes,
    ...UserRoutesConfig
};
