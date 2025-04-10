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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { expect } from "chai";

import data from "../__test__/SampleData";
import getAllPaths from "../layout/getAllPaths";
import getColumns from "../layout/getColumns";
import getTreeNodePositions from "./getTreeNodePositions";

describe(
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/positions/getTreeNodePositions",
    () => {
        const paths = getAllPaths(data.tree.entryNodeId, data.tree.nodes);
        const columns = getColumns(paths);
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
            expect(getTreeNodePositions(columns, data.dimensions)).to.deep.equal(positions);
        });
    });
