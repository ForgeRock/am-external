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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { expect } from "chai";

import data from "../__test__/SampleData";
import getAllPaths from "../layout/getAllPaths";
import getColumns from "../layout/getColumns";
import getOrphanPositions from "./getOrphanPositions";

describe("org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/positions/getOrphanPositions",
    () => {
        const { orphan1, orphan2 } = data.tree.nodes;
        const paths = getAllPaths(data.tree.entryNodeId, data.tree.nodes);
        const columns = getColumns(paths);
        const positions = [
            { "id": "orphan1", "x": 50, "y": 300 },
            { "id": "orphan2", "x": 185, "y": 300 }
        ];

        it("returns positions (id, x, y) for all orphans", () => {
            expect(getOrphanPositions({ orphan1, orphan2 }, columns, data.dimensions))
                .to.deep.equal(positions);
        });
    });
