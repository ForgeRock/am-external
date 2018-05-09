/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "sinon",
    "org/forgerock/openam/ui/common/models/JSONValues"
], (sinon, JSONValues) => {
    let i18next;
    let JSONSchema;
    describe("org/forgerock/openam/ui/common/models/JSONSchema", () => {
        beforeEach(() => {
            const injector = require("inject-loader!org/forgerock/openam/ui/common/models/JSONSchema");

            i18next = {
                t: sinon.stub().withArgs("console.common.global").returns("Global Attributes")
            };

            JSONSchema = injector({
                i18next
            });
        });

        describe("#constructor", () => {
            let schemaWithGlobalProps;
            let schemaWithDefaultsProps;
            let schemaWithDefaultsCollectionProps;

            beforeEach(() => {
                schemaWithGlobalProps = new JSONSchema({
                    "properties": {
                        "globalSimpleProperty": {},
                        "globalCollectionProperty": {
                            "type": "object",
                            "title": "",
                            "properties": {}
                        },
                        "dynamic": {}
                    },
                    "type": "object"
                });

                schemaWithDefaultsProps = new JSONSchema({
                    "properties": {
                        "defaults": {
                            "type": "object",
                            "title": "",
                            "properties": {
                                "defaultsSimpleProperty": {},
                                "defaultsCollectionProperty": {
                                    type: "object",
                                    title: "",
                                    properties: {}
                                }
                            }
                        }
                    },
                    "type": "object"
                });

                schemaWithDefaultsCollectionProps = new JSONSchema({
                    "properties": {
                        "defaults": {
                            "type": "object",
                            "title": "",
                            "properties": {
                                "defaultsCollectionProperty": {
                                    type: "object",
                                    title: "",
                                    properties: {}
                                }
                            }
                        }
                    },
                    "type": "object"
                });
            });

            // Global properties
            it("groups the top-level simple properties under a \"global\" property", () => {
                expect(schemaWithGlobalProps.raw.properties).to.contain.keys("global");
                expect(schemaWithGlobalProps.raw.properties.global.properties).to.contain.keys("globalSimpleProperty");
            });

            it("groups the top-level simple properties with title", () => {
                expect(i18next.t).to.be.calledWith("console.common.globalAttributes");
                expect(schemaWithGlobalProps.raw.properties.global.title).eq("Global Attributes");
            });

            it("groups the top-level simple properties with property order", () => {
                expect(schemaWithGlobalProps.raw.properties.global.propertyOrder).eq(-10);
            });

            it("does not group the top-level collection properties under a \"global\" property", () => {
                expect(schemaWithGlobalProps.raw.properties).to.contain.keys("global");
                expect(schemaWithGlobalProps.raw.properties.global.properties).to.not.have
                    .keys("globalCollectionProperty");
            });

            //Defaults properties
            it("ungroups \"defaults\" collection properties, moving them one level up", () => {
                expect(schemaWithDefaultsProps.raw.properties).to.contain.keys("defaultsCollectionProperty");
            });

            it("does not ungroup \"defaults\" simple properties", () => {
                expect(schemaWithDefaultsProps.raw.properties.defaults.properties).to.contain
                    .keys("defaultsSimpleProperty");
            });

            it("ungroups \"defaults\" collection properties, moving them one level up (collection props only)", () => {
                expect(schemaWithDefaultsCollectionProps.raw.properties).to.contain.keys("defaultsCollectionProperty");
            });

            it("removes \"defaults\" property when there are no simple props", () => {
                expect(schemaWithDefaultsCollectionProps.raw.properties).to.not.have.keys("defaults");
            });
        });

        describe("#hasInheritance", () => {
            it("returns true for an inheritable property", () => {
                const jsonSchema = new JSONSchema({
                    type: "object",
                    properties: {
                        propertyCollection: {
                            type: "object",
                            title: "",
                            properties: {
                                inherited: {}
                            }
                        }
                    }
                });

                expect(jsonSchema.hasInheritance(jsonSchema.raw.properties.propertyCollection)).to.be.true;
            });

            it("returns false for a non-inheritable property", () => {
                const jsonSchema = new JSONSchema({
                    type: "object",
                    properties: {
                        propertyCollection: {
                            type: "object",
                            title: "",
                            properties: {
                                property: {}
                            }
                        }
                    }
                });

                expect(jsonSchema.hasInheritance(jsonSchema.raw.properties.propertyCollection)).to.be.false;
            });
        });

        describe("#removeUnrequiredNonDefaultProperties", () => {
            let schema;
            beforeEach(() => {
                const jsonSchema = new JSONSchema({
                    "type": "object",
                    "properties": {
                        "requiredDefault": {
                            required: true
                        },
                        "unrequiredDefault": {
                            required: false
                        },
                        "unrequiredNonDefault": {
                            required: false
                        },
                        "requiredNonDefault": {
                            required: true
                        },
                        "requiredDefaultInherited": {
                            "type": "object",
                            "properties": {
                                "inherited" : {},
                                "value": { required: true }
                            }
                        },
                        "unrequiredDefaultInherited": {
                            "type": "object",
                            "properties": {
                                "inherited" : {},
                                "value": { required: false }
                            }
                        },
                        "unrequiredNonDefaultInherited": {
                            "type": "object",
                            "properties": {
                                "inherited" : {},
                                "value": { required: false }
                            }
                        },
                        "requiredNonDefaultInherited": {
                            "type": "object",
                            "properties": {
                                "inherited" : {},
                                "value": { required: true }
                            }
                        }
                    },
                    "defaultProperties" : [
                        "requiredDefault", "unrequiredDefault", "requiredDefaultInherited", "unrequiredDefaultInherited"
                    ]
                });
                schema = jsonSchema.removeUnrequiredNonDefaultProperties();
            });
            it("removes properties where \"required\" is \"false\"", () => {
                expect(schema.raw.properties).to.have.keys([
                    "requiredDefault",
                    "unrequiredDefault",
                    "requiredNonDefault",
                    "requiredDefaultInherited",
                    "unrequiredDefaultInherited",
                    "requiredNonDefaultInherited"
                ]);
                expect(schema.raw.properties).not.to.have.keys([
                    "unrequiredNonDefault",
                    "unrequiredNonDefaultInherited"
                ]);
            });
        });

        describe("#toFlatWithInheritanceMeta", () => {
            const jsonValues = new JSONValues({
                "com.iplanet.am.smtphost":{
                    "value":"localhost",
                    "inherited":true
                },
                "com.iplanet.am.smtpport":{
                    "value":25,
                    "inherited":true
                }
            });
            let schema;

            beforeEach(() => {
                const jsonSchema = new JSONSchema({
                    "title":"Mail Server",
                    "type":"object",
                    "propertyOrder":3,
                    "properties":{
                        "com.iplanet.am.smtphost":{
                            "title":"Mail Server Host Name",
                            "type":"object",
                            "propertyOrder":0,
                            "description":"(property name: com.iplanet.am.smtphost)",
                            "properties":{
                                "value":{
                                    "type":"string",
                                    "required":false
                                },
                                "inherited":{
                                    "type":"boolean",
                                    "required":true
                                }
                            }
                        }
                    }
                });
                schema = jsonSchema.toFlatWithInheritanceMeta(jsonValues);
            });

            it("flattens inherited property values onto the top-level properties", () => {
                expect(schema.raw.properties).to.contain.keys("com.iplanet.am.smtphost");
                expect(schema.raw.properties["com.iplanet.am.smtphost"]).to.contain
                    .keys("type", "required");
            });

            it("sets the title on the flattened properties", () => {
                expect(schema.raw.properties["com.iplanet.am.smtphost"].title).eq("Mail Server Host Name");
            });

            it("adds 'isInherited' key to each property of the schema", () => {
                expect(schema.raw.properties["com.iplanet.am.smtphost"]).to.contain.keys("isInherited");
                expect(schema.raw.properties["com.iplanet.am.smtphost"].isInherited).eq(true);
            });
        });
    });
});