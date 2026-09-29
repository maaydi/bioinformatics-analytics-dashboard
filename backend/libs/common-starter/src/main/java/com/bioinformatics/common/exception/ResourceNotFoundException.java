package com.bioinformatics.common.exception;

/**
 * Thrown when a requested resource (protein entry, saved filter, import job) does not exist
 * or could not be found in the database.
 * Semantically different from {@link ResourceDeletedException} — this indicates the resource
 * has never existed or never existed in the requested scope.
 * Mapped to HTTP 404 Not Found by {@link GlobalExceptionHandler}.
 *
 * @see ResourceDeletedException
 * @see GlobalExceptionHandler#handleNotFound(ResourceNotFoundException)
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs an exception with the provided message.
     *
     * @param message description of the missing resource
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs an exception for a missing protein by its accession.
     *
     * @param accession the UniProt accession that was not found
     * @return a new ResourceNotFoundException instance
     */
    public static ResourceNotFoundException forProtein(String accession) {
        return new ResourceNotFoundException("Protein not found with accession: " + accession);
    }

    /**
     * Constructs an exception for a missing import job.
     *
     * @param jobId the import job ID that was not found
     * @return a new ResourceNotFoundException instance
     */
    public static ResourceNotFoundException forImportJob(String jobId) {
        return new ResourceNotFoundException("Import job not found: " + jobId);
    }

    /**
     * Constructs an exception for a missing saved filter.
     *
     * @param id the saved filter ID that was not found
     * @return a new ResourceNotFoundException instance
     */
    public static ResourceNotFoundException forSavedFilter(Long id) {
        return new ResourceNotFoundException("Saved filter not found with id: " + id);
    }
}
