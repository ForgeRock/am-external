/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/SMSServiceUtils
 */
define([
    "lodash",
    "org/forgerock/openam/ui/common/util/Promise"
], (_, Promise) => {
    const obj = {};

    /**
     * Adds a type attribute of <code>object</code> if not present
     * @param {Object} schema Schema to check
     */
    function addSchemaType (schema) {
        if (!schema.type) {
            console.warn("JSON schema detected without root type attribute! Defaulting to \"object\" type.");
            schema.type = "object";
        }
    }
    /**
     * Determines whether the specified object is of type <code>object</code>
     * @param   {Object}  object Object to determine the type of
     * @returns {Boolean}        Whether the object is of type <code>object</code>
     */
    function isObjectType (object) {
        return object.type === "object";
    }
    /**
     * Recursively invokes the specified functions over each object's properties
     * @param {Object} object   Object with properties
     * @param {Array} callbacks Array of functions
     */
    function eachProperty (object, callbacks) {
        if (isObjectType(object)) {
            _.forEach(object.properties, (property, key) => {
                _.forEach(callbacks, (callback) => {
                    callback(property, key);
                });

                if (isObjectType(property)) {
                    eachProperty(property, callbacks);
                }
            });
        }
    }
    /**
    * Removes schema <code>defaults</code> attribute if present
    * @param {Object} schema Schema to check
    */
    function removeSchemaDefaults (schema) {
        if (schema.properties.defaults) {
            console.warn("JSON schema detected with a \"defaults\" section present in it's properties. Removing.");
            delete schema.properties.defaults;
        }
    }
    /**
    * Transforms boolean types to checkbox format
    * FIXME: To fix server side? Visual only?
    * @param {Object} property Property to transform
    */
    function transformBooleanTypeToCheckboxFormat (property) {
        if (property.hasOwnProperty("type") && property.type === "boolean") {
            property.format = "checkbox";
        }
    }
    /**
    * Recursively add string type to enum
    * FIXME: To fix server side
    * @param {Object} property Property to transform
    */
    function transformEnumTypeToString (property) {
        if (property.hasOwnProperty("enum")) {
            property.type = "string";
        }
    }
    /**
     * Warns if a property is inferred to be a password and does not have a format of password
     * @param {Object} property Property to transform
     * @param {String} name Raw property name
     */
    function warnOnInferredPasswordWithoutFormat (property, name) {
        var possiblePassword = name.toLowerCase().indexOf("password", name.length - 8) !== -1,
            hasFormat = property.format === "password";
        if (property.type === "string" && possiblePassword && !hasFormat) {
            console.warn("JSON schema password property detected (inferred) without format of \"password\"");
        }
    }

    /**
     * Sanitizes JSON Schemas.
     * @param  {Object} schema Schema to sanitize
     * @returns {Object}       Sanitized schema
     * @deprecated
     */
    obj.sanitizeSchema = function (schema) {
        console.warn("[SMSServiceUtils] \"#sanitizeSchema\" is deprecated. Use JSONSchema model instead.");
        var transformedSchema = _.cloneDeep(schema);

        /**
         * Missing and superfluous attribute checks
         */
        addSchemaType(transformedSchema);
        removeSchemaDefaults(transformedSchema);

        /**
         * Property transforms & warnings
         */
        eachProperty(transformedSchema, [
            transformBooleanTypeToCheckboxFormat,
            transformEnumTypeToString,
            warnOnInferredPasswordWithoutFormat
        ]);

        /**
         * Additional attributes
         */
        // Adds attribute indicating if all the schema properties are of the type "object" (hence grouped)
        transformedSchema.grouped = _.every(transformedSchema.properties, isObjectType);
        // Create ordered array
        transformedSchema.orderedProperties = _.sortBy(_.map(transformedSchema.properties, (value, key) => {
            value._id = key;
            return value;
        }), "propertyOrder");

        return transformedSchema;
    };

    obj.sortResultBy = function (attribute) {
        return function (data) {
            data.result = _.sortBy(data.result, attribute);
        };
    };

    obj.schemaWithDefaults = (delegate, url) =>
        Promise.all([
            delegate.serviceCall({
                url: `${url}?_action=schema`,
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }),
            delegate.serviceCall({
                url: `${url}?_action=template`,
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            })
        ]).then((results) => {
            return {
                schema: obj.sanitizeSchema(results[0][0]),
                values: results[1][0]
            };
        });

    obj.schemaWithValues = function (delegate, url) {
        return Promise.all([
            delegate.serviceCall({
                url: `${url}?_action=schema`,
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }),
            delegate.serviceCall({
                url,
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            })
        ]).then((results) => {
            return {
                schema: obj.sanitizeSchema(results[0][0]),
                values: results[1][0]
            };
        });
    };

    return obj;
});
