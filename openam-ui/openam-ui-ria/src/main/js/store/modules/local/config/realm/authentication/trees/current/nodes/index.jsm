/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { combineReducers } from "redux";
import PropTypes from "prop-types";

import measurements, { propType as measurementsType } from "./measurements";
import pages, { propType as pagesType } from "./pages/index";
import properties, { propType as propertiesType } from "./properties";
import selected, { propType as selectedType } from "./selected";

export const propType = {
    measurements: measurementsType.measurements,
    pages: PropTypes.shape(pagesType).isRequired,
    properties: propertiesType.properties,
    selected: selectedType.selected
};

export default combineReducers({
    measurements,
    pages,
    properties,
    selected
});
