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
 * Copyright 2018 ForgeRock AS.
 */

import { Form, Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import Footer from "org/forgerock/openam/ui/admin/views/realms/common/new/Footer";
import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";

const NewGroup = ({ id, isCreateAllowed, isValidId, onCreate, onIdChange }) => {
    const footer = (
        <Footer
            backRoute={ Router.configuration.routes.realmsIdentities }
            isCreateAllowed={ isCreateAllowed }
            onCreateClick={ onCreate }
        />
    );

    return (
        <div>
            <PageHeader title={ t("console.identities.groups.new.title") } />
            <Panel footer={ footer }>
                <Form horizontal>
                    <FormGroupInput
                        isValid={ isValidId }
                        label={ t("console.identities.groups.new.groupId") }
                        onChange={ onIdChange }
                        validationMessage={ t("console.common.validation.invalidCharacters") }
                        value={ id }
                    />
                </Form>
            </Panel>
        </div>
    );
};

NewGroup.propTypes = {
    id: PropTypes.string.isRequired,
    isCreateAllowed: PropTypes.bool.isRequired,
    isValidId: PropTypes.bool.isRequired,
    onCreate: PropTypes.func.isRequired,
    onIdChange: PropTypes.func.isRequired
};

export default NewGroup;
