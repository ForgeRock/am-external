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

import { arrayMove, SortableContainer, SortableElement } from "react-sortable-hoc";
import { isEmpty, without } from "lodash";
import { ListGroup, Well } from "react-bootstrap";
import PropTypes from "prop-types";
import React, { Component } from "react";

import AddInput from "./AddInput";
import ListItem from "./ListItem";

const SortableItem = SortableElement((props) => <ListItem { ...props } />);
const SortableList = SortableContainer(({ isSorting, items, onDelete }) => {
    const isEnabled = items.length > 1;
    const sortableItems = items.map((value, index) => (
        <SortableItem
            disabled={ !isEnabled }
            index={ index }
            isActiveAlias={ index === 0 }
            isCursorActive={ isEnabled }
            isListGroupSorting={ isSorting }
            key={ value }
            onDelete={ onDelete }
            value={ value }
        />
    ));
    return (
        <ListGroup className="custom-aliases-field-list-group">
            { sortableItems }
        </ListGroup>
    );
});

class CustomAliasesField extends Component {
    constructor (props) {
        super(props);

        this.state = { isSorting: false };

        this.handleAdd = this.handleAdd.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
        this.handleSortEnd = this.handleSortEnd.bind(this);
        this.handleSortStart = this.handleSortStart.bind(this);
    }

    handleAdd (value) {
        // minLength
        if (value.length < this.props.schema.items.minLength) {
            return false;
        }

        // uniqueItems
        if (this.props.schema.uniqueItems &&
            this.props.formData.includes(value)) {
            return false;
        }

        this.props.onChange([...this.props.formData, value]);

        return true;
    }

    handleDelete (value) {
        const formData = without(this.props.formData, value);
        this.props.onChange(formData);
    }

    handleSortEnd ({ oldIndex, newIndex }) {
        const reorderedData = arrayMove(this.props.formData, oldIndex, newIndex);
        this.props.onChange(reorderedData);
        this.setState({ isSorting: false });
    }

    handleSortStart () {
        this.setState({ isSorting: true });
    }

    render () {
        const list = isEmpty(this.props.formData)
            ? null
            : (
                <Well bsSize="small">
                    <SortableList
                        helperClass="dragged"
                        isSorting={ this.state.isSorting }
                        items={ this.props.formData }
                        lockAxis="y"
                        lockToContainerEdges
                        onDelete={ this.handleDelete }
                        onSortEnd={ this.handleSortEnd }
                        onSortStart={ this.handleSortStart }
                        useDragHandle
                    />
                </Well>
            );

        return (
            <div className="custom-aliases-field">
                { list }
                <AddInput
                    id={ this.props.idSchema && this.props.idSchema.$id }
                    onAdd={ this.handleAdd }
                />
            </div>
        );
    }
}

CustomAliasesField.defaultProps = {
    formData: []
};

CustomAliasesField.propTypes = {
    formData: PropTypes.arrayOf(PropTypes.any),
    idSchema: PropTypes.objectOf(PropTypes.any),
    onChange: PropTypes.func.isRequired,
    schema: PropTypes.shape({
        items: PropTypes.shape({
            minLength: PropTypes.number.isRequired
        }).isRequired,
        minItems: PropTypes.number.isRequired,
        uniqueItems: PropTypes.bool.isRequired
    }).isRequired
};

export default CustomAliasesField;
