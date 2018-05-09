/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "backbone",
    "org/forgerock/openam/ui/common/components/table/InlineEditTable"
], (_, Backbone, InlineEditTable) => {
    const StaticResponseAttributesView = Backbone.View.extend({

        initialize ({ staticAttributes }) {
            this.staticAttributes = staticAttributes;
        },

        render () {
            const getFlattenedStaticAttributes = () => _.flatten(
                _.map(this.staticAttributes, (attribute) =>
                    _.map(attribute.propertyValues, (value) => ({ key: attribute.propertyName, value }))
                ));

            this.inlineEditList = new InlineEditTable({
                values: getFlattenedStaticAttributes()
            });
            this.$el.append(this.inlineEditList.render().$el);

            return this;
        },

        getGroupedData () {
            return _(this.inlineEditList.getData())
                .groupBy("key")
                .map((values, key) => ({
                    type: "Static",
                    propertyName: key,
                    propertyValues: _.map(values, "value")
                }))
                .value();
        }
    });

    return StaticResponseAttributesView;
});
