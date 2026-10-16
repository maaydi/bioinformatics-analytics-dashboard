export const DASHBOARD_KPI_CARD_LIST_MESSAGES = {
  loading: 'Loading KPI cards...',
  empty: 'No KPI data available.',
  loadFailed: 'Unable to load dashboard KPIs.',
  retry: 'Retry',
  total: {title: 'Total', label: 'Total proteins'},
  reviewed: {title: 'Reviewed', label: 'Reviewed count'},
  organisms: {title: 'Organisms', label: 'Distinct organisms'},
  taxa: {title: 'Taxa', label: 'Distinct taxa'},
  averageLength: {title: 'Avg Len', label: 'Average Length', unit: 'AA'},
} as const;
