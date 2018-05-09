/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getAllPaths",
    "../data/SampleData"
], (getAllPaths, data) => {
    describe("org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getAllPaths",
        () => {
            const { one, two, three, four, five, six, seven, eight, nine, success } = data.tree.nodes;
            const paths = [
                [{ one }, { two }, { five }, { four }, { six }],
                [{ one }, { two }, { four }, { five }],
                [{ one }, { two }, { four }, { six }],
                [{ one }, { two }, { five }, { four }],
                [{ one }, { two }, { six }],
                [{ one }, { two }, { seven }],
                [{ one }, { two }, { eight }],
                [{ one }, { two }, { nine }],
                [{ one }, { three }, { success }]
            ];

            it("returns all paths for the tree", () => {
                expect(
                    getAllPaths.default(data.tree.entryNodeId, data.tree.nodes)
                ).to.deep.equal(paths);
            });
        });
});
