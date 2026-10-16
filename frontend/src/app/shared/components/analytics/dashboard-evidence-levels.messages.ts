import {ANALYTICS_CHART_MESSAGES} from './analytics-chart.messages';

export const DASHBOARD_EVIDENCE_LEVELS_MESSAGES = {
  ...ANALYTICS_CHART_MESSAGES,
  title: 'Evidence Levels',
  hint: 'Distribution by UniProt evidence category',
  empty: 'No evidence level data available.',
  loadFailed: 'Unable to load evidence level distribution.',
  chartLabel: 'Evidence levels',
  filterLabel: (level: number, label: string): string => `Filter by evidence level ${level}, ${label}`,
  levelLabel: (level: number, label: string): string => `L${level} - ${label}`,
  percentage: (value: number): string => `(${value}%)`,
} as const;
