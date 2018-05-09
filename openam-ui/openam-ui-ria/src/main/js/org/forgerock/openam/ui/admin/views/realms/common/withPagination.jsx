/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import React, { Component } from "react";
import { PropTypes } from "prop-types";

const withPagination = (WrappedComponent) => {
    return class withPagination extends Component {
        constructor (props) {
            super(props);
            this.state = {
                dataTotalSize: 0,
                page: 1,
                pagedResultsOffset: 0,
                sizePerPage: 10,
                sortDirection: "+",
                sortKey: ""
            };

            this.handlePageChange = this.handlePageChange.bind(this);
            this.handleSortChange = this.handleSortChange.bind(this);
            this.handleSizePerPageChange = this.handleSizePerPageChange.bind(this);
            this.handleDataChange = this.handleDataChange.bind(this);
            this.handleDataDelete = this.handleDataDelete.bind(this);
        }

        handleSizePerPageChange (sizePerPage) {
            this.setState({
                page: 1,
                pagedResultsOffset: 0,
                sizePerPage
            });
        }

        handlePageChange (page, sizePerPage) {
            this.setState({
                page,
                pagedResultsOffset: (page - 1) * sizePerPage
            });
        }

        handleSortChange (sortKey, direction) {
            this.setState({
                sortKey,
                sortDirection: direction === "asc" ? "+" : "-"
            });
        }

        handleDataChange ({ remainingPagedResults, result, totalPagedResults }) {
            // CREST 2.0 based resources use the convention of returning the remaining count of resources to the caller
            // {remainingPagedResults}. Whereas CREST 3.0 resources use the total resource count {totalPagedResults}.
            const isCrest2Pagination = remainingPagedResults !== -1;
            const isCrest3Pagination = totalPagedResults !== -1;
            let dataTotalSize = result.length;

            if (isCrest2Pagination) {
                dataTotalSize = remainingPagedResults + result.length + this.state.pagedResultsOffset;
            } else if (isCrest3Pagination) {
                dataTotalSize = totalPagedResults;
            }

            this.setState({ dataTotalSize });
        }

        handleDataDelete (numToRemove) {
            // This handler will change the dataTotalSize with will in turn trigger the reload of the data from
            // the endpoint.
            const { dataTotalSize, pagedResultsOffset, sizePerPage } = this.state;
            const numItemsFromOffset = dataTotalSize - pagedResultsOffset;
            const lastPage = numItemsFromOffset <= sizePerPage;
            const multiplePages = dataTotalSize / sizePerPage > 1;
            const removedAllItemsFromLastPage = numItemsFromOffset === numToRemove;

            if (multiplePages && lastPage && removedAllItemsFromLastPage) {
                this.handlePageChange(this.state.page - 1, sizePerPage);
            }

            this.setState({ dataTotalSize: dataTotalSize - numToRemove });
        }

        render () {
            return (
                <WrappedComponent
                    pagination={ {
                        hidePageListOnlyOnePage: true,
                        page: this.state.page,
                        sizePerPage: this.state.sizePerPage,
                        pagedResultsOffset: this.state.pagedResultsOffset,
                        sortDirection: this.state.sortDirection,
                        sortKey: this.state.sortKey,
                        dataTotalSize: this.state.dataTotalSize,
                        onDataDelete: this.handleDataDelete,
                        onDataChange: this.handleDataChange,
                        onPageChange: this.handlePageChange,
                        onSizePerPageList: this.handleSizePerPageChange,
                        onSortChange: this.handleSortChange,
                        pagination: true,
                        remote: true
                    } }
                    { ...this.props }
                />
            );
        }
    };
};

withPagination.displayName = "withPagination";

export const withPaginationPropTypes = PropTypes.shape({
    pagination: PropTypes.shape({
        page: PropTypes.number.isRequired,
        pageSize: PropTypes.number.isRequired,
        pagedResultsOffset: PropTypes.number.isRequired,
        sortDirection: PropTypes.string.isRequired,
        sortKey: PropTypes.string.isRequired,
        totalDataSize: PropTypes.number.isRequired,
        onDataChange: PropTypes.func.isRequired,
        onDataDelete: PropTypes.func.isRequired,
        onPageChange: PropTypes.func.isRequired,
        onSizePerPageList: PropTypes.func.isRequired,
        onSortChange: PropTypes.func.isRequired
    })
});

export default withPagination;
