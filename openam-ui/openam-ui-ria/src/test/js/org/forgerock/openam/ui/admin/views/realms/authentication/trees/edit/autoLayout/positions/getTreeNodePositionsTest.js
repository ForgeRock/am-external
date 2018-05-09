/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getColumns",
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getAllPaths",
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/positions/getTreeNodePositions",
    "../data/SampleData"
], (getColumns, getAllPaths, getTreeNodePositions, data) => {
    describe(
        `org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/positions/
            getTreeNodePositions`,
        () => {
            const paths = getAllPaths.default(data.tree.entryNodeId, data.tree.nodes);
            const columns = getColumns.default(paths);
            const positions = [
                { "id": "one", "x": 50, "y": 112.5 },
                { "id": "two", "x": 165, "y": 25 },
                { "id": "three", "x": 165, "y": 225 },
                { "id": "five", "x": 290, "y": 37 },
                { "id": "seven", "x": 290, "y": 87 },
                { "id": "eight", "x": 290, "y": 137 },
                { "id": "nine", "x": 290, "y": 187 },
                { "id": "success", "x": 290, "y": 237 },
                { "id": "four", "x": 425, "y": 112.5 },
                { "id": "six", "x": 545, "y": 125 }
            ];

            it("returns positions (id, x, y) for all connected tree nodes", () => {
                expect(getTreeNodePositions.default(columns, data.dimensions)).to.deep.equal(positions);
            });
        });
});
