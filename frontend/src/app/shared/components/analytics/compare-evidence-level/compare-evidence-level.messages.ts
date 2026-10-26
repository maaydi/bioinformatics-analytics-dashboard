export const COMPARE_EVIDENCE_LEVEL_MESSAGES = {
  title: 'Evidence Levels Comparison',
  primaryLabel: 'Base Filter',
  secondaryLabel: 'Compare Filter',
  empty: 'No comparison data available.',
  chartLabel: 'Evidence levels comparison',
  levelLabel: (level: number, label: string): string => `L${level} - ${label}`,
  countLabel: (label: string, count: number): string => `${label}: ${count}`,
} as const;
