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
 * Copyright 2011-2022 ForgeRock AS.
 */

import _ from "lodash";

/**
 * @exports org/forgerock/commons/ui/common/util/ObjectUtil
 */

const ObjectUtil = {};

/**
 * Translates an arbitrarily-complex object into a flat one composed of JSONPointer key-value pairs.
 * Example:
 *   toJSONPointerMap({"c": 2, "a": {"b": ['x','y','z',true]}}) returns:
 *   {/c: 2, /a/b/0: "x", /a/b/1: "y", /a/b/2: "z", /a/b/3: true}
 * @param {Object} originalObject - the object to convert to a flat map of JSONPointer values
 */
ObjectUtil.toJSONPointerMap = function (originalObject) {
    const pointerList = function (obj) {
        return _.chain(obj)
            .toPairs()
            .filter((p) => {
                return p[1] !== undefined;
            })
            .map((p) => {
                if (_.indexOf(["string", "boolean", "number"], (typeof p[1])) !== -1 ||
                    _.isEmpty(p[1]) ||
                    _.isArray(p[1])) {
                    return { "pointer": `/${p[0]}`, "value": p[1] };
                } else {
                    return _.map(pointerList(p[1]), (child) => {
                        return { "pointer": `/${p[0]}${child.pointer}`, "value": child.value };
                    });
                }
            })
            .flattenDeep()
            .value();
    };

    return _.reduce(pointerList(originalObject), (map, entry) => {
        map[entry.pointer] = entry.value;
        return map;
    }, {});
};

/**
 * Uses a JSONPointer string to find a value within a provided object
 * Examples:
 *   getValueFromPointer({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test/0") returns: "apple"
 *   getValueFromPointer({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test/1/foo") returns: "bar"
 *   getValueFromPointer({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test/1") returns:
 *      {"foo":"bar", "hello": "world"}
 *   getValueFromPointer({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test2") returns: undefined
 *   getValueFromPointer({test:["apple", {"foo":"bar", "hello": "world"}]}, "/") returns:
        {test:["apple", {"foo":"bar", "hello": "world"}]}
 * @param {Object} object - the object to search within
 * @param {string} pointer - the JSONPointer to use to find the value within the object
 */
ObjectUtil.getValueFromPointer = function (object, pointer) {
    const pathParts = pointer.split("/");
    // remove first item which came from the leading slash
    pathParts.shift(1);
    if (pathParts[0] === "") { // the case when pointer is just "/"
        return object;
    }

    return _.reduce(pathParts, (result, path) => {
        if (_.isObject(result)) {
            return result[path];
        } else {
            return result;
        }
    }, object);
};

/**
 * Look through the provided object to see how far it can be traversed using a given JSONPointer string
 * Halts at the first undefined entry, or when it has reached the end of the pointer path.
 * Returns a JSONPointer that represents the point at which it was unable to go further
 * Examples:
 *   walkDefinedPath({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test/0") returns: "/test/0"
 *   walkDefinedPath({test:["apple", {"foo":"bar", "hello": "world"}]}, "/test/3/foo") returns: "/test/3"
 *   walkDefinedPath({test:["apple", {"foo":"bar", "hello": "world"}]}, "/missing") returns: "/missing"
 *   walkDefinedPath({test:["apple", {"foo":"bar", "hello": "world"}]}, "/missing/bar") returns: "/missing"
 * @param {Object} object - the object to walk through
 * @param {string} pointer - the JSONPointer to use to walk through the object
 */
ObjectUtil.walkDefinedPath = function (object, pointer) {
    let finalPath = "";
    let node = object;
    let currentPathPart;
    const pathParts = pointer.split("/");

    // remove first item which came from the leading slash
    pathParts.shift(1);

    // walk through the path, stopping when hitting undefined
    while (node !== undefined && node !== null && pathParts.length) {
        currentPathPart = pathParts.shift(1);
        finalPath += (`/${currentPathPart}`);
        node = node[currentPathPart];
    }

    // if the whole object needs to be added....
    if (finalPath === "") {
        finalPath = "/";
    }
    return finalPath;
};

/**
 * Compare to Array values, interpeted as sets, to see if they contain the same values.
 * Important distinctive behavior of sets - order doesn't matter for equality.
 * @param {Array} set1 - an set of any values. Nested Arrays will also be interpreted as sets.
 * @param {Array} set2 - an set of any values. Nested Arrays will also be interpreted as sets.
 * Examples:
 *  isEqualSet([1], [1]) -> true
 *  isEqualSet([1], [1,3]) -> false
 *  isEqualSet([3,1], [1,3]) -> true
 *  isEqualSet([3,{a:1},1], [1,3,{a:1}]) -> true
 *  isEqualSet([3,{a:1},1], [1,3,{a:2}]) -> false
 *  isEqualSet([3,{a:1},['b','a'],1], [1,3,{a:1},['a','b']]) -> true
 */
ObjectUtil.isEqualSet = function (set1, set2) {
    const traverseSet = function (targetSet, result, sourceItem) {
        if (_.isArray(sourceItem)) {
            return result && _.find(targetSet, (targetItem) => {
                return ObjectUtil.isEqualSet(sourceItem, targetItem);
            }) !== undefined;
        } else if (_.isObject(sourceItem)) {
            return result && _.find(targetSet, sourceItem) !== undefined;
        } else {
            return result && _.indexOf(targetSet, sourceItem) !== -1;
        }
    };

    return _.reduce(set1, _.curry(traverseSet)(set2), true) &&
        _.reduce(set2, _.curry(traverseSet)(set1), true);
};

/**
 * Given a first set, return a subset containing all items which are not
 * present in the second set.
 * @param {Array} set1 set
 * @param {Array} set2 to intersect with
 * Examples:
 *  findItemsNotInSet([1,2,3],[2,3]) -> [1]
 *  findItemsNotInSet([1,2,3],[2,3,1]) -> []
 *  findItemsNotInSet([1,{a:1},3],[3,1,{a:2}]) -> [{a:1}]
 */
ObjectUtil.findItemsNotInSet = function (set1, set2) {
    return _.filter(set1, (item1) => {
        return !_.find(set2, (item2) => {
            return _.isEqual(item1, item2);
        });
    });
};

/**
 * Compares two objects and generates a patchset necessary to convert the second object to match the first
 * Examples:
 *   generatePatchSet({"a": 1, "b": 2}, {"a": 1}) returns:
 *   [{"operation":"add","field":"/b","value":2}]
 *
 *   generatePatchSet({"a": 1, "b": 2}, {"c": 1}) returns:
 *   [
 *     {"operation":"add","field":"/a","value":1},
 *     {"operation":"add","field":"/b","value":2},
 *     {"operation":"remove","field":"/c"}
 *   ]
 *
 *   generatePatchSet({"a": [1,2]}, {"a": [1,3]}) returns:
 *   [
 *     {"operation":"add","field":"/a/-","value":2},
 *     {"operation":"remove","field":"/a","value":3}
 *   ]
 * @param {Object} newObject - the object to build up to
 * @param {Object} oldObject - the object to start from
 */
ObjectUtil.generatePatchSet = function (newObject, oldObject) {
    const newObjectClosure = newObject; // needed to have access to newObject within _ functions
    const oldObjectClosure = oldObject; // needed to have access to oldObject within _ functions
    const newPointerMap = ObjectUtil.toJSONPointerMap(newObject);
    const previousPointerMap = ObjectUtil.toJSONPointerMap(oldObject);
    const newValues = _.chain(newPointerMap)
        .toPairs()
        .filter((p) => {
            if (_.isArray(previousPointerMap[p[0]]) && _.isArray(p[1])) {
                return !ObjectUtil.isEqualSet(previousPointerMap[p[0]], p[1]);
            } else {
                return !_.isEqual(previousPointerMap[p[0]], p[1]);
            }
        })
        .map((p) => {
            const finalPathToAdd = ObjectUtil.walkDefinedPath(oldObjectClosure, p[0]);
            const newValueAtFinalPath = ObjectUtil.getValueFromPointer(newObjectClosure, finalPathToAdd);
            const oldValueAtFinalPath = ObjectUtil.getValueFromPointer(oldObjectClosure, finalPathToAdd);
            const setToPatchOperation = function (set, operation, path) {
                return _.map(set, (item) => {
                    return {
                        operation,
                        "field": path,
                        "value": item
                    };
                });
            };
            if (_.isArray(newValueAtFinalPath) && _.isArray(oldValueAtFinalPath)) {
                return setToPatchOperation(
                    ObjectUtil.findItemsNotInSet(newValueAtFinalPath, oldValueAtFinalPath),
                    "add",
                    `${finalPathToAdd}/-` // add to set syntax
                ).concat(setToPatchOperation(
                    ObjectUtil.findItemsNotInSet(oldValueAtFinalPath, newValueAtFinalPath),
                    "remove",
                    finalPathToAdd
                ));
            } else if (newValueAtFinalPath === null) {
                return {
                    "operation": "remove",
                    "field": finalPathToAdd
                };
            } else {
                return {
                    "operation": (oldValueAtFinalPath === undefined) ? "add" : "replace",
                    "field": finalPathToAdd,
                    "value": newValueAtFinalPath
                };
            }
        })
        .flatten()
        // Filter out duplicates which might result from adding whole containers
        // Have to stringify the patch operations to do object comparisons with uniq
        .uniqBy(JSON.stringify)
        .value();
    const removedValues = _.chain(previousPointerMap)
        .toPairs()
        .filter((p) => {
            return ObjectUtil.getValueFromPointer(newObjectClosure, p[0]) === undefined;
        })
        .map((p) => {
            const finalPathToRemove = ObjectUtil.walkDefinedPath(newObjectClosure, p[0]);
            return { "operation": "remove", "field": finalPathToRemove };
        })
        // Filter out duplicates which might result from deleting whole containers
        // Have to stringify the patch operations to do object comparisons with uniq
        .uniqBy(JSON.stringify)
        .value();

    return newValues.concat(removedValues);
};

const COMPLEX_OBJECT_LIST = [Boolean, Number, String, RegExp, Date];

/**
 * Determines whether given param is a pure object
 * will return false for RegExp, Number, Function, Number objects and Arrays etc.
 * @param {*} value object to determine
 * @returns boolean true if the type is an object
 */
ObjectUtil.isPureObject = (value) => {
    if (!value) return false;

    const isComplexObject = (val) => COMPLEX_OBJECT_LIST.some((type) => val instanceof type);

    if (typeof value === "function"
        || isComplexObject(value)
        || Array.isArray(value)) {
        return false;
    }

    return _.isObject(value);
};


export default ObjectUtil;
