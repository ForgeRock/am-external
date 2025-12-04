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
 * Copyright 2015-2025 Ping Identity Corporation.
 */

/**
 * Setup file that gets executed before tests are required (within the Webpack bundle).
 *
 * Compiled with Webpack and executed as part of the resulting bundle.
 * @see https://github.com/zinserjan/mocha-webpack/blob/e2aeeb0dc460f09b77808dcc45b595aa54a3fdcd/docs/installation/cli-usage.md#--require---include
 */

import chai from "chai";
import sinonChai from "sinon-chai";

chai.use(sinonChai);
