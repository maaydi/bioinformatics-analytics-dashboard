export const AUTOCOMPLETE_MESSAGES = {
  placeholder: 'Search...',
  inputLabel: 'Search and select options',
  noMatches: 'No matches found',
  selected: (count: number): string => `${count} Selected`,
} as const;

export const RANGE_INPUT_MESSAGES = {
  min: 'Min',
  max: 'Max',
  minLabel: (label: string): string => `${label || 'Range'} minimum`,
  maxLabel: (label: string): string => `${label || 'Range'} maximum`,
  minTitle: (label: string): string => `${label} Minimum`,
  maxTitle: (label: string): string => `${label} Maximum`,
  invalid: 'Min cannot be greater than Max',
} as const;

export const INPUT_MESSAGES = {title: 'Your Title', placeholder: 'Type here...'} as const;

export const LOADING_MESSAGES = {label: 'Loading', content: 'Loading content'} as const;

export const EMPTY_STATE_MESSAGES = {message: 'No data found'} as const;

export const CHIP_LIST_MESSAGES = {label: 'Active search criteria', remove: 'Remove Filter'} as const;

export const LIMIT_SELECTOR_MESSAGES = {
  label: 'Number of items to display',
  invalid: (min: number, max: number): string => `Enter a value between ${min} and ${max}`,
} as const;

export const BREADCRUMB_MESSAGES = {label: 'breadcrumb'} as const;

export const COMMON_MESSAGES = {missingValue: '–', yes: 'Yes', no: 'No', ok: 'OK', close: 'Close', dismiss: 'Dismiss'} as const;
