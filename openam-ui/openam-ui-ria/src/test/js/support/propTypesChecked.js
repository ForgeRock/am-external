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
 * Copyright 2018 ForgeRock AS.
 */

import sinon from "sinon";

const propTypesChecked = (subject) => {
    let spy;
    if (console.error.reset) {
        spy = console.error;
        spy.resetHistory();
    } else {
        spy = sinon.spy(console, "error");
    }
    const stubbed = {};
    Object.getOwnPropertyNames(subject).filter((prop) => prop.indexOf("_") === -1).forEach((prop) => {
        stubbed[prop] = subject[prop];
    });
    stubbed.default = function () {
        const result = subject["default"](...arguments);
        expect(spy).not.to.have.been.called;
        return result;
    };
    return stubbed;
};

export default propTypesChecked;
