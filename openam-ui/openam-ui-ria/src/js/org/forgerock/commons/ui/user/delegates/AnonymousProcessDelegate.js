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
 * Copyright 2015-2018 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";

const AnonymousProcessDelegate = function (path, token, additional) {
    this.token = token;
    this.additional = additional || "";
    return AbstractDelegate.call(this, `${Constants.context}/${path}`);
};

/*
    This supports the special case of the "parameters" stage. This is a stage which may
    or may not be present as the first stage in the process. If it is present, then we
    want to take any input that was provided up-front and see if any of those input parameters
    match the parameters which were returned as "requirements" from the stage.

    For example, if the process was started like so:

        #register/&goto=http%3A%2F%2Fwww.example.com

    And the first stage is "parameters", and "parameters" lists "goto" as one of its
    known "requirements", then we submit {"goto": "http://www.example.com"} as the input and
    advance to the next stage, returning the next stage requirements from this function.

    If the input does not contain the fields specified by "parameters", then an empty JSON map
    is submitted and the next stage requirements are returned.

    If there are other details provided in the input which were not specified by "parameters",
    those details will be submitted as input to the following stage.

    If the first stage is not "parameters" then the promise is resolved with the original
    "response" value.

    The intent is that the "parameters" stage is always transparently handled by this delegate.
    No external consumer of the "start" or "submit" functions should know anything about it.
    @param {Object} response - the output produced from the most recent call to the process
    @param {Object} input - candidate data to submit to the "parameters" stage, if necessary
*/
const submitParamsIfNeeded = function (response, input) {
    const promise = $.Deferred();
    let parameterKeys;
    if (response.type === "parameters") {
        parameterKeys = _.intersection(
            _.keys(input),
            _.keys(response.requirements.properties)
        );
        this.lastResponse = response;
        this.submit(_.pick(input, parameterKeys)).then(_.bind(function (nextResponse) {
            const remainingInput = _.omit(input, parameterKeys);
            if (_.isEmpty(remainingInput)) {
                promise.resolve(nextResponse);
            } else {
                this.submit(remainingInput).then(promise.resolve, promise.reject);
            }
        }, this),
        promise.reject);
    } else if (_.isEmpty(input)) {
        promise.resolve(response);
    } else {
        this.lastResponse = response;
        this.submit(input).then(promise.resolve, promise.reject);
    }
    return promise;
};

AnonymousProcessDelegate.prototype = Object.create(AbstractDelegate.prototype);
AnonymousProcessDelegate.prototype.constructor = AnonymousProcessDelegate;

/**
 * Initiates a new process and returns the requirements necessary for advancing to the next stage.
 * @param {Object} beginningInput - optional input which may be supplied to automatically submit to the first stage
 * @returns {Promise.<Object>} A promise that is resolved with the first set of requirements for this process
 */
AnonymousProcessDelegate.prototype.start = function (beginningInput) {
    beginningInput = beginningInput || {};
    if (this.lastResponse) { // the presence of lastResponse means this can be treated as more of a "resume" than a start
        return $.Deferred().resolve(this.lastResponse);
    } else {
        return this.serviceCall({
            "type": "GET",
            "url" : "",
            "headers": { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then(_.bind(function (response) {
            return submitParamsIfNeeded.call(this, response, beginningInput).then(_.bind(function (nextResponse) {
                this.lastResponse = nextResponse;
                return this.lastResponse;
            }, this));
        }, this));
    }
};

/**
 * Takes a generic object as input to submit to the process, intended to fulfil the requirements
 * outlined by the previous request. If there is no previous request, one will be started with the provided input.
 * @param {Object} input Generic object
 * @returns {Promise.<Object>} A promise that is resolved when the backend responds to the provided input
 */
AnonymousProcessDelegate.prototype.submit = function (input) {
    let promise;

    if (this.token || this.lastResponse) {
        promise = this.serviceCall({
            "type": "POST",
            "url": `?_action=submitRequirements${this.additional}`,
            "data": JSON.stringify({
                "token" : this.token,
                input
            }),
            "headers": { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            "errorsHandlers": {
                "failed" : {
                    status: "400"
                }
            }
        });
    } else {
        promise = this.start(input);
    }

    return promise.then(_.bind(function (response) {
        if (_.has(response, "token")) {
            this.token = response.token;
        }
        this.lastResponse = response;
        return response;
    }, this),
    _.bind(function (errorResponse) {
        delete this.token;
        delete this.lastResponse;
        if (_.has(errorResponse, "responseJSON.message")) {
            return {
                "status": {
                    "success": false,
                    "reason": errorResponse.responseJSON.message,
                    "code": errorResponse.responseJSON.code
                }
            };
        } else {
            return errorResponse;
        }
    }, this));
};

export default AnonymousProcessDelegate;
