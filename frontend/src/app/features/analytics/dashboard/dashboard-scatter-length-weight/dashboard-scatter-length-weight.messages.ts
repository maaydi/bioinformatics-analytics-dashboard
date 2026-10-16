import {ANALYTICS_CHART_MESSAGES} from '@shared/components/analytics/analytics-chart.messages';

export const DASHBOARD_SCATTER_LENGTH_WEIGHT_MESSAGES = {
  ...ANALYTICS_CHART_MESSAGES,
  title: 'Molecular Weight vs. Sequence Length',
  hint: 'Click a point for detailed analysis in Gene Explorer',
  loading: 'Loading scatter plot...',
  empty: 'No molecular data available for plotting.',
  loadFailed: 'Unable to load chart data.',
  high: 'High',
  low: 'Low',
  lengthAxis: 'Length',
  weightAxis: 'Molecular Weight (Da)',
  tooltipTitle: 'Molecule Stat',
  lengthLabel: 'Length:',
  weightLabel: 'Weight:',
  countLabel: 'Count:',
  weight: (value: number): string => `${value} Da`,
  proteins: (count: number): string => `${count} protein${count > 1 ? 's' : ''}`,
} as const;
