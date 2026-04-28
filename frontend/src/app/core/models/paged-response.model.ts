/**
 * Pagination envelope returned by all paginated list endpoints.
 *
 * Schema: documentation/api-contract.md — Shared Schemas — PagedResponse<T>
 */
export interface PagedResponse<T> {
  content:       T[];
  page:          number;
  size:          number;
  totalElements: number;
  totalPages:    number;
}

/**
 * Standard error envelope returned by the API for all error responses.
 *
 * Schema: documentation/api-contract.md — Shared Schemas — ErrorResponse
 */
export interface ApiError {
  status:    number;
  error:     string;
  message:   string;
  timestamp: string;
}
