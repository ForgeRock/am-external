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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";
import $ from "jquery";

const EventManager = {};

const eventRegistry = {};

const subscriptions = {};

EventManager.sendEvent = function (eventId, event) {
    return $.when(..._.map(eventRegistry[eventId], (eventHandler) => {
        const promise = $.Deferred();
        window.setTimeout(() => {
            $.when(eventHandler(event)).always(promise.resolve);
        });
        return promise;
    })).then(
        () => {
            let promise;
            if (_.has(subscriptions, eventId)) {
                promise = subscriptions[eventId];
                delete subscriptions[eventId];
                promise.resolve();
            }
            return;
        }
    );
};

EventManager.registerListener = function (eventId, callback) {
    if (_.has(eventRegistry, eventId)) {
        eventRegistry[eventId].push(callback);
    } else {
        eventRegistry[eventId] = [callback];
    }
};

EventManager.unregisterListener = function (eventId, callbackToRemove) {
    if (_.has(eventRegistry, eventId)) {
        if (callbackToRemove === undefined) {
            delete eventRegistry[eventId];
        } else {
            eventRegistry[eventId] = _.omitBy(eventRegistry[eventId], (callback) => {
                return callback === callbackToRemove;
            });
        }
    }
};

/**
 * Returns a promise that will be resolved the next time the provided eventId has completed processing.
 */
EventManager.whenComplete = function (eventId) {
    if (!_.has(subscriptions, eventId)) {
        subscriptions[eventId] = $.Deferred();
    }
    return subscriptions[eventId];
};

export default EventManager;
