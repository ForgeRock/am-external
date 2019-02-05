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

import _ from "lodash";
import Backbone from "backbone";
import Handlebars from "handlebars-template-loader/runtime";

export default Backbone.View.extend({
    initialize (options) {
        if (!_.isString(options.partial)) {
            throw new TypeError("[PartialBasedView] \"partial\" argument is not a String.");
        }

        this.options = options;
    },
    render () {
        const html = Handlebars.partials[this.options.partial](this.options.data);
        this.$el.html(html);
        return this;
    }
});