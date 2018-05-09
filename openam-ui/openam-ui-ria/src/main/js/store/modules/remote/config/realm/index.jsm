/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { combineReducers } from "redux";

import applications from "./applications/index";
import authentication from "./authentication/index";
import datastores from "./datastores/index";
import identities from "./identities/index";
import sts from "./sts/index";

export default combineReducers({
    applications,
    authentication,
    datastores,
    identities,
    sts
});
