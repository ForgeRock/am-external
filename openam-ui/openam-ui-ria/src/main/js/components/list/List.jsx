/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { isEmpty, isEqual, omit, pluck } from "lodash";
import { TableHeaderColumn } from "react-bootstrap-table";
import PropTypes from "prop-types";
import React, { Component } from "react";

import ListToolbar from "./ListToolbar";
import ListCallToAction from "./ListCallToAction";
import Loading from "components/Loading";
import Table from "components/table/Table";

class List extends Component {
    constructor () {
        super();

        this.state = { selectedItems: [] };

        this.handleDelete = this.handleDelete.bind(this);
        this.handleSelectedChange = this.handleSelectedChange.bind(this);
    }

    componentWillReceiveProps (nextProps) {
        const listHasChanged = !isEqual(
            pluck(this.props.items, "_id").sort(),
            pluck(nextProps.items, "_id").sort()
        );

        if (listHasChanged) {
            // deselect everything in case of pagination, items deletion, etc.
            this.setState({ selectedItems: [] });
        }
    }

    handleDelete () {
        this.props.onDelete(this.state.selectedItems);
    }

    handleSelectedChange (items) {
        this.setState({ selectedItems: items });
    }

    render () {
        let content;
        const parentProps = omit(this.props, "children");

        if (this.props.isFetching) {
            content = <Loading />;
        } else if (isEmpty(this.props.items)) {
            content = (
                <ListCallToAction { ...parentProps } />
            );
        } else {
            const tableProperties = {
                ...parentProps,
                data: this.props.items,
                idField: "_id",
                onSelectedChange: this.handleSelectedChange,
                selectedItems: this.state.selectedItems
            };
            const toolbar = (
                <ListToolbar
                    { ...parentProps }
                    isDeleteDisabled={ !this.state.selectedItems.length }
                    numberSelected={ this.state.selectedItems.length }
                    onDelete={ this.handleDelete }
                />
            );
            content = (
                <div>
                    { toolbar }
                    <Table { ...tableProperties }>{ this.props.children }</Table>
                </div>
            );
        }

        return content;
    }
}

List.propTypes = {
    children: PropTypes.arrayOf(PropTypes.instanceOf(TableHeaderColumn)).isRequired,
    isFetching: PropTypes.bool.isRequired,
    items: PropTypes.arrayOf(PropTypes.object).isRequired,
    onDelete: PropTypes.func.isRequired
};

export default List;
