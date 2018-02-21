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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "store/modules/local/authentication/trees/current/nodes/measurements"
], (module) => {
    const dimensions = { id: "dimensions", height: 100, width: 100 };
    const position = { id: "position", x: 100, y: 100 };

    describe("store/modules/local/authentication/trees/current/nodes/measurements", () => {
        describe("actions", () => {
            describe("#updateDimensions", () => {
                it("creates an action", () => {
                    expect(module.updateDimensions(dimensions)).eql({
                        type: "local/authentication/trees/current/nodes/measurements/UPDATE_DIMENSIONS",
                        payload: dimensions
                    });
                });
            });
            describe("#updatePosition", () => {
                it("creates an action", () => {
                    expect(module.updatePosition(position)).eql({
                        type: "local/authentication/trees/current/nodes/measurements/UPDATE_POSITION",
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
                    dimensions: {
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
                    position: {
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
