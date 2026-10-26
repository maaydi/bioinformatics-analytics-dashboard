import {ANALYTICS_CHART_MESSAGES} from '../analytics-chart.messages';

export const DASHBOARD_REVIEWED_RATIO_MESSAGES = {
  ...ANALYTICS_CHART_MESSAGES,
  title: 'Reviewed vs Unreviewed Proteins',
  empty: 'No reviewed ratio data available.',
  loadFailed: 'Unable to load reviewed ratio data.',
  reviewed: (count: string, percent: number): string => `Reviewed: ${count} (${percent}%)`,
  unreviewed: (count: string, percent: number): string => `Unreviewed: ${count} (${percent}%)`,
} as const;
