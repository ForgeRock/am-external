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
 * Copyright 2011-2019 ForgeRock AS.
 */

import _ from "lodash";

import AMConfig from "config/process/AMConfig";
import CommonConfig from "config/process/CommonConfig";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";

const callRegisterListenerFromConfig = ({ processDescription, startEvent }) => {
    EventManager.registerListener(startEvent, (event) => processDescription(event));
};

const ProcessConfiguration = {};

ProcessConfiguration.loadEventHandlers = function () {
    _.map([...AMConfig, ...CommonConfig], callRegisterListenerFromConfig);
    EventManager.sendEvent(Constants.EVENT_READ_CONFIGURATION_REQUEST);
};

export default ProcessConfiguration;
