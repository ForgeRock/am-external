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
 * Copyright 2016-2017 ForgeRock AS.
 */

define([
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues"
], (JSONSchema, JSONValues) => {
    describe("org/forgerock/openam/ui/common/models/JSONValues", () => {
        describe("#constructor", () => {
            let globalValues;
            let defaultsValues;
            let defaultsCollectionValues;

            beforeEach(() => {
                globalValues = new JSONValues({
                    "_id": "",
                    "_type": {},
                    "globalSimpleKey": "globalSimpleValue",
                    "globalCollectionKey": {
                        "collectionItem_1": "collectionItemValue_1",
                        "collectionItem_2": "collectionItemValue_12"
                    },
                    "dynamic": {}
                });

                defaultsValues = new JSONValues({
                    "_id": "",
                    "_type": {},
                    "defaults": {
                        "defaultsCollection": {
                            "collectionItem1": "value1",
                            "collectionItem2": "value2"
                        },
                        "defaultsSimple": "simpleValue"
                    }
                });

                defaultsCollectionValues = new JSONValues({
                    "_id": "",
                    "_type": {},
                    "defaults": {
                        "defaultsCollection": {
                            "collectionItem1": "value1",
                            "collectionItem2": "value2"
                        }
                    }
                });
            });

            //Global values
            it("groups the top-level simple values under a \"global\" value", () => {
                expect(globalValues.raw).to.contain.keys("global");
                expect(globalValues.raw.global).to.contain.keys("globalSimpleKey");
            });

            it("does not group the top-level collection values under a \"global\" value", () => {
                expect(globalValues.raw).to.contain.keys("global");
                expect(globalValues.raw.global).to.not.contain.keys("globalCollectionKey");
            });

            //Defaults values
            it("does not ungroup \"defaults\" simple values", () => {
                expect(defaultsValues.raw).to.contain.keys("defaults");
                expect(defaultsValues.raw.defaults).to.contain.keys("defaultsSimple");
            });

            it("ungroups \"defaults\" collection values, moving them one level up", () => {
                expect(defaultsValues.raw).to.contain.keys("defaultsCollection");
            });

            it("contains \"_defaultsCollectionProperties\"", () => {
                expect(defaultsValues.raw).to.contain.keys("_defaultsCollectionProperties");
            });

            it("keeps \"defaults\" property in place as there are simple props present", () => {
                expect(defaultsValues.raw).to.contain.keys("defaults");
            });

            it("ungroups \"defaults\" collection values, moving them one level up (collection props only)", () => {
                expect(defaultsCollectionValues.raw).to.contain.keys("defaultsCollection");
            });

            it("removes \"defaults\" property when there are no simple props", () => {
                expect(defaultsCollectionValues.raw).to.not.have.keys("defaults");
            });
        });
        describe("#addInheritance", () => {
            const jsonValues = new JSONValues({
                propertyKey: "value"
            });

            let values;

            beforeEach(() => {
                values = jsonValues.addInheritance({
                    propertyKey: {
                        inherited: true
                    }
                });
            });

            it("creates an object for each property key", () => {
                expect(values.raw.propertyKey).to.be.an("object");
            });

            it("creates a \"value\" attribute on the property object", () => {
                expect(values.raw.propertyKey.value).to.exist;
                expect(values.raw.propertyKey.value).eq("value");
            });

            it("creates a \"inherited\" attribute on the property object", () => {
                expect(values.raw.propertyKey.inherited).to.exist;
                expect(values.raw.propertyKey.inherited).to.be.true;
            });
        });
        describe("#removeInheritance", () => {
            it("flattens each inherited property into a single value", () => {
                const jsonValues = new JSONValues({
                    propertyKey: {
                        value: "value",
                        inherited: true
                    }
                });

                const values = jsonValues.removeInheritance();

                expect(values.raw.propertyKey).eq("value");
            });
        });
        describe("#revertFalseCollections", () => {
            it("reverts false collection properties back into field properties (OPENAM-10769)", () => {
                const jsonValues = new JSONValues({
                    _id: "",
                    _type: {},
                    defaults: {
                        "defaults.collection": {
                            "collection.item.1": "value1"
                        },
                        "defaults.object": {
                            "object.prop.1": "value1",
                            "object.prop.2": "value2"
                        }
                    }
                });

                const values = jsonValues.revertFalseCollections(new JSONSchema({
                    type: "object",
                    properties: {
                        defaults: {
                            type: "object",
                            properties: {
                                "defaults.object": {
                                    type: "object"
                                },
                                "defaults.collection": {
                                    properties: {},
                                    type: "object"
                                }
                            }
                        }
                    }
                }));

                expect(values.raw).to.contain.keys("defaults");
                expect(values.raw.defaults).to.contain.keys("defaults.object");
                expect(values.raw).to.not.have.keys("defaults.object");
                expect(values.raw._defaultsCollectionProperties).to.eql(["defaults.collection"]);
                expect(values.raw).to.contain.keys("defaults.collection");
            });
        });
        describe("#toJSON", () => {
            let values;
            let valueWithDefaultsCollectionProperties;
            let valuesWithDefaultsMixedProperties;

            beforeEach(() => {
                values = new JSONValues({
                    "_id": {},
                    "_type": {},
                    "globalValue": {},
                    "defaults": {
                        "defaultsSimple": "simpleValue"
                    },
                    "dynamic": {
                        "dynamicSimple": "simpleValue"
                    }
                }).toJSON();

                valueWithDefaultsCollectionProperties = new JSONValues({
                    "_id": {},
                    "_type": {},
                    "_defaultsCollectionProperties": ["defaultsCollection", "defaultsCollection2"],
                    "defaultsCollection": {
                        "collectionItem1": "value1",
                        "collectionItem2": "value2"
                    },
                    "defaultsCollection2": {
                        "collectionItem1": "value1",
                        "collectionItem2": "value2"
                    }
                }).toJSON();

                valuesWithDefaultsMixedProperties = new JSONValues({
                    "_id": "",
                    "_type": {},
                    "defaultsCollection": {
                        "collectionItem1": "value1",
                        "collectionItem2": "value2"
                    },
                    "_defaultsCollectionProperties": ["defaultsCollection"],
                    "defaults": {
                        "defaultsSimple": "simpleValue"
                    }
                }).toJSON();
            });

            it("returns an JSON string", () => {
                expect(values).to.be.a("string");
                expect(JSON.parse(values)).to.not.throw;
            });

            it("returns global values at the top-level", () => {
                expect(JSON.parse(values)).to.contain.keys("globalValue");
            });

            it("returns defaults values under a \"defaults\" property", () => {
                expect(JSON.parse(values)).to.contain.keys("defaults");
                expect(JSON.parse(values).defaults).to.contain.keys("defaultsSimple");
            });

            it("returns \"_id\" at the top-level", () => {
                expect(JSON.parse(values)).to.contain.keys("_id");
            });

            it("returns \"_type\" at the top-level", () => {
                expect(JSON.parse(values)).to.contain.keys("_type");
            });

            // valueWithDefaultsCollectionProperties
            it("constructs \"defaults\" property from the defaults collection values", () => {
                expect(JSON.parse(valueWithDefaultsCollectionProperties))
                    .to.contain.keys("defaults");
            });

            it("returns defaults collection values under a \"defaults\" property", () => {
                expect(JSON.parse(valueWithDefaultsCollectionProperties).defaults)
                    .to.contain.keys("defaultsCollection");
                expect(JSON.parse(valueWithDefaultsCollectionProperties).defaults)
                    .to.contain.keys("defaultsCollection2");
            });

            it("deletes meta info key \"_defaultsCollectionProperties\" from the valueWithDefaultsCollectionProperties",
            () => {
                expect(JSON.parse(valueWithDefaultsCollectionProperties))
                    .to.not.contain.keys("_defaultsCollectionProperties");
            });

            it("deletes meta info key \"_defaultsCollectionProperties\" from the valuesWithDefaultsMixedProperties",
            () => {
                expect(JSON.parse(valuesWithDefaultsMixedProperties))
                    .to.not.contain.keys("_defaultsCollectionProperties");
            });
        });

        describe("#removeNullPasswords", () => {
            context("when using a schema without inheritance", () => {
                const schema = new JSONSchema({
                    type: "object",
                    properties: {
                        "non.password.property.1": { type: "string" },
                        "password.1": { format: "password" },
                        "password.2": { format: "password" },
                        "collection.prop.1": {
                            type: "object",
                            properties: {
                                "non.password.property.2": { type: "string" },
                                "password.3": { format: "password" },
                                "password.4": { format: "password" },
                                "object.property": { type: "object" },
                                "collection.prop.2": {
                                    type: "object",
                                    properties: {
                                        "non.password.property.3": { type: "string" },
                                        "password.5": { format: "password" },
                                        "password.6": { format: "password" }
                                    }
                                }
                            }
                        }
                    }
                });

                const values = new JSONValues({
                    _id: "",
                    _type: {},
                    "non.password.property.1": "test",
                    "password.1": "password",
                    "password.2": null,
                    "collection.prop.1": {
                        "non.password.property.2": "test",
                        "password.3": "password",
                        "password.4": null,
                        "object.property": {
                            objectProp: "value"
                        },
                        "collection.prop.2": {
                            "non.password.property.3": "test",
                            "password.5": "password",
                            "password.6": null
                        },
                        "not.in.schema": {
                            "property": "test",
                            "password": null
                        }
                    }
                });

                const valuesWithoutNullPasswords = values.removeNullPasswords(schema);

                it("removes non-grouped null passwords", () => {
                    expect(valuesWithoutNullPasswords.raw).to.not.have.keys("password.2");
                });

                it("removes null passwords from 'collection.prop.1' collection", () => {
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]).to.not.have.keys("password.4");
                });

                it("removes null passwords from a collection property inside 'collection.prop.1' collection", () => {
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]["collection.prop.2"])
                        .to.not.have.keys("password.6");
                });

                it("leaves non-null passwords untouched", () => {
                    expect(valuesWithoutNullPasswords.raw).to.contain.keys("password.1");
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]).to.contain.keys("password.3");
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]["collection.prop.2"])
                        .to.contain.keys("password.5");
                });

                it("leaves non-password properties untouched", () => {
                    expect(valuesWithoutNullPasswords.raw).to.contain.keys("non.password.property.1");
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"])
                        .to.contain.keys("non.password.property.2");
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]).to.contain.keys("object.property");
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]["collection.prop.2"])
                        .to.contain.keys("non.password.property.3");
                });

                it("leaves non schema properties untouched", () => {
                    expect(valuesWithoutNullPasswords.raw).to.contain.keys("_id");
                    expect(valuesWithoutNullPasswords.raw).to.contain.keys("_type");
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]["not.in.schema"])
                        .to.be.eql({ "property": "test", "password": null });
                });
            });

            context("when using a schema with inheritance", () => {
                const schema = new JSONSchema({
                    type: "object",
                    properties: {
                        "password.1": { properties: { inherited: { }, value: { format: "password" } } },
                        "password.2": { properties: { inherited: { }, value: { format: "password" } } },
                        "password.3": { properties: { inherited: { }, value: { format: "password" } } },
                        "collection.prop.1": {
                            type: "object",
                            properties: {
                                "password.4": { properties: { inherited: { }, value: { format: "password" } } },
                                "password.5": { properties: { inherited: { }, value: { format: "password" } } },
                                "password.6": { properties: { inherited: { }, value: { format: "password" } } },
                                "collection.prop.2": {
                                    type: "object",
                                    properties: {
                                        "password.7": { properties: { inherited: { }, value: { format: "password" } } },
                                        "password.8": { properties: { inherited: { }, value: { format: "password" } } },
                                        "password.9": { properties: { inherited: { }, value: { format: "password" } } }
                                    }
                                }
                            }
                        }
                    }
                });

                const values = new JSONValues({
                    _id: "",
                    _type: {},
                    "password.1": { value: "password", inherited: false },
                    "password.2": { value: null, inherited: true },
                    "password.3": { value: null, inherited: false },
                    "collection.prop.1": {
                        "password.4": { value: "password", inherited: false },
                        "password.5": { value: null, inherited: true },
                        "password.6": { value: null, inherited: false },
                        "collection.prop.2": {
                            "password.7": { value: "password", inherited: false },
                            "password.8": { value: null, inherited: true },
                            "password.9": { value: null, inherited: false }
                        },
                        "not.in.schema": {
                            "password.1": { value: "password", inherited: false },
                            "password.2": null, inherited: false
                        }
                    }
                });

                const valuesWithoutNullPasswords = values.removeNullPasswords(schema);

                it("removes non-grouped null passwords", () => {
                    expect(valuesWithoutNullPasswords.raw["password.2"]).to.be.eql({ inherited: true });
                    expect(valuesWithoutNullPasswords.raw["password.3"]).to.be.eql({ inherited: false });
                });

                it("removes null passwords from 'collection.prop.1' collection", () => {
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]["password.5"])
                        .to.be.eql({ inherited: true });
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]["password.6"])
                        .to.be.eql({ inherited: false });
                });

                it("removes null passwords from the collection property inside 'collection.prop.1' collection", () => {
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]["collection.prop.2"]["password.8"])
                        .to.be.eql({ inherited: true });
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]["collection.prop.2"]["password.9"])
                        .to.be.eql({ inherited: false });
                });

                it("leaves non-null passwords untouched", () => {
                    expect(valuesWithoutNullPasswords.raw["password.1"])
                        .to.be.eql({ value: "password", inherited: false });
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]["password.4"])
                        .to.be.eql({ value: "password", inherited: false });
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]["collection.prop.2"])
                        .to.contain.keys("password.7");
                });

                it("leaves non schema properties untouched", () => {
                    expect(valuesWithoutNullPasswords.raw).to.contain.keys("_id");
                    expect(valuesWithoutNullPasswords.raw).to.contain.keys("_type");
                    expect(valuesWithoutNullPasswords.raw["collection.prop.1"]["not.in.schema"])
                        .to.be.eql({
                            "password.1": { value: "password", inherited: false },
                            "password.2": null, inherited: false
                        });
                });
            });

            context("when using a schema with the 'defaults' property", () => {
                const schema = new JSONSchema({
                    type: "object",
                    properties: {
                        defaults: {
                            type: "object",
                            properties: {
                                "password.1": { format: "password" },
                                "password.2": { format: "password" }
                            }
                        }
                    }
                });

                const values = new JSONValues({
                    defaults: {
                        "password.1": "password",
                        "password.2": null
                    }
                });

                const valuesWithoutNullPasswords = values.removeNullPasswords(schema);

                it("removes null passwords from 'defaults' collection", () => {
                    expect(valuesWithoutNullPasswords.raw.defaults).to.not.have.keys("password.2");
                });

                it("leaves non-null passwords from 'defaults' collection untouched", () => {
                    expect(valuesWithoutNullPasswords.raw.defaults).to.contain.keys("password.1");
                });
            });
        });

        describe("#nullifyEmptyPasswords", () => {
            const values = new JSONValues({
                _id: "",
                _type: {},
                "password.1": "password",
                "password.2": "",
                "non.password.prop": ""
            });

            const valuesWithNullifiedPasswords = values.nullifyEmptyPasswords(["password.1", "password.2"]);

            it("nullifies empty passwords", () => {
                expect(valuesWithNullifiedPasswords.raw["password.2"]).to.be.eql(null);
            });

            it("leaves non-empty passwords untouched", () => {
                expect(valuesWithNullifiedPasswords.raw["password.1"]).to.be.eql("password");
            });

            it("leaves empty non-passwords untouched", () => {
                expect(valuesWithNullifiedPasswords.raw["non.password.prop"]).to.be.eql("");
            });
        });
    });
});
