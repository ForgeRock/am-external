/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
