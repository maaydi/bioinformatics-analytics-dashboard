export const EXPORT_FILTER_REVIEW_MESSAGES = {
  stepLabel: 'Filter Review',
  title: 'Current filters',
  hint: 'These filters will be applied to the exported data.',
  filtersLabel: 'Current export filters (read-only)',
  noFilters: 'No filters applied. All data will be included.',
  estimatedRows: 'Estimated rows',
  noResults: 'No data matches the current filters. Adjust them to continue.',
  saveFilter: 'Save this filter',
  close: 'Close',
  retry: 'Retry',
  savedFilter: (name: string): string => `Saved filter "${name}"`,
  saveFailed: (name: string): string => `Failed to save filter "${name}"`,
} as const;
