import {describe, expect, it} from 'vitest';
import {buildFiltersChips} from './filter-chips-builder';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';

describe('filter-chips-builder', () => {
  it('should return empty array when filters is null', () => {
    expect(buildFiltersChips(null)).toEqual([]);
  });

  it('should return chips for populated fields', () => {
    const filters: GeneFilterSnapshot = {
      globalSearch: 'kinase',
      accession: 'P12345',
      entryName: null,
      geneNamePrimary: undefined,
      proteinFullName: '',
      reviewed: true,
      organism: 'Human',
      taxid: null,
      lineage: null,
      lengthMin: 10,
      lengthMax: 100,
      molecularWeightMin: null,
      molecularWeightMax: null,
      evidenceLevels: [1, 2],
      keywords: [],
      goTermId: null,
      goAspect: null,
      featureType: null,
      crossRefSource: null
    };

    const chips = buildFiltersChips(filters);

    expect(chips).toEqual([
      {key: 'globalSearch', label: 'Search', value: 'kinase'},
      {key: 'accession', label: 'Accession', value: 'P12345'},
      {key: 'reviewed', label: 'Reviewed', value: 'Yes'},
      {key: 'organism', label: 'Organism', value: 'Human'},
      {key: 'lengthMin', label: 'Length Min', value: '10'},
      {key: 'lengthMax', label: 'Length Max', value: '100'},
      {key: 'evidenceLevels', label: 'Evidence', value: '1, 2'}
    ]);
  });

  it('should return No for reviewed: false', () => {
    const filters: Partial<GeneFilterSnapshot> = {
      reviewed: false
    };
    const chips = buildFiltersChips(filters as GeneFilterSnapshot);
    expect(chips).toEqual([
      {key: 'reviewed', label: 'Reviewed', value: 'No'},
    ]);
  });
});

