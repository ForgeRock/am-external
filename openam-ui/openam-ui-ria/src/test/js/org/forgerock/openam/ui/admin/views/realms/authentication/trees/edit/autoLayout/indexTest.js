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
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/index",
    "./data/SampleData"
], (autoLayout, data) => {
    describe("org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/index",
        () => {
            const positions = {
                "one": { "id": "one", "x": 50, "y": 112.5 },
                "two": { "id": "two", "x": 165, "y": 25 },
                "three": { "id": "three", "x": 165, "y": 225 },
                "five": { "id": "five", "x": 290, "y": 37 },
                "success": { "id": "success", "x": 290, "y": 237 },
                "eight": { "id": "eight", "x": 290, "y": 137 },
                "nine": { "id": "nine", "x": 290, "y": 187 },
                "seven": { "id": "seven", "x": 290, "y": 87 },
                "four": { "id": "four", "x": 425, "y": 112.5 },
                "six": { "id": "six", "x": 545, "y": 125 },
                "orphan1": { "id": "orphan1", "x": 50, "y": 300 },
                "orphan2": { "id": "orphan2", "x": 185, "y": 300 }
            };

            it("returns positions (x, y) for all its nodes both connected nodes and orphans", () => {
                expect(autoLayout.default(data.tree.entryNodeId, data.tree.nodes, data.dimensions))
                    .to.deep.equal(positions);
            });
        });
});
