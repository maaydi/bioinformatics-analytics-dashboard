export const EXPORT_FIELD_SELECTION_MESSAGES = {
  modeLabel: 'Field selection mode',
  allFields: 'All Fields',
  customFields: 'Custom Fields',
  allFieldsHint: 'All available fields will be included in the export.',
  loadError: 'Available fields could not be loaded. Close the wizard and try again.',
  pickerLegend: 'Available export fields',
  selectAll: 'Select all',
  clearAll: 'Clear all',
  noFields: 'No fields available.',
  selectionCount: {
    '=0': 'No fields selected',
    '=1': '1 field selected',
    other: '# fields selected',
  },
} as const;
