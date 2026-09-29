package com.bioinformatics.exportservice.dto;

public enum DefaultExportFormat implements ExportFormat {
    CSV {
        @Override
        public String getFileExtension() {
            return "csv";
        }

        @Override
        public String getContentType() {
            return "text/csv; charset=UTF-8";
        }
    }, TSV {
        @Override
        public String getFileExtension() {
            return "tsv";
        }

        @Override
        public String getContentType() {
            return "text/tab-separated-values; charset=UTF-8";
        }

    }, JSON {
        @Override
        public String getFileExtension() {
            return "json";
        }

        @Override
        public String getContentType() {
            return "application/json";
        }

    }, EXCEL {
        @Override
        public String getFileExtension() {
            return "xlsx";
        }

        @Override
        public String getContentType() {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
    };


}
