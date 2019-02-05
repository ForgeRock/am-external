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
 * Copyright 2016-2018 ForgeRock AS.
 */

import i18next from "i18next";
import _ from "lodash";

import transformBooleanTypeToCheckboxFormat from "./schemaTransforms/transformBooleanTypeToCheckboxFormat";
import transformEnumTypeToString from "./schemaTransforms/transformEnumTypeToString";
import warnOnInferredPasswordWithoutFormat from "./schemaTransforms/warnOnInferredPasswordWithoutFormat";

/**
 * Represents a JSON Schema.
 * <p/>
 * <h2>Function naming conventions</h2>
 * Refer to the following naming convention, when adding new functions to this class:
 * <ul>
 *   <li>For <strong>query</strong> functions, which do not return a new instance of <code>JSONSchema</code>, use <code>#get*</code></li>
 *   <li>For <strong>transform</strong> functions, which do not loose data, use <code>#to*</code> and <code>#from*</code></li>
 *   <li>For <strong>modification</strong> functions, which loose the data, use <code>add*</code> and <code>#remove*</code></li>
 *   <li>For functions, which <strong>check for presense</strong>, use <code>#has*</code> and <code>#is*</code></li>
 *   <li>For <strong>utility</strong> functions use simple verbs, e.g. <code>#omit</code>, <code>#pick</code>, etc.</li>
 * </ul>
 * @module
 * @example
 * // The structure of JSON Schema documents emitted from OpenAM is expected to be the following:
 * {
 *   properties: {
 *     globalProperty: true, // Global properties (OpenAM wide) are listed at the top-level
 *     default: { ... }, // Default properties are organisation (Realm) level properties and are nested under "default"
 *     dynamic: { ... } // Dynamic properties are user level properties (OpenAM wide) and are nested under "dynamic"
 *   },
 *   type: "object"
 * }
 */

/**
 * Determines whether the specified object is of type <code>object</code>
 * @param   {Object}  object Object to determine the type of
 * @returns {Boolean}        Whether the object is of type <code>object</code>
 */
function isObjectType (object) {
    return object.type === "object";
}

function groupTopLevelSimpleProperties (raw) {
    const collectionProperties = _(raw.properties)
        .pick((property) => _.has(property, "properties"))
        .keys()
        .value();

    const predicate = ["defaults", ...collectionProperties];
    const simplePropertiesToGroup = _.omit(raw.properties, ...predicate);

    if (_.isEmpty(simplePropertiesToGroup)) {
        return raw;
    }

    const schema = _.cloneDeep(raw);

    schema.properties = {
        ..._.omit(schema.properties, _.keys(simplePropertiesToGroup)),
        global: {
            properties: simplePropertiesToGroup,
            propertyOrder: -10,
            title: i18next.t("console.common.globalAttributes"),
            type: "object"
        }
    };

    return schema;
}

function throwOnNoSchemaRootType (schema) {
    if (!schema.type) {
        throw new Error("[JSONSchema] No \"type\" attribute found on schema root object.");
    }
}

/**
* Ungroups collection properties, moving them one level up.
*
* @param   {Object} raw Schema
* @param   {string} groupKey Group key of the property value object
* @returns {JSONSchema} JSONSchema new JSONSchema object
*/
function ungroupCollectionProperties (raw, groupKey) {
    const collectionProperties = _.pick(raw.properties[groupKey].properties, (value) => {
        return value.type === "object" && _.has(value, "properties");
    });

    if (_.isEmpty(collectionProperties)) {
        return raw;
    }

    const schema = _.cloneDeep(raw);
    schema.properties = { ...schema.properties, ...collectionProperties };
    schema.properties[groupKey].properties =
        _.omit(schema.properties[groupKey].properties, _.keys(collectionProperties));

    if (_.isEmpty(schema.properties[groupKey].properties)) {
        delete schema.properties[groupKey];
    }

    return schema;
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
 * Iterates over a scheam, transforming adding appropriate warnings.
 * @param {Object} schema the schema to be transformed
 * @returns {Object} the transformed schema
 */
function cleanJSONSchema (schema) {
    eachProperty(schema, [
        transformBooleanTypeToCheckboxFormat,
        transformEnumTypeToString,
        warnOnInferredPasswordWithoutFormat
    ]);

    return schema;
}

export default class JSONSchema {
    constructor (schema) {
        throwOnNoSchemaRootType(schema);

        const hasDefaults = _.has(schema, "properties.defaults");
        const hasDynamic = _.has(schema, "properties.dynamic");

        if (hasDefaults || hasDynamic) {
            schema = groupTopLevelSimpleProperties(schema);
        }

        if (hasDefaults) {
            schema = ungroupCollectionProperties(schema, "defaults");
        }

        schema = cleanJSONSchema(schema);

        this.raw = Object.freeze(schema);
    }
    addDefaultProperties (keys) {
        const schema = _.cloneDeep(this.raw);
        schema.defaultProperties = _.union(schema.defaultProperties, keys);
        return new JSONSchema(schema);
    }
    hasDefaultProperties () {
        return !_.isEmpty(this.raw.defaultProperties);
    }
    getEnableKey () {
        const key = `${_.camelCase(this.raw.title)}Enabled`;
        if (this.raw.properties[key]) {
            return key;
        }
    }
    getEnableProperty () {
        return this.pick(this.getEnableKey());
    }
    getKeys (sort) {
        sort = typeof sort === "undefined" ? false : sort;

        if (sort) {
            const sortedSchemas = _.sortBy(_.map(this.raw.properties), "propertyOrder");
            return _.map(sortedSchemas, (schema) => _.findKey(this.raw.properties, schema));
        } else {
            return _.keys(this.raw.properties);
        }
    }
    getPasswordKeys () {
        const passwordProperties = _.pick(this.raw.properties, (property) => {
            const propertyPath = this.hasInheritance(property) ? "properties.value.format" : "format";
            return _.get(property, propertyPath) === "password";
        });

        return _.keys(passwordProperties);
    }
    getPropertiesAsSchemas () {
        return _.mapValues(this.raw.properties, (property) => new JSONSchema(property));
    }
    getRequiredPropertyKeys () {
        const requiredProperties = _.pick(this.raw.properties, (property) => {
            const propertyPath = this.hasInheritance(property) ? "properties.value.required" : "required";
            return _.get(property, propertyPath) === true;
        });

        return _.keys(requiredProperties);
    }
    hasEnableProperty () {
        return !_.isUndefined(this.raw.properties[`${_.camelCase(this.raw.title)}Enabled`]);
    }
    hasInheritance (property) {
        return !_.isEmpty(property) && property.type === "object" && _.has(property, "properties.inherited");
    }
    /**
     * Whether this schema objects' properties are all schemas in their own right.
     * If true, this object is a simply a container for other schemas.
     * @returns {Boolean} Whether this object is a collection
     */
    isCollection () {
        if (this.raw.properties) {
            return _.every(this.raw.properties, (property) => property.type === "object");
        } else {
            return false;
        }
    }
    isEmpty () {
        return _.isEmpty(this.raw.properties);
    }
    pick (predicate) {
        const schema = _.cloneDeep(this.raw);
        schema.properties = _.pick(this.raw.properties, predicate);

        return new JSONSchema(schema);
    }
    omit (predicate) {
        const schema = _.cloneDeep(this.raw);
        schema.properties = _.omit(this.raw.properties, predicate);

        return new JSONSchema(schema);
    }
    /**
     * Returns a new JSONSchema with only the required and default properties present.
     * @returns {JSONSchema} JSONSchema object with only the required and default properties present.
     */
    removeUnrequiredNonDefaultProperties () {
        const schema = _.cloneDeep(this.raw);
        const defaultProperties = this.raw.defaultProperties;
        schema.properties = _.pick(this.raw.properties, (property, key) => {
            const required = this.hasInheritance(property) ? property.properties.value.required : property.required;
            return _.contains(defaultProperties, key) || required;
        });
        return new JSONSchema(schema);
    }
    /**
     * Flattens schema properties to enable schema to be renderable. Adds inheritance metadata to each property of
     * the schema, so JSONEditor knows whether to enable or disable the input field.
     * @param {JSONValues} values JSONValues object to take inheritance metadata from.
     * @returns {JSONSchema} Flattened JSONSchema object with inheritance metadata.
     */
    toFlatWithInheritanceMeta (values) {
        const schema = _.cloneDeep(this.raw);

        schema.properties = _.mapValues(this.raw.properties, (originalValue, propName) => {
            if (this.hasInheritance(originalValue)) {
                const value = originalValue.properties.value;
                const property = {
                    ..._.omit(originalValue, "properties"),
                    ...value,
                    isInherited: Boolean(values.raw[propName] && values.raw[propName].inherited)
                };
                return property;
            }

            return originalValue;
        });

        return new JSONSchema(schema);
    }
}