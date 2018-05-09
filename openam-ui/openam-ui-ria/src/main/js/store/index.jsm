/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { createStore, compose } from "redux";

import rootReducer from "./modules/index";

const defaultState = {};
const enhancers = compose(window.devToolsExtension ? window.devToolsExtension() : (f) => f);

const store = createStore(rootReducer, defaultState, enhancers);

export default store;
