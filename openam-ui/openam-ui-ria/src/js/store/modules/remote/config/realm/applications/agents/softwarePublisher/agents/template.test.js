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
 * Copyright 2017-2018 ForgeRock AS.
 */

import { expect } from "chai";

import reducer, { setTemplate } from "./template";

const template = { key: "value" };

describe("store/modules/remote/config/realm/applications/agents/softwarePublisher/agents/template", () => {
    describe("actions", () => {
        describe("#setTemplate", () => {
            it("creates an action", () => {
                expect(setTemplate(template)).eql({
                    type: "remote/config/realm/applications/agents/softwarePublisher/agents/template/SET_TEMPLATE",
                    payload: template
                });
            });
        });
    });
    describe("reducer", () => {
        it("returns the initial state", () => {
            expect(reducer(undefined, {})).to.be.null;
        });

        it("handles #setTemplate action", () => {
            expect(reducer({}, setTemplate(template))).eql(template);
        });
    });
});
