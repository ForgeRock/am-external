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
    "../data/SampleData"
], (getColumns, getAllPaths, data) => {
    describe("org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getColumns",
        () => {
            const { one, two, three, four, five, six, seven, eight, nine, success } = data.tree.nodes;
            const paths = getAllPaths.default(data.tree.entryNodeId, data.tree.nodes);
            const columns = [
                [{ one }],
                [{ two }, { three }],
                [{ five }, { seven }, { eight }, { nine }, { success }],
                [{ four }],
                [{ six }]
            ];

            it("removes all repeating nodes in all paths and organizes them in columns", () => {
                expect(getColumns.default(paths)).to.deep.equal(columns);
            });
        });
});
