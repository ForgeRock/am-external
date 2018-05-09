/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/*globals QUnit */

define([
    "lodash",
    "org/forgerock/commons/ui/common/util/ObjectUtil"
], function (_, ObjectUtil) {
    QUnit.module('ObjectUtil Functions');

    QUnit.test("toJSONPointerMap", function () {
        var jsonMap = ObjectUtil.toJSONPointerMap({"c": 2, "a": {"b": ['x','y','z',true], "d": undefined }});
        QUnit.equal(jsonMap["/c"], '2', "toJSONPointerMap correctly flattens complex object");
        QUnit.ok(_.isEqual(jsonMap["/a/b"], ['x','y','z',true]),
            "toJSONPointerMap correctly returns a list when it encounters an array");
        QUnit.ok(!_.has(jsonMap, '/d'), "undefined value not included in map produced by toJSONPointerMap");
    });

    QUnit.test("getValueFromPointer", function () {
        var testObject = {
            testSet: ["apple", "pear"],
            testMap: {"foo":"bar", "hello": "world"}
        };
        QUnit.equal(ObjectUtil.getValueFromPointer(testObject, "/testMap/foo"), "bar", "/testMap/foo");
        QUnit.ok(
            _.isEqual(ObjectUtil.getValueFromPointer(testObject, "/testSet"),
            ["apple", "pear"],
            "/testSet"));
        QUnit.equal(ObjectUtil.getValueFromPointer(testObject, "/test2"), undefined, "/test2");
        QUnit.equal(ObjectUtil.getValueFromPointer(testObject, "/"), testObject, "/");
    });

    QUnit.test("isEqualSet", function () {
        QUnit.ok(ObjectUtil.isEqualSet([1], [1]), "Simple set equality");
        QUnit.ok(!ObjectUtil.isEqualSet([1], [1,3]), "Simple set inequality");
        QUnit.ok(ObjectUtil.isEqualSet([3,1], [1,3]), "Set equality regardless of order");
        QUnit.ok(ObjectUtil.isEqualSet([3,{a:1},1], [1,3,{a:1}]), "Set equality with complex items");
        QUnit.ok(!ObjectUtil.isEqualSet([3,{a:1},1], [1,3,{a:2}]),
            "Set inequality with differing complex items");
        QUnit.ok(ObjectUtil.isEqualSet([3,{a:1},['b','a'],1], [1,3,{a:1},['a','b']]),
            "Set equality with complex objects, regardless of order, and with nested sets");
    });

    QUnit.test("findItemsNotInSet", function () {
        QUnit.ok(_.isEqual(ObjectUtil.findItemsNotInSet([1,2,3],[2,3]), [1]), "Simple difference found");
        QUnit.ok(_.isEqual(ObjectUtil.findItemsNotInSet([1,2,3],[2,3,1]), []),
            "No differences found despite order differences");
        QUnit.ok(_.isEqual(ObjectUtil.findItemsNotInSet([1,{a:1},3],[3,1,{a:2}]), [{a:1}]),
            "Complex item difference recognized");
        QUnit.ok(_.isEqual(ObjectUtil.findItemsNotInSet([1,{b:2,a:1},3],[3,1,{a:1,b:2}]), []),
            "Complex item equality recognized, regardless of order");
    });

    QUnit.test("walkDefinedPath", function () {
        var testObject = {test:["apple", {"foo":"bar", "hello": "world"}]};
        QUnit.equal(ObjectUtil.walkDefinedPath(testObject, "/test/0"), "/test/0", "/test/0");
        QUnit.equal(ObjectUtil.walkDefinedPath(testObject, "/test/3/foo"), "/test/3", "/test/3/foo");
        QUnit.equal(ObjectUtil.walkDefinedPath(testObject, "/missing"), "/missing", "/missing");
        QUnit.equal(ObjectUtil.walkDefinedPath(testObject, "/missing/bar"), "/missing", "/missing/bar");
        QUnit.equal(ObjectUtil.walkDefinedPath({ } , "/foo"), "/foo", "/foo with empty object");
        QUnit.equal(ObjectUtil.walkDefinedPath({ foo: undefined } , "/foo"),
            "/foo",
            "/foo as a property with undefined as the value");
        QUnit.equal(ObjectUtil.walkDefinedPath({ foo: null }, "/foo/bar"),
            "/foo",
            "/foo as a property with null as the value");
        QUnit.equal(ObjectUtil.walkDefinedPath({ foo: {bar:null} } , "/foo/bar"),
            "/foo/bar",
            "/foo/bar as a property with null as the value");
    });

    QUnit.test("generatePatchSet", function () {
        var patchDef = ObjectUtil.generatePatchSet({"a": 1, "b": 2}, {"a": 1});
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "add" &&
                patchDef[0].field === "/b" && patchDef[0].value === 2,
            "Simple field addition returned for patchDef");

        patchDef = ObjectUtil.generatePatchSet({"a": 1, "b": 2}, {"c": 1});
        QUnit.equal(patchDef.length, 3,
            "Expected operation count for removal of one attribute and addition of two others");

        patchDef = ObjectUtil.generatePatchSet({
            "setItems": [{"sub": 2}]
        }, {
            "setItems": [{"sub": 1}, {"sub": 2}]
        });
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "remove" &&
                patchDef[0].field === "/setItems" && _.isEqual(patchDef[0].value, {"sub": 1}),
            "Removal of value from set based on value of item");

        /* note that the order of the items isn't relevant; only the content matters */
        patchDef = ObjectUtil.generatePatchSet({
            "setItems": [{"sub": 4}, {"sub": 2}, {"sub": 3}]
        }, {
            "setItems": [{"sub": 3}, {"sub": 2}]
        });

        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "add" &&
                patchDef[0].field === "/setItems/-" && _.isEqual(patchDef[0].value, {"sub": 4}),
            "Addition of value to set");

        patchDef = ObjectUtil.generatePatchSet({manager:{_ref: "a/b/c"}},{});
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "add" &&
                patchDef[0].field === "/manager" && _.isEqual(patchDef[0].value, {_ref: "a/b/c"}),
            "Addition of whole new complex property results in full map added");

        patchDef = ObjectUtil.generatePatchSet({manager:null},{manager:{_ref: "a/b/c"}});
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "remove" &&
                patchDef[0].field === "/manager" && !patchDef[0].value,
            "Setting a complex property to null results in a remove operation on the whole object");

        patchDef = ObjectUtil.generatePatchSet({manager:{_ref: "a/b/c"}},{manager:null});
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "replace" &&
                patchDef[0].field === "/manager" && _.isEqual(patchDef[0].value, {_ref: "a/b/c"}),
            "Replacement of null value with whole new complex property results in full map added");

        patchDef = ObjectUtil.generatePatchSet({manager:{_ref: "a/b/d"}},{manager:{_ref: "a/b/c"}});
        QUnit.ok(patchDef.length === 1 && patchDef[0].operation === "replace" &&
                patchDef[0].field === "/manager/_ref" && patchDef[0].value === "a/b/d",
            "Replacement of simple value in nested map");
    });

});
