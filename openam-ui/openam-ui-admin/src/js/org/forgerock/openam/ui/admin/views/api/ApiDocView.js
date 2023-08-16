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

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import calculateHeight from "org/forgerock/openam/ui/admin/views/api/calculateHeight";
import IframeViewTemplate from "templates/admin/views/common/IframeViewTemplate";
const ApiDocView = AbstractView.extend({
    template: IframeViewTemplate,
    render () {
        this.data.src = `${Constants.context}/json/docs/api`;
        this.data.className = "am-iframe-full-width";
        this.parentRender(() => {
            this.resize();
            window.addEventListener("resize", _.debounce(this.resize.bind(this), 100));
        });
    },
    resize () {
        const height = calculateHeight();
        this.$el.find("[data-iframe]").height(height);
    },
    remove () {
        window.removeEventListener("resize", this.resize);
    }
});

export default new ApiDocView();