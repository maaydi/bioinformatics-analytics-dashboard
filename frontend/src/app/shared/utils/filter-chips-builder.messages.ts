import type {GeneSearchRequest} from '@core/models/saved-filter.model';
import {COMMON_MESSAGES} from '@shared/messages/ui.messages';

export const FILTER_CHIP_MESSAGES = {
  yes: COMMON_MESSAGES.yes,
  no: COMMON_MESSAGES.no,
  labels: {
    globalSearch: 'Search', accession: 'Accession', entryName: 'Entry', geneNamePrimary: 'Gene ',
    proteinFullName: 'Protein', reviewed: 'Reviewed', organism: 'Organism', taxid: 'TaxID',
    lineage: 'Lineage', lengthMin: 'Length Min', lengthMax: 'Length Max',
    molecularWeightMin: 'Weight Min', molecularWeightMax: 'Weight Max',
    evidenceLevels: 'Evidence', keywords: 'Keywords', goTermId: 'Go ID', goAspect: 'Go Aspect',
    featureType: 'Feature', crossRefSource: 'CrossRef',
  } satisfies Readonly<Record<keyof GeneSearchRequest, string>>,
} as const;
