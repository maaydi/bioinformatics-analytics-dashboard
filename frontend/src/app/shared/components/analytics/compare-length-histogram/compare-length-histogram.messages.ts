import {ANALYTICS_CHART_MESSAGES} from '../analytics-chart.messages';

export const COMPARE_LENGTH_HISTOGRAM_MESSAGES = {
  ...ANALYTICS_CHART_MESSAGES,
  title: 'Protein Length Distribution',
  labelA: 'Set A',
  labelB: 'Set B',
  empty: 'No histogram data available.',
  chartLabel: 'Compare length distribution histogram',
  axisCaption: 'Sequence Length (AA)',
  bucketLabel: (min: number, max: number): string => `Length bucket ${min} to ${max}`,
  range: (min: string | null, max: string | null): string => `${min} - ${max} AA`,
  series: (label: string, count: string | null, share: string | null): string =>
    `${label}: ${count} (${share}%)`,
} as const;
