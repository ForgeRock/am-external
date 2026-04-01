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
 * Copyright 2026 Ping Identity Corporation.
 */

import { expect } from "chai";
import { 
    isHigherVersion,
    getVersionDataForNodeType,
    isNodeUpgradable,
    sortVersions
} from "./nodeVersioningUtils";

describe("nodeVersioningUtils", () => {
    describe('isHigherVersion', () => {
      const testCases = [
        {
          a: "1.0",
          b: "2.0",
          expected: false,
        },
        {
          a: "11.0",
          b: "12.0",
          expected: false,
        },
        {
          a: "11.0",
          b: "20.0",
          expected: false,
        },
        {
          a: "10.0",
          b: "10.1",
          expected: false,
        },
        {
          a: "10",
          b: "11",
          expected: false,
        },
        {
          a: "1.0",
          b: "1.0",
          expected: false,
        },
        {
          a: "2.0",
          b: "1.0",
          expected: true,
        },
        {
          a: "12.0",
          b: "11.0",
          expected: true,
        },
        {
          a: "20.0",
          b: "11.0",
          expected: true,
        },
        {
          a: "20.1",
          b: "20.0",
          expected: true,
        },
        {
          a: "20",
          b: "10",
          expected: true,
        }
      ];
      
      testCases.forEach(({ a, b, expected }) => {
        const result = isHigherVersion(a, b);;
        expect(result).eql(expected);
      });
    });

    describe('getVersionDataForNodeType', () => {
        const nodeTypes = [
          { 
            _id: "ValidatedUsernameNode",
            versions: ["1.0"]
          }, 
          { 
            _id: "DeviceSaveNode",
            versions: ["1.0", "2.0"]
          },
          { 
            _id: "AcceptTermsAndConditionsNode",
            versions: ["1.0", "1.1"]
          },
          {
            _id: "PageNode",
            versions: ["1.0", "2.0"]
          },
          {
            _id: "PollingWaitNode",
          }
        ];

        // note: we don't want to test whether isUpgradable is correct here, we
        // test that separately below. We only want to test that it returns the 
        // correct data - versions array with correct values and isUpgradable property
        const testCases = [
          { 
            node: { nodeType: "ValidatedUsernameNode", version: "1.0" },
            expected: { versions: ["1.0"], isUpgradable: false }
          },
          { 
            node: { nodeType: "DeviceSaveNode", version: "1.0" },
            expected: { versions: ["1.0", "2.0",], isUpgradable: true }
          },
          { 
            node: { nodeType: "AcceptTermsAndConditionsNode", version: "1.1" },
            expected: { versions: ["1.0", "1.1"], isUpgradable: false }
          },
          { 
            node: { nodeType: "PageNode", nodeVersion: "1.0" },
            expected: { versions: ["1.0", "2.0"], isUpgradable: true }
          },
          { 
            node: { nodeType: "PollingWaitNode", version: "1.0" },
            expected: { versions: [], isUpgradable: false }
          },
        ];

        testCases.forEach(({ node, expected }) => {
          const result = getVersionDataForNodeType(nodeTypes, node);

          expect(result).to.deep.eql(expected);
        });
    });

    describe("isNodeUpgradable", () => {
        const testCases = [
          { 
            currentVersion: "1.0",
            availableVersions: ["1.0"],
            expected: false
          },
          { 
            currentVersion: "1.0",
            availableVersions: ["1.0", "2.0", "3.0"],
            expected: true
          },
          { 
            currentVersion: "2.0",
            availableVersions: ["1.0", "2.0"],
            expected: false
          },
          { 
            currentVersion: "1.10",
            availableVersions: ["1.0", "1.9"],
            expected: false
          },
          { 
            currentVersion: "1.10",
            availableVersions: [],
            expected: false
          },
          { 
            currentVersion: "1.10",
            availableVersions: ["1.11", "2.0"],
            expected: true
          },
          { 
            currentVersion: "1.10",
            availableVersions: ["1.10", "1.90"],
            expected: true
          },
          { 
            currentVersion: "5",
            availableVersions: ["1.0", "2.0", "3.0", "4.0", "5.0", "6.0", "7.0", "8.0", "9.0"],
            expected: true
          }
        ];

        testCases.forEach(({ currentVersion, availableVersions, expected }) => {
          const result = isNodeUpgradable(currentVersion, availableVersions);

          expect(result).to.eql(expected);
        });
    });

    describe('sortVersions', () => {
        const testCases = [
          {
            availableVersions: ["1.0", "2.0"],
            expected: ["1.0", "2.0"]
          },
          {
            availableVersions: ["2.0", "1.0"],
            expected: ["1.0", "2.0"]
          },
          {
            availableVersions: ["10.0", "20.0"],
            expected: ["10.0", "20.0"]
          },
          {
            availableVersions: ["10.1", "10.2"],
            expected: ["10.1", "10.2"]
          },
          {
            availableVersions: ["10.1", "10"],
            expected: ["10", "10.1"]
          },
          {
            availableVersions: ["10", "11.1", "86.3", "999.1", "0.1"],
            expected: ["0.1", "10", "11.1", "86.3", "999.1"]
          }
        ];

        testCases.forEach(({ availableVersions, expected }) => {
          const result = sortVersions(availableVersions);

          expect(result).to.eql(expected);
        });
    });
});