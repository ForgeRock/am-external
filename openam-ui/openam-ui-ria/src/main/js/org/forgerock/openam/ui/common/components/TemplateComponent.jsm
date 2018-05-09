/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { View } from "backbone";

/**
 * A component for rendering a Handlebars template with some associated data.
 *
 * This component is not designed for extension.
 */
export default class TemplateComponent extends View {
    /**
     * Initialise the component.
     *
     * @param {String} template - the Handlebars template (contents not filename).
     */
    initialize ({ template }) {
        this.template = template;
    }
    render () {
        const html = this.template(this.data);
        this.$el.html(html);
        return this;
    }
    setData (data) {
        this.data = data;
    }
}
