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
 * Copyright 2015-2019 ForgeRock AS.
 */

import $ from "jquery";
import form2js from "form2js/src/form2js";

import AnonymousProcessView from "org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView";
import KBAView from "org/forgerock/commons/ui/user/anonymousProcess/KBAView";

const SelfRegistrationView = AnonymousProcessView.extend({
    processType: "registration",
    i18nBase: "common.user.selfRegistration",
    getFormContent () {
        const form = $(this.element).find("form")[0];

        if (form.hasAttribute("data-kba-questions")) {
            return { "kba": KBAView.getQuestions() };
        } else {
            return form2js(form);
        }
    },
    renderProcessState (response) {
        AnonymousProcessView.prototype.renderProcessState.call(this, response).then(() => {
            if (response.type === "kbaSecurityAnswerDefinitionStage" && response.tag === "initial") {
                KBAView.render(response.requirements.properties.kba);
            }
        });
    }
});

export default new SelfRegistrationView();
