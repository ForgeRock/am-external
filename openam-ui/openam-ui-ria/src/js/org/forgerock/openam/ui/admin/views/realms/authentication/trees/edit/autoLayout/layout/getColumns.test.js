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

import data from "../__test__/SampleData";
import getAllPaths from "./getAllPaths";
import getColumns from "./getColumns";

describe("org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getColumns",
    () => {
        const { one, two, three, four, five, six, seven, eight, nine, success } = data.tree.nodes;
        const paths = getAllPaths(data.tree.entryNodeId, data.tree.nodes);
        const columns = [
            [{ one }],
            [{ two }, { three }],
            [{ five }, { seven }, { eight }, { nine }, { success }],
            [{ four }],
            [{ six }]
        ];

        it("removes all repeating nodes in all paths and organizes them in columns", () => {
            expect(getColumns(paths)).to.deep.equal(columns);
        });
    });
