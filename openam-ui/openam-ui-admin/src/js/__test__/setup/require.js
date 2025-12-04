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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

/**
 * Setup file that gets executed before tests are required (directly upon Node.js).
 *
 * Executed directly on Node.js (not part of the Webpack bundle).
 * @see https://github.com/zinserjan/mocha-webpack/blob/e2aeeb0dc460f09b77808dcc45b595aa54a3fdcd/docs/installation/cli-usage.md#--require---include
 */

const { JSDOM } = require("jsdom");

/**
 * Setup JSDOM.
 * @see http://airbnb.io/enzyme/docs/guides/jsdom.html
 */

const copyProps = (src, target) => {
    const props = Object.getOwnPropertyNames(src)
        .filter((prop) => typeof target[prop] === "undefined")
        .reduce((result, prop) => ({
            ...result,
            [prop]: Object.getOwnPropertyDescriptor(src, prop)
        }), {});
    Object.defineProperties(target, props);
};

const jsdom = new JSDOM("<!doctype html><html><body></body></html>");
const { window } = jsdom;

global.window = window;
global.document = window.document;
global.navigator = { userAgent: "Node.js" };

copyProps(window, global);
