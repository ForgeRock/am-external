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

/**
 * Refer to the following naming convention, when adding new functions to this class:
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
 * // The structure of JSON Value documents emitted from OpenAM is expected to be the following:
 * {
 *   {
 *     globalProperty: true, // Global values (OpenAM wide) are listed at the top-level
 *     default: { ... }, // Default values are organisation (Realm) level values and are nested under "default"
 *     dynamic: { ... } // Dynamic values are user level values (OpenAM wide) and are nested under "dynamic"
 *   }
 * }
 */
define([
    "lodash"
], (_) => {
    function groupTopLevelSimpleValues (raw) {
        const collectionProperties = _(raw)
            .pick((property) => _.isObject(property) && !_.isArray(property))
            .keys()
            .value();

        const simplePropertiesToGroup = _.omit(raw, (prop, key) => {
            return _.startsWith(key, "_") || key === "defaults" || collectionProperties.indexOf(key) !== -1;
        });

        if (_.isEmpty(simplePropertiesToGroup)) {
            return raw;
        }

        const values = {
            ..._.omit(raw, _.keys(simplePropertiesToGroup)),
            global: simplePropertiesToGroup
        };

        return values;
    }

    /**
    * Ungroups collection properties, moving them one level up.
    *
    * @param   {Object} raw Values
    * @param   {string} groupKey Group key of the property value object
    * @returns {JSONValues} JSONValues object with new value set
    */
    function ungroupCollectionProperties (raw, groupKey) {
        const collectionProperties = _.pick(raw[groupKey], (value) => {
            return _.isObject(value) && !_.isArray(value);
        });

        if (_.isEmpty(collectionProperties)) {
            return raw;
        }

        const collectionPropertiesKeys = _.keys(collectionProperties);
        const allPropertiesKeys = _.keys(raw.defaults);
        const nonGroupedProperties = _.difference(allPropertiesKeys, collectionPropertiesKeys);

        if (!_.isEmpty(nonGroupedProperties)) {
            console.warn(`Detected properties which do not belong to any group: [${nonGroupedProperties}]. ` +
                "They will be displayed under the 'Realm Defaults' tab");
        }

        const values = { ...raw, ...collectionProperties };

        values[`_${groupKey}CollectionProperties`] = collectionPropertiesKeys;
        values[groupKey] = _.omit(values[groupKey], collectionPropertiesKeys);

        if (_.isEmpty(values[groupKey])) {
            delete values[groupKey];
        }

        return values;
    }

    function isCollection (schema, key) {
        return _.has(schema.properties[key], "properties");
    }

    return class JSONValues {
        constructor (values) {
            const hasDefaults = _.has(values, "defaults");
            const hasDynamic = _.has(values, "dynamic");

            if (hasDefaults || hasDynamic) {
                values = groupTopLevelSimpleValues(values);
            }

            if (hasDefaults) {
                values = ungroupCollectionProperties(values, "defaults");
            }

            this.raw = Object.freeze(values);
        }
        addInheritance (inheritance) {
            const valuesWithInheritance = _.mapValues(this.raw, (value, key) => {
                return _.has(inheritance[key], "inherited")
                    ? { value, inherited: inheritance[key].inherited }
                    : value;
            });

            return new JSONValues(valuesWithInheritance);
        }
        /**
         * Adds value for the property.
         *
         * @param   {string} path Property key
         * @param   {string} key Key of the property value object
         * @param   {string} value Value to be set
         * @returns {JSONValues} JSONValues object with new value set
         */
        addValueForKey (path, key, value) {
            const clone = _.cloneDeep(this.raw);
            clone[path][key] = value;
            return new JSONValues(clone);
        }
        extend (object) {
            return new JSONValues(_.extend({}, this.raw, object));
        }
        /**
         * Creates a JSONValues object with the same keys as this JSONValues object and values generated by running each property through iteratee.
         * "Properties" are defined as the leafs of the object, for values without inheritance information this will be the raw primitive value, for
         * values with inheritance information this will be an Object with value and inherited attributes.
         * <p/>
         * iteratee Function signiture is similar to #mapValues except the key is the full key within the JSONValues object. e.g.
         * @param   {Function} iteratee The function invoked per property iteration.
         * @returns {JSONValues} Returns the new mapped JSONValues.
         * @example values.mapProperties((value, key) => { ... } => value = { ... } or primate, key = "some.key"
         */
        mapProperties (iteratee) {
            return new JSONValues(_.mapValues(this.raw, (parentValue, parentKey) => {
                if (_.startsWith(parentKey, "_")) {
                    return parentValue;
                } else {
                    return _.mapValues(parentValue, (value, key, object) => {
                        return iteratee(value, `${parentKey}.${key}`, object);
                    });
                }
            }));
        }
        getEmptyValueKeys () {
            function isEmpty (value) {
                if (_.isNumber(value)) {
                    return false;
                } else if (_.isBoolean(value)) {
                    return false;
                }

                return _.isEmpty(value);
            }

            const keys = [];

            _.forIn(this.raw, (value, key) => {
                if (isEmpty(value)) {
                    keys.push(key);
                }
            });

            return keys;
        }
        omit (predicate) {
            return new JSONValues(_.omit(this.raw, predicate));
        }
        pick (predicate) {
            return new JSONValues(_.pick(this.raw, predicate));
        }
        removeInheritance () {
            return new JSONValues(_.mapValues(this.raw, (property) => {
                return _.has(property, "value") ? property.value : property;
            }));
        }
        /**
         * Returns a new JSONValues object with any null password properties that have inheritance (either on or off)
         * with the value removed, or any null password properties that do not have inheritance removed completely.
         *
         * @param   {JSONSchema} jsonSchema Corresponding JSONSchema object
         * @returns {JSONValues} a new JSONValues Object
         * @example
         * const schema = new JSONSchema(...);
         * const values = new JSONValues({
         *      "property.1": "test",
         *      "password.2": "password",
         *      "password.3": null,
         *      "password.4": { value: null, inherited: true },
         *      "password.5": { value: null, inherited: false },
         *      "password.6": { value: "password", inherited: false }
         *      "collection.prop.1": {
         *          "property.1": "test",
         *          "password.2": "password",
         *          "password.3": null,
         *          "password.4": { value: null, inherited: true },
         *          "password.5": { value: null, inherited: false },
         *          "password.6": { value: "password", inherited: false }
         *      },
         *      "not.in.schema.1": "value"
         *      "not.in.schema.2": {
         *          "password.1": { value: "password", inherited: false },
         *          "password.2": null, inherited: false
         *      }
         * });
         *
         * values.removeNullPasswords(schema); // => {
         *      "property.1": "test",
         *      "password.2": "password",
         *      "password.4": { inherited: true },
         *      "password.5": { inherited: false },
         *      "password.6": { value: "password" }
         *      "collection.prop.1": {
         *          "property.1": "test",
         *          "password.2": "password",
         *          "password.4": { inherited: true },
         *          "password.5": { inherited: false },
         *          "password.6": { value: "password", inherited: false }
         *      },
         *      "not.in.schema.1": "value"
         *      "not.in.schema.2": {
         *          "password.1": { value: "password", inherited: false },
         *          "password.2": null, inherited: false
         *      }
         * });
         */
        removeNullPasswords (jsonSchema) {
            const isNullPassword = (value, schema, path) => _.isNull(value) && _.get(schema, path) === "password";
            const omitNullPasswords = (values, schema) => {
                return _.reduce(values, (result, value, key) => {
                    if (jsonSchema.hasInheritance(schema.properties[key])) {
                        result[key] = isNullPassword(value.value, schema.properties[key], "properties.value.format")
                            ? { inherited: value.inherited } // return only the inheritance flag
                            : value;
                    } else if (isCollection(schema, key)) {
                        result[key] = omitNullPasswords(value, schema.properties[key]);
                    } else if (isNullPassword(value, schema.properties[key], "format")) {
                        // We explicitly do not include null passwords
                    } else {
                        result[key] = value;
                    }
                    return result;
                }, {});
            };

            return new JSONValues(omitNullPasswords(this.raw, jsonSchema.raw));
        }
        /**
         * Returns a new JSONValues object with all it's empty passwords set to null.
         *
         * @param   {string[]} passwords An array of password property keys
         * @returns {JSONValues} a new JSONValues Object with any empty passwords set to null
         */
        nullifyEmptyPasswords (passwords) {
            return new JSONValues(_.mapValues(this.raw, (value, key) => {
                const isEmptyPassword = passwords.indexOf(key) !== -1 && _.isEmpty(value);
                return isEmptyPassword ? null : value;
            }));
        }
        /**
         * @see OPENAM-10769. The problem is in the initial transformation of the values object. The values are transformed
         * in isolation with the corresponding schema. This might result in a problem, when field properties
         * are recognised as collection properties and are transformed to display them as tabs on the UI. This method reverts false
         * collection properties back into the field properties (initial pre-transformation state). This is a workaround
         * that we are hoping to get rid of when we switch to a better json schema library.
         * Note: JSONValues field properties and collection properties have the same structure, so there is no way
         * to distinguish between them. Only JSONSchema has enough knowledge to distinguish these two.
         * This is a mutating function.
         *
         * @param   {JSONSchema} schema Corresponding JSONSchema object
         * @returns {JSONValues} JSONValues object ("this" mutated)
         */
        revertFalseCollections (schema) {
            let raw = _.cloneDeep(this.raw);

            if (raw._defaultsCollectionProperties) {
                const falseCollections = [];

                // generate a list of false collection properties based on schema
                _.forEach(raw._defaultsCollectionProperties, (property) => {
                    if (!isCollection(schema.raw, property)) {
                        falseCollections.push(property);
                    }
                });

                if (falseCollections.length) {
                    // remove false collection properties from the list of collections keys
                    raw._defaultsCollectionProperties =
                        _.difference(raw._defaultsCollectionProperties, falseCollections);
                    if (_.isEmpty(raw._defaultsCollectionProperties)) {
                        delete raw._defaultsCollectionProperties;
                    }

                    // re-generate "defaults" property as it may have been removed if all its properties are field properies
                    if (!_.has(raw, "defaults")) {
                        raw.defaults = {};
                    }

                    // assign field properties back into the "defaults" property
                    _.assign(raw.defaults, _.pick(raw, falseCollections));
                    raw = _.omit(raw, falseCollections);
                }
            }

            this.raw = Object.freeze(raw);

            return this;
        }
        toJSON () {
            let json = _.cloneDeep(this.raw);

            const wrapCollectionProperties = (json, propertyKey) => {
                let data = _.cloneDeep(json);

                const collectionPropertiesKeys = data[`_${propertyKey}CollectionProperties`];
                const collectionProperties = _.pick(data, collectionPropertiesKeys);
                data[propertyKey] = { ...data[propertyKey], ...collectionProperties };
                data = _.omit(data, collectionPropertiesKeys);

                return data;
            };

            const collectionPropertiesPresent = (json, propertyKey) => {
                const collectionPropertiesKeys = json[`_${propertyKey}CollectionProperties`];
                return collectionPropertiesKeys && !_.isEmpty(collectionPropertiesKeys);
            };

            if (collectionPropertiesPresent(json, "defaults")) {
                json = wrapCollectionProperties(json, "defaults");
                delete json._defaultsCollectionProperties;
            }

            json = { ...json, ...json.global };
            delete json.global;

            return JSON.stringify(json);
        }
    };
});
