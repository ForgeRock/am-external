/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
