import {GeneFilterSnapshot} from '@core/models/saved-filter.model';

export type FilterChip = { key: keyof GeneFilterSnapshot, label: string, value: string };

/** Converts non-empty filter fields into display chips. */
export const buildFiltersChips = (filters: GeneFilterSnapshot | null): FilterChip[] => {
  if (!filters) {
    return [];
  }
  const config: Array<{ key: keyof GeneFilterSnapshot; label: string }> = [
    {key: 'globalSearch', label: 'Search'},
    {key: 'accession', label: 'Accession'},
    {key: 'entryName', label: 'Entry'},
    {key: 'geneNamePrimary', label: 'Gene '},
    {key: 'proteinFullName', label: 'Protein'},
    {key: 'reviewed', label: 'Reviewed'},
    {key: 'organism', label: 'Organism'},
    {key: 'taxid', label: 'TaxID'},
    {key: 'lineage', label: 'Lineage'},
    {key: 'lengthMin', label: 'Length Min'},
    {key: 'lengthMax', label: 'Length Max'},
    {key: 'molecularWeightMin', label: 'Weight Min'},
    {key: 'molecularWeightMax', label: 'Weight Max'},
    {key: 'evidenceLevels', label: 'Evidence'},
    {key: 'keywords', label: 'Keywords'},
    {key: 'goTermId', label: 'Go ID'},
    {key: 'goAspect', label: 'Go Aspect'},
    {key: 'featureType', label: 'Feature'},
    {key: 'crossRefSource', label: 'CrossRef'},

  ];
  const chips: FilterChip[] = [];
  for (const item of config) {
    const rawValue = filters[item.key];
    if (rawValue === null || rawValue === undefined || rawValue === '') {
      continue;
    }
    if (Array.isArray(rawValue)) {
      if (rawValue.length === 0) {
        continue;
      }
      chips.push({key: item.key, label: item.label, value: rawValue.join(', ')});
      continue;
    }
    if (item.key === 'reviewed') {
      chips.push({key: item.key, label: item.label, value: rawValue ? 'Yes' : 'No'});
      continue;
    }
    chips.push({key: item.key, label: item.label, value: String(rawValue)});
  }
  return chips;
};
