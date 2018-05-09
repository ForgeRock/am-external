/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define(["lodash"], ({ mapValues, size }) => {
    const tree = {
        _id: "treeOne",
        entryNodeId: "one"
    };

    const createNode = (id, connections, outcomes) => ({
        displayName: id,
        connections: connections || {},
        _outcomes: outcomes || []
    });

    const one = createNode("one", {
        "outcome1": "two",
        "outcome2": "three"
    }, [{
        id: "outcome1",
        displayName: "Outcome 1"
    }, {
        id: "outcome2",
        displayName: "Outcome 2"
    }]);

    const two = createNode("two", {
        "outcome1": "four",
        "outcome2": "five",
        "outcome3": "six",
        "outcome4": "seven",
        "outcome5": "eight",
        "outcome6": "nine"
    }, [{
        id: "outcome1",
        displayName: "Outcome 1"
    }, {
        id: "outcome2",
        displayName: "Outcome 2"
    }, {
        id: "outcome3",
        displayName: "Outcome 3"
    }, {
        id: "outcome4",
        displayName: "Outcome 4"
    }, {
        id: "outcome5",
        displayName: "Outcome 5"
    }, {
        id: "outcome6",
        displayName: "Outcome 6"
    }, {
        id: "outcome7",
        displayName: "Outcome 7"
    }]);
    const three = createNode("three", {
        "outcome": "success"
    }, [{
        id: "outcome",
        displayName: "Outcome"
    }]);
    const four = createNode("four", {
        "outcome1": "five",
        "outcome2": "six"
    }, [{
        id: "outcome1",
        displayName: "Outcome 1"
    }, {
        id: "outcome2",
        displayName: "Outcome 2"
    }]);
    const five = createNode("five", {
        "outcome": "four"
    }, [{
        id: "outcome",
        displayName: "Outcome"
    }]);
    const six = createNode("six", {}, [{
        id: "outcome",
        displayName: "Outcome"
    }]);
    const seven = createNode("seven", {}, [{
        id: "outcome",
        displayName: "Outcome"
    }]);
    const eight = createNode("eight", {}, [{
        id: "outcome",
        displayName: "Outcome"
    }]);
    const nine = createNode("nine", {}, [{
        id: "outcome",
        displayName: "Outcome"
    }]);
    const success = createNode("success");
    const orphan1 = createNode("orphan1", {
        "outcome": "orphan2"
    }, [{
        id: "outcome",
        displayName: "Outcome"
    }]);
    const orphan2 = createNode("orphan2", {}, [{
        id: "outcome",
        displayName: "Outcome"
    }]);

    tree.nodes = { one, two, three, four, five, six, seven, eight, nine, success, orphan1, orphan2 };

    const DEFAULT_MIN_HEIGHT = 25;
    const DEFAULT_MIN_WIDTH = 50;
    const DEFAULT_CHARACTER_WIDTH = 5;

    const dimensions = mapValues(tree.nodes, (node, nodeId) => {
        return {
            height: DEFAULT_MIN_HEIGHT * size(node._outcomes) || 1,
            width:  DEFAULT_MIN_WIDTH + (nodeId.length * DEFAULT_CHARACTER_WIDTH)
        };
    });

    return {
        dimensions,
        tree
    };
});
