/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
