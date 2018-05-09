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
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/positions/getOrphanPositions",
    "../data/SampleData"
], (getColumns, getAllPaths, getOrphanPositions, data) => {
    describe(`org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/positions/
        getOrphanPositions`,
        () => {
            const { orphan1, orphan2 } = data.tree.nodes;
            const paths = getAllPaths.default(data.tree.entryNodeId, data.tree.nodes);
            const columns = getColumns.default(paths);
            const positions = [
                { "id": "orphan1", "x": 50, "y": 300 },
                { "id": "orphan2", "x": 185, "y": 300 }
            ];

            it("returns positions (id, x, y) for all orphans", () => {
                expect(getOrphanPositions.default({ orphan1, orphan2 }, columns, data.dimensions))
                    .to.deep.equal(positions);
            });
        });
});
