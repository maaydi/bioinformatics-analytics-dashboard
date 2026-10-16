import {ANALYTICS_CHART_MESSAGES} from '../analytics-chart.messages';

export const DASHBOARD_TOP_ORGANISMS_MESSAGES = {
  ...ANALYTICS_CHART_MESSAGES,
  title: (limit: number): string => `Top ${limit} Organisms by Protein Count`,
  chartLabel: (limit: number): string => `Top ${limit} organisms by protein count`,
  empty: 'No organism distribution data available.',
  loadFailed: 'Unable to load top organisms.',
} as const;
