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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import { debounce, isEmpty, map, partial } from "lodash";
import { Panel, FormGroup, ControlLabel } from "react-bootstrap";
import { t } from "i18next";
import React, { Component } from "react";

import { getByRealmAndUsername, invalidateByHandles }
    from "org/forgerock/openam/ui/admin/services/global/SessionsService";
import { getByUsernameStartsWith } from "org/forgerock/openam/ui/admin/services/realm/identities/UsersService";
import AsyncCreatableSingleSelect from "components/inputs/select/AsyncCreatableSingleSelect";
import CallToAction from "components/CallToAction";
import PageDescription from "components/PageDescription";
import SessionsTable from "./SessionsTable";
import PageHeader from "components/PageHeader";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const fetchUsersByPartialUsername = debounce((realm, username, callback) => {
    if (isEmpty(username)) {
        callback([]);
    } else {
        getByUsernameStartsWith(realm, username).then((response) => {
            callback(map(response.result, ({ username }) => ({ label: username, value: username })));
        }, (error) => callback(error.statusText));
    }
}, 300);

class SessionsView extends Component {
    static propTypes = {
        router: withRouterPropType
    };

    state = {
        sessions: []
    };

    handleInvalidateSessions = (realm, sessions) => {
        const handles = map(sessions, "sessionHandle");
        invalidateByHandles(handles).then(() => this.fetchSessionsByUsernameAndRealm(realm, this.state.username));
    };

    fetchSessionsByUsernameAndRealm = (realm, username) => {
        getByRealmAndUsername(realm, username).then((response) => {
            this.setState({ sessions: response, username });
        });
    };

    handleInputChange = (username, { action }) => {
        if (action["input-change"] && username !== this.state.username) {
            this.setState({ username });
        }
    };

    handleOnChange = (data, { action }) => {
        if ((action === "create-option" || action === "select-option") && data.value) {
            const realm = this.props.router.params[0];
            this.fetchSessionsByUsernameAndRealm(realm, data.value);
        } else if (action === "clear") {
            this.setState({
                sessions: [],
                username: null
            });
        }
    };

    render () {
        let content;
        const realm = this.props.router.params[0];

        if (this.state.sessions.length) {
            content = (
                <SessionsTable
                    data={ this.state.sessions }
                    onSessionsInvalidate={ partial(this.handleInvalidateSessions, realm) }
                    username={ this.state.username }
                />
            );
        } else if (this.state.username) {
            content = (
                <Panel>
                    <Panel.Body>
                        <CallToAction><h3>{ t("console.sessions.table.noResults") }</h3></CallToAction>
                    </Panel.Body>
                </Panel>
            );
        }

        return (
            <div>
                <PageHeader title={ t("console.sessions.title") } />
                <PageDescription>{ t("console.sessions.search.intro") }</PageDescription>

                <FormGroup controlId="findAUser">
                    <ControlLabel srOnly>{ t("console.sessions.search.title") }</ControlLabel>
                    <AsyncCreatableSingleSelect
                        inputId="findAUser"
                        loadOptions={ partial(fetchUsersByPartialUsername, realm) }
                        onChange={ this.handleOnChange }
                        onInputChange={ this.handleInputChange }
                        placeholder={ t("console.sessions.search.placeholder") }
                    />
                </FormGroup>
                { content }
            </div>
        );
    }
}

export default withRouter(SessionsView);
