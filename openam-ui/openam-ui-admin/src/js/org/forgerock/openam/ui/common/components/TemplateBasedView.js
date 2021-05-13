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
 * Copyright 2016-2019 ForgeRock AS.
 */

import _ from "lodash";

import Backbone from "backbone";

export default Backbone.View.extend({
    initialize ({ template, data = {}, callback = _.noop }) {
        if (!template) {
            throw new Error("[TemplateBasedView] No \"template\" found.");
        }
        this.template = template;
        this.callback = callback;
        this.data = data;
    },
    render () {
        const html = this.template(this.data);

        this.$el.html(html);
        this.callback();

        return this;
    }
});
