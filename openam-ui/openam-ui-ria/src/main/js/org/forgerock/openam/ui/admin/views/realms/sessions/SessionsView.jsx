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
 * Copyright 2016-2017 ForgeRock AS.
 */

import _ from "lodash";
import { Panel, FormGroup, ControlLabel } from "react-bootstrap";
import { t } from "i18next";
import React, { Component } from "react";
import Select from "react-select";
import {
    getByRealmAndUsername,
    invalidateByHandles
} from "org/forgerock/openam/ui/admin/services/global/SessionsService";
import { getByUsernameStartsWith } from "org/forgerock/openam/ui/admin/services/realm/UsersService";
import CallToAction from "components/CallToAction";
import PageDescription from "components/PageDescription";
import SessionsTable from "./SessionsTable";
import PageHeader from "components/PageHeader";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const fetchUsersByPartialUsername = _.debounce((realm, username, callback) => {
    if (_.isEmpty(username)) {
        callback(null, { options: [] });
    } else {
        getByUsernameStartsWith(realm, username).then((response) => {
            callback(null, {
                options: _.map(response, (user) => ({ label: user, value: user }))
            });
        }, (error) => callback(error.statusText));
    }
}, 300);

class SessionsView extends Component {
    constructor (props) {
        super(props);

        this.handleSelectAsyncOnChange = this.handleSelectAsyncOnChange.bind(this);
        this.handleInvalidateSessions = this.handleInvalidateSessions.bind(this);
        this.fetchSessionsByUsernameAndRealm = this.fetchSessionsByUsernameAndRealm.bind(this);
        this.state = {
            sessions: []
        };
    }

    handleInvalidateSessions (realm, sessions) {
        const handles = _.pluck(sessions, "sessionHandle");

        invalidateByHandles(handles).then(() => this.fetchSessionsByUsernameAndRealm(realm, this.state.username));
    }

    fetchSessionsByUsernameAndRealm (realm, username) {
        getByRealmAndUsername(realm, username).then((response) => {
            this.setState({ sessions: response });
        });
    }

    handleSelectAsyncOnChange (realm, newValue) {
        const username = _.get(newValue, "value");
        this.setState({
            sessions: [],
            username
        });

        if (username) {
            this.fetchSessionsByUsernameAndRealm(realm, username);
        }
    }

    render () {
        let content;
        const realm = this.props.router.params[0];

        if (this.state.sessions.length) {
            content = (
                <SessionsTable
                    data={ this.state.sessions }
                    onSessionsInvalidate={ _.partial(this.handleInvalidateSessions, realm) }
                    username={ this.state.username }
                />
            );
        } else if (this.state.username) {
            content = <Panel><CallToAction><h3>{ t("console.sessions.table.noResults") }</h3></CallToAction></Panel>;
        }

        return (
            <div>
                <PageHeader title={ t("console.sessions.title") } />

                <PageDescription>{ t("console.sessions.search.intro") }</PageDescription>

                <FormGroup controlId="findAUser">
                    <ControlLabel srOnly>{ t("console.sessions.search.title") }</ControlLabel>
                    <Select.Async
                        autoload={ false }
                        inputProps={ {
                            id: "findAUser"
                        } }
                        isLoading
                        loadOptions={ _.partial(fetchUsersByPartialUsername, realm) }
                        noResultsText={ t("console.sessions.search.noResults") }
                        onChange={ _.partial(this.handleSelectAsyncOnChange, realm) }
                        placeholder={ t("console.sessions.search.placeholder") }
                        searchPromptText={ t("console.sessions.search.searchPrompt") }
                        value={ this.state.username }
                    />
                </FormGroup>
                { content }
            </div>
        );
    }
}

SessionsView.propTypes = {
    router: withRouterPropType
};

export default withRouter(SessionsView);
