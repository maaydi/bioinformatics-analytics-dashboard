import {ANALYTICS_CHART_MESSAGES} from '../analytics-chart.messages';

export const DASHBOARD_KEYWORD_FREQUENCY_HISTOGRAM_MESSAGES = {
  ...ANALYTICS_CHART_MESSAGES,
  title: 'Keyword Frequency',
  hint: 'Top keywords by protein occurrence',
  empty: 'No keyword data available.',
  loadFailed: 'Unable to load keyword frequency.',
  chartLabel: 'Keyword frequency histogram',
  bucketLabel: (keyword: string, count: number): string => `Keyword ${keyword}, count ${count}`,
  keyword: (value: string): string => `Keyword: ${value}`,
  count: (value: string | null): string => `Count: ${value}`,
  share: (value: string | null): string => `Share: ${value}%`,
} as const;
