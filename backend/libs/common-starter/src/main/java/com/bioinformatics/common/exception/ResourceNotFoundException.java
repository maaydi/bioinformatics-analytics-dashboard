package com.bioinformatics.common.exception;

/**
 * Thrown when a requested resource (protein entry, saved filter, import job) does not exist.
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException forProtein(String accession) {
        return new ResourceNotFoundException("Protein not found with accession: " + accession);
    }

    public static ResourceNotFoundException forImportJob(String jobId) {
        return new ResourceNotFoundException("Import job not found: " + jobId);
    }

    public static ResourceNotFoundException forSavedFilter(Long id) {
        return new ResourceNotFoundException("Saved filter not found with id: " + id);
    }
}
