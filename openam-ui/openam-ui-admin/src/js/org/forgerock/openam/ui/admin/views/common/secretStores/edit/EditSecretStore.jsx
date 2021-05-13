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
 * Copyright 2018-2019 ForgeRock AS.
 */

import { Button, Panel, Tab, Tabs } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Fragment } from "react";

import Loading from "components/Loading";
import PageHeader from "components/PageHeader";

const EditSecretStore = ({ instance, isFetching, onDelete, mappingsChild, mappingsTitle, settingsChild }) => {
    if (isFetching) {
        return <Loading />;
    }
    const content = mappingsTitle
        ? (
            <Tabs animation defaultActiveKey={ 1 } id="editSecretStore" mountOnEnter unmountOnExit>
                <Tab eventKey={ 1 } title={ t("common.form.settings") } >
                    <Panel className="fr-panel-tab clearfix">
                        { settingsChild }
                    </Panel>
                </Tab>
                <Tab eventKey={ 2 } title={ mappingsTitle } >
                    <Panel className="fr-panel-tab clearfix">
                        { mappingsChild }
                    </Panel>
                </Tab>
            </Tabs>
        ) : (
            <Panel>
                { settingsChild }
            </Panel>
        );

    return (
        <Fragment>
            <PageHeader icon="eye" subtitle={ instance._type.name } title={ instance._id }>
                <Button onClick={ onDelete } >
                    <i className="fa fa-times" /> { t("common.form.delete") }
                </Button>
            </PageHeader>
            { content }
        </Fragment>
    );
};

EditSecretStore.propTypes = {
    instance: PropTypes.shape({
        _id: PropTypes.string.isRequired,
        _type: PropTypes.shape({
            name: PropTypes.string.isRequired
        }).isRequired
    }),
    isFetching: PropTypes.bool.isRequired,
    mappingsChild: PropTypes.node,
    mappingsTitle: PropTypes.string,
    onDelete: PropTypes.func.isRequired,
    settingsChild: PropTypes.node
};

export default EditSecretStore;
