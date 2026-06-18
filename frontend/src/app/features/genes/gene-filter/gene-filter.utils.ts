import {GeneFilterFormValue, GeneFilterSnapshot} from '@core/models/saved-filter.model';

/**
 * Converts form raw values to an immutable filter snapshot for API requests.
 * Strips empty strings and null values; preserves min/max ranges.
 *
 * @param rawValue submitted form data
 * @returns normalized filter snapshot
 */
export const toSnapshot = (rawValue: GeneFilterFormValue): GeneFilterSnapshot => {
  return {
    accession: rawValue.accession || null,
    entryName: rawValue.entryName || null,
    geneNamePrimary: rawValue.geneNamePrimary || null,
    proteinFullName: rawValue.proteinFullName || null,
    reviewed: rawValue.reviewed ?? null,
    organism: rawValue.organism || null,
    taxid: rawValue.taxid ?? null,
    lineage: rawValue.lineage || null,
    evidenceLevels: rawValue.evidenceLevels ?? null,
    keywords: rawValue.keywords ?? null,
    featureType: rawValue.featureType || null,
    crossRefSource: rawValue.crossRefSource || null,

    globalSearch: rawValue.globalSearch && rawValue.globalSearch !== '' ? rawValue.globalSearch : null,
    lengthMin: rawValue.length ? rawValue.length.min : null,
    lengthMax: rawValue.length ? rawValue.length.max : null,
    molecularWeightMin: rawValue.molecularWeight ? rawValue.molecularWeight.min : null,
    molecularWeightMax: rawValue.molecularWeight ? rawValue.molecularWeight.max : null,
    goAspect: rawValue.goAspect ?? null,
    goTermId: rawValue.goTermId && rawValue.goTermId !== '' ? rawValue.goTermId : null
  };
};

/**
 * Converts a filter snapshot to form-compatible partial values.
 * Restores null/sparse fields for form binding and UI state.
 *
 * @param snapshot immutable filter snapshot
 * @returns partial form values
 */
export const toForm = (snapshot: GeneFilterSnapshot): Partial<GeneFilterFormValue> => {
  return {
    globalSearch: snapshot.globalSearch ?? '',
    accession: snapshot.accession ?? '',
    entryName: snapshot.entryName ?? '',
    geneNamePrimary: snapshot.geneNamePrimary ?? '',
    proteinFullName: snapshot.proteinFullName ?? '',
    reviewed: snapshot.reviewed ?? null,
    organism: snapshot.organism ?? '',
    taxid: snapshot.taxid ?? null,
    lineage: snapshot.lineage ?? '',
    length: {min: snapshot.lengthMin ?? null, max: snapshot.lengthMax ?? null},
    molecularWeight: {min: snapshot.molecularWeightMin ?? null, max: snapshot.molecularWeightMax ?? null},
    evidenceLevels: snapshot.evidenceLevels ?? [],
    keywords: snapshot.keywords ?? [],
    goTermId: snapshot.goTermId ?? '',
    goAspect: snapshot.goAspect ?? null,
    featureType: snapshot.featureType ?? '',
    crossRefSource: snapshot.crossRefSource ?? '',
  };
};

/**
 * Returns the default/reset form state with all fields cleared.
 *
 * @returns fresh form value object
 */
export const getDefaultFormValue = (): GeneFilterFormValue => {
  return {
    globalSearch: '',
    accession: '',
    entryName: '',
    geneNamePrimary: '',
    proteinFullName: '',
    reviewed: null,
    organism: '',
    taxid: null,
    lineage: '',
    length: {min: null, max: null},
    molecularWeight: {min: null, max: null},
    evidenceLevels: [],
    keywords: [],
    goTermId: '',
    goAspect: null,
    featureType: '',
    crossRefSource: '',
  };
};

/**
 * Deep equality check for two objects; handles arrays, objects, and primitives.
 * Used to detect if two filter snapshots are identical (UI warning).
 *
 * @param obj1 first object
 * @param obj2 second object
 * @returns true if objects are deeply equal
 */
export const isEqual = (obj1: any, obj2: any): boolean => {
  if (obj1 === obj2) return true;

  if (typeof obj1 !== 'object' || obj1 === null ||
    typeof obj2 !== 'object' || obj2 === null) return false;

  const keys1 = Object.keys(obj1);
  const keys2 = Object.keys(obj2);

  if (keys1.length !== keys2.length) return false;

  for (const key of keys1) {
    if (!keys2.includes(key) || !isEqual(obj1[key], obj2[key])) return false;
  }

  return true;
};
