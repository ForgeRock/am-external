/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { combineReducers } from "redux";

import instances from "./instances";
import schema from "./schema";
import template from "./template";

export default combineReducers({
    instances,
    schema,
    template
});