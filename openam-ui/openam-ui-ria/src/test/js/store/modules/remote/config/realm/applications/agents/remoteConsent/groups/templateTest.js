/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "store/modules/remote/config/realm/applications/agents/remoteConsent/groups/template"
], (module) => {
    const template = { key: "value" };

    describe("store/modules/remote/config/realm/applications/agents/remoteConsent/groups/template", () => {
        describe("actions", () => {
            describe("#setTemplate", () => {
                it("creates an action", () => {
                    expect(module.setTemplate(template)).eql({
                        type: "remote/config/realm/applications/agents/remoteConsent/groups/template/SET_TEMPLATE",
                        payload: template
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).to.be.null;
            });

            it("handles #setTemplate action", () => {
                expect(module.default({}, module.setTemplate(template))).eql(template);
            });
        });
    });
});
