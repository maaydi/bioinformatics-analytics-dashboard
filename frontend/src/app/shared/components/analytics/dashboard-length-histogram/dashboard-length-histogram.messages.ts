import {ANALYTICS_CHART_MESSAGES} from '../analytics-chart.messages';

export const DASHBOARD_LENGTH_HISTOGRAM_MESSAGES = {
  ...ANALYTICS_CHART_MESSAGES,
  title: 'Protein Length Distribution',
  hint: 'Protein count per 100 AA length bucket',
  empty: 'No histogram data available.',
  loadFailed: 'Unable to load protein length distribution.',
  chartLabel: 'Protein length distribution histogram',
  axisCaption: 'Sequence Length (AA)',
  bucketLabel: (min: number, max: number): string =>
    `Protein length bucket ${min} to ${max} amino acids`,
  range: (min: string | null, max: string | null): string => `Bar limit: ${min} - ${max} AA`,
  count: (value: string | null): string => `Count: ${value}`,
  percentage: (value: string | null): string => `Percentage: ${value}%`,
} as const;
