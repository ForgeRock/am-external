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
 * Copyright 2019 ForgeRock AS.
 */

import { findKey, minBy, sortBy, toArray } from "lodash";
import { Tab, Tabs } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

import EditEntityFormContainer from "./form/EditEntityFormContainer";
import EditEntityPageHeaderContainer from "./header/EditEntityPageHeaderContainer";
import FocusProvider from "./context/FocusProvider";
import FocusContext from "./context/FocusContext";

const EditEntityProvider = (props) => {
    const property = minBy(toArray(props.schema.properties), "propertyOrder");
    const initialTab = findKey(props.schema.properties, property);

    const tabs = sortBy(props.schema.properties, "propertyOrder").map((property) => {
        const key = findKey(props.schema.properties, property);
        return (
            <Tab eventKey={ key } key={ property.propertyOrder } title={ property.title } >
                <EditEntityFormContainer selectedTab={ key } />
            </Tab>
        );
    });

    return (
        <FocusProvider tab={ initialTab }>
            <EditEntityPageHeaderContainer />
            <FocusContext.Consumer>
                { ({ setTab, tab }) => (
                    <Tabs
                        activeKey={ tab }
                        animation={ false }
                        id="editEntityProviderTabs"
                        mountOnEnter
                        onSelect={ setTab }
                        unmountOnExit
                    >
                        { tabs }
                    </Tabs>
                )}
            </FocusContext.Consumer>
        </FocusProvider>
    );
};

EditEntityProvider.propTypes = {
    schema: PropTypes.shape({
        properties: PropTypes.objectOf(
            PropTypes.shape({
                propertyOrder: PropTypes.number.isRequired
            }).isRequired
        ).isRequired
    })
};

export default EditEntityProvider;
