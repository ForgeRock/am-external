/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { combineReducers } from "redux";

import java from "./java/index";
import remoteConsent from "./remoteConsent/index";
import soapSts from "./soapSts/index";
import softwarePublisher from "./softwarePublisher/index";
import web from "./web/index";

export default combineReducers({
    java,
    remoteConsent,
    soapSts,
    softwarePublisher,
    web
});
