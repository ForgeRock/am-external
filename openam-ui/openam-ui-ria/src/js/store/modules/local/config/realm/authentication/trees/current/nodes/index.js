/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017-2018 ForgeRock AS.
 */
import { combineReducers } from "redux";
import PropTypes from "prop-types";

import measurements, { propType as measurementsType } from "./measurements";
import pages, { propType as pagesType } from "./pages";
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
