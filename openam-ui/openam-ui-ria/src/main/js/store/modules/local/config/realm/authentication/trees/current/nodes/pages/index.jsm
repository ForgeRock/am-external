/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { combineReducers } from "redux";

import childnodes, { propType as childnodesType } from "./childnodes";
import positions, { propType as positionsType } from "./positions";

export const propType = {
    childnodes: childnodesType.childnodes,
    positions: positionsType.positions
};

export default combineReducers({
    childnodes,
    positions
});
