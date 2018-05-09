/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "support/propTypesChecked",
    "store/modules/local/config/realm/authentication/trees/current/nodes/measurements"
], (propTypesChecked, subject) => {
    const dimensions = { id: "dimensionsId", height: 100, width: 100 };
    const position = { id: "positionId", x: 100, y: 100 };
    let module;

    describe("store/modules/local/config/realm/authentication/trees/current/nodes/measurements", () => {
        beforeEach(() => {
            module = propTypesChecked.default(subject);
        });
        describe("actions", () => {
            describe("#updateDimensions", () => {
                it("creates an action", () => {
                    expect(module.updateDimensions(dimensions)).eql({
                        type: "local/config/realm/authentication/trees/current/nodes/measurements/UPDATE_DIMENSIONS",
                        payload: dimensions
                    });
                });
            });
            describe("#updatePosition", () => {
                it("creates an action", () => {
                    expect(module.updatePosition(position)).eql({
                        type: "local/config/realm/authentication/trees/current/nodes/measurements/UPDATE_POSITION",
                        payload: position
                    });
                });
            });
        });
        describe("reducer", () => {
            it("returns the initial state", () => {
                expect(module.default(undefined, {})).eql({});
            });

            it("handles #updateDimensions action", () => {
                expect(module.default({}, module.updateDimensions(dimensions))).eql({
                    dimensionsId: {
                        id: dimensions.id,
                        height: dimensions.height,
                        width: dimensions.width,
                        x: 0,
                        y: 0
                    }
                });
            });

            it("handles #updatePosition action", () => {
                expect(module.default({}, module.updatePosition(position))).eql({
                    positionId: {
                        id: position.id,
                        height: 0,
                        width: 0,
                        x: position.x,
                        y: position.y
                    }
                });
            });
        });
    });
});
