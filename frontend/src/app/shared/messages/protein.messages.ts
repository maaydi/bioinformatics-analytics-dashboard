import type {EvidenceLevel} from '@core/models/protein.model';

export const EVIDENCE_LEVEL_MESSAGES = {
  1: 'Protein level',
  2: 'Transcript level',
  3: 'Homology',
  4: 'Predicted',
  5: 'Uncertain',
} as const satisfies Readonly<Record<EvidenceLevel, string>>;
