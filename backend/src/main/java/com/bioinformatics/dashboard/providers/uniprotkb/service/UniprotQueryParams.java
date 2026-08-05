package com.bioinformatics.dashboard.providers.uniprotkb.service;

import org.springframework.util.LinkedMultiValueMap;

public record UniprotQueryParams(String format, String query, int pageSize, String dict, String sort, String cursor) {

    public LinkedMultiValueMap<String, String> toQueryParams() {
        var queryParams = new LinkedMultiValueMap<String, String>();
        queryParams.add("format", format);
        queryParams.add("query", query);
        if (pageSize > 0) {
            queryParams.add("size", String.valueOf(pageSize));
        }
        if (dict != null) {
            queryParams.add("dict", dict);
        }
        if (sort != null) {
            queryParams.add("sort", sort);
        }
        if (cursor != null) {
            queryParams.add("cursor", cursor);
        }
        return queryParams;
    }


    public static class Builder {
        private String format = "json";
        private String query;
        private int pageSize;
        private String dict;
        private String sort;
        private String cursor;

        public Builder withFormat(String format) {
            this.format = format;
            return this;
        }

        public Builder withQuery(String query) {
            this.query = query;
            return this;
        }

        public Builder withPageSize(int pageSize) {
            assert pageSize > 0 : "Page size must be greater than zero";
            assert pageSize <= 500 : "Page size must be less than or equal to 500";
            this.pageSize = pageSize;
            return this;
        }

        public Builder withDict(String dict) {
            this.dict = dict;
            return this;
        }

        public Builder withCursor(String cursor) {
            this.cursor = cursor;
            return this;
        }

        public Builder withSort(String field, String direction) {
            if (field != null && direction != null) {
                assert direction.equals("asc") || direction.equals("desc") : "Direction should be asc or desc";
                this.sort = "%s %s".formatted(field, direction);
            }
            return this;
        }

        public UniprotQueryParams build() {
            return new UniprotQueryParams(format, query, pageSize, dict, sort, cursor);
        }
    }


}
