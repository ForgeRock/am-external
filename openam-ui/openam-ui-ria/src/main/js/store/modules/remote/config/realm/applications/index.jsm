/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { combineReducers } from "redux";

import agents from "./agents/index";
import federation from "./federation/index";
import oauth2 from "./oauth2/index";

export default combineReducers({
    agents,
    federation,
    oauth2
});
