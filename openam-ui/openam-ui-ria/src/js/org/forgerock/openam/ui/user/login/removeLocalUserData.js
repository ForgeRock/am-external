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
 * Copyright 2019 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/user/login/removeLocalUserData
 */

import { removeRealm } from "store/modules/local/session";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import SessionValidator from "org/forgerock/openam/ui/common/sessions/SessionValidator";
import store from "store";

const removeLocalUserData = () => {
    Configuration.setProperty("loggedUser", null);
    Configuration.globalData.auth = {};
    SessionValidator.stop();
    store.dispatch(removeRealm());
    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });
};

export default removeLocalUserData;
