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
import $ from "jquery";

import { getLanguage } from "org/forgerock/commons/ui/common/main/i18n/manager";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import detectiOS from "org/forgerock/commons/ui/common/util/detectiOS";
import ErrorsHandler from "org/forgerock/commons/ui/common/main/ErrorsHandler";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import SpinnerManager from "org/forgerock/commons/ui/common/main/SpinnerManager";

/**
 * @exports org/forgerock/commons/ui/common/main/ServiceInvoker
 */
const ServiceInvoker = {
    configuration: {
        defaultHeaders: {}
    }
};

ServiceInvoker.setAnonymousDefaultHeaders = function () {
    ServiceInvoker.configuration.defaultHeaders = {
        "X-Password": "anonymous",
        "X-Username": "anonymous",
        "X-NoSession": true
    };
};

ServiceInvoker.removeAnonymousDefaultHeaders = function () {
    ServiceInvoker.configuration.defaultHeaders = {};
};

/**
 * Performs a REST service call.
 * <p>
 * If a <tt>dataType</tt> of <tt>"json"</tt> is set on the options, the request has it's <tt>contentType</tt> set to
 * be <tt>"application/json"</tt> automatically.
 * <p>
 * Additional options can also be passed to control behaviour beyond what
 * {@link http://api.jquery.com/jquery.ajax|$.ajax()} is aware of:
 * <code><pre>
 * {
 *     suppressEvents: true // Default "false". Suppresses the ErrorsHandlers.
 * }
 * </code></pre>
 * @param  {object} options Options that will be passed to {@link http://api.jquery.com/jquery.ajax|$.ajax()}
 * @returns {Deferred} {@link http://api.jquery.com/Types/#jqXHR|jqXHR} Return value from call to
 *                          {@link http://api.jquery.com/jquery.ajax|$.ajax()}
 */
ServiceInvoker.restCall = function (options) {
    const successCallback = options.success;
    const errorCallback = options.error;
    const hasDataType = options.hasOwnProperty("dataType");
    const isJSONRequest = hasDataType && options.dataType === "json";
    const promise = $.Deferred();

    const ie11 = !!window.MSInputMethodContext && !!document.documentMode;

    const resolveHandler = function () {
        promise.resolve(...arguments);
    };

    const rejectHandler = function (jqXHR) {
        if (!options.suppressSpinner) {
            SpinnerManager.hideSpinner();
        }

        if (options.suppressEvents) {
            if (errorCallback) { errorCallback(jqXHR); }
            promise.reject(...arguments);
        } else if (jqXHR.status === 401 && !ErrorsHandler.matchError({ status:401 }, options.errorsHandlers)) {
            // attempt to handle session timeout errors (indicated by a 401)
            // gracefully, by deferring the failure until after the user has
            // had a chance to reauthenticate. After they have successfully
            // logged-in, resubmit their original request. Only do this if there
            // isn't an errorsHandler for 401 included in the request.
            EventManager.sendEvent(Constants.EVENT_SHOW_LOGIN_DIALOG);
        } else {
            ErrorsHandler.handleError (
                $.extend({}, jqXHR, { type: this.type }),
                options.errorsHandlers
            );
            if (errorCallback) { errorCallback(jqXHR); }
            promise.reject(...arguments);
        }
    };

    /**
     * Logic to cover two scenarios:
     * 1. If we don't have a dataType we default to JSON
     * 2. If the dataType is "json" we ensure the correct value for contentType has been set
     */
    if (!hasDataType || isJSONRequest) {
        options.dataType = "json";
        options.contentType = "application/json";
    }

    ServiceInvoker.applyDefaultHeadersIfNecessary(options, ServiceInvoker.configuration.defaultHeaders);

    if (!options.suppressSpinner) {
        SpinnerManager.showSpinner();
    }

    options.success = function (data, textStatus, jqXHR) {
        if (data && data.error) {
            if (!options.suppressEvents) {
                ErrorsHandler.handleError (
                    $.extend({}, data, { type: this.type }),
                    options.errorsHandlers
                );
            }

            if (errorCallback) { errorCallback(data); }
        } else if (successCallback) {
            successCallback(data, jqXHR);
        }

        if (!options.suppressSpinner) {
            SpinnerManager.hideSpinner();
        }
    };

    // The error handling function passed into restCall will be executed
    // (if defined) as part of the rejectHandler above. A provided success
    // handler will execute as normal, because there is no special logic
    // needed around handling successful requests.
    delete options.error;

    options.xhrFields = {
        /**
         * Useful for CORS requests, should we be accessing a remote endpoint.
         * @see http://www.html5rocks.com/en/tutorials/cors/#toc-withcredentials
         */
        withCredentials: true
    };

    /**
     * This is the jQuery default value for this header, but unless manually specified (like so) it won't be
     * included in CORS requests.
     */
    options.headers["X-Requested-With"] = "XMLHttpRequest";

    /**
     * Default to disabled caching for all AJAX requests. Can be overriden in the rare cases when caching AJAX is
     * needed
     */
    if (!_.has(options.headers, "Cache-Control")) {
        options.headers["Cache-Control"] = "no-cache";
    }

    if (!_.has(options.headers, "Accept-Language")) {
        options.headers["Accept-Language"] = getLanguage();
    }

    // Avoids WebKit bug. See OPENAM-9610
    if (_.inRange(detectiOS(), 9, 10)) {
        options.async = false;
    }

    // This fix is needed for IE11. If you make an ajax request at the same time
    // as certain events (like paste) the event can conflict with the ajax request resulting an
    // access denied from the ajax request. Putting the ajax request in a settimeout forces
    // the ajax request to occur at the end of the stack, preventing the access denied error.
    // https://stackoverflow.com/questions/26891783/ie-11-error-access-is-denied-xmlhttprequest
    if (ie11) {
        setTimeout(() => {
            $.ajax(options).then(resolveHandler, rejectHandler);
        }, 1);
    } else {
        $.ajax(options).then(resolveHandler, rejectHandler);
    }

    return promise;
};

/**
 * Test TODO create test using below formula
 * var x = {headers:{"a": "a"},b:"b"};
 * require("org/forgerock/commons/ui/common/main/ServiceInvoker").applyDefaultHeadersIfNecessary(x, {a:"x",b:"b"});
 * y ={};
 * require("org/forgerock/commons/ui/common/main/ServiceInvoker").applyDefaultHeadersIfNecessary(y, {a:"c",d:"c"});
 */
ServiceInvoker.applyDefaultHeadersIfNecessary = function (options, defaultHeaders) {
    let oneHeaderName;

    if (!defaultHeaders) { return; }

    if (options.headers) {
        for (oneHeaderName in defaultHeaders) {
            if (options.headers[oneHeaderName] === undefined) {
                options.headers[oneHeaderName] = defaultHeaders[oneHeaderName];
            }
        }
    } else {
        options.headers = defaultHeaders;
    }
};

export default ServiceInvoker;
