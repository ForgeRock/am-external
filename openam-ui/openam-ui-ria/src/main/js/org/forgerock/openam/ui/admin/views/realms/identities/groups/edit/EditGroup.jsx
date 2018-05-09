/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button, Tab, Tabs } from "react-bootstrap";

import { clone, get, set } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import MembersTab from "./EditGroupMembers";
import PageHeader from "components/PageHeader";
import PrivilegesTab from "./EditGroupPrivileges";

class EditGroup extends Component {
    constructor (props) {
        super(props);

        this.handleMembersSave = this.handleMembersSave.bind(this);
        this.handlePrivilegesSave = this.handlePrivilegesSave.bind(this);
    }

    handleMembersSave (membersData) {
        const data = clone(this.props.values);

        set(data, "members", membersData);

        this.props.onSave(data);
    }

    handlePrivilegesSave (privilegesData) {
        const data = clone(this.props.values);

        set(data, "privileges", privilegesData);

        this.props.onSave(data);
    }

    render () {
        return (
            <div>
                <PageHeader icon="folder" title={ this.props.id } type={ t("console.identities.groups.edit.type") }>
                    <Button onClick={ this.props.onDelete }>
                        { t("common.form.delete") }
                    </Button>
                </PageHeader>
                <Tabs animation={ false } defaultActiveKey={ 1 }>
                    <Tab eventKey={ 1 } title={ t("console.identities.groups.edit.tabs.0") }>
                        <MembersTab
                            isFetching={ this.props.isFetching }
                            onAddAll={ this.props.onAddAll }
                            onRemoveAll={ this.props.onRemoveAll }
                            onSave={ this.handleMembersSave }
                            schema={ get(this.props.schema, "properties.members") }
                            values={ get(this.props.values, "members") }
                        />
                    </Tab>
                    <Tab eventKey={ 2 } title={ t("console.identities.groups.edit.tabs.1") }>
                        <PrivilegesTab
                            isFetching={ this.props.isFetching }
                            onSave={ this.handlePrivilegesSave }
                            schema={ get(this.props.schema, "properties.privileges") }
                            values={ get(this.props.values, "privileges") }
                        />
                    </Tab>
                </Tabs>
            </div>
        );
    }
}

EditGroup.propTypes = {
    id: PropTypes.string.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onAddAll: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
    onRemoveAll: PropTypes.func.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object),
    values: PropTypes.objectOf(PropTypes.object)
};

export default EditGroup;
