import {GeneFilterFormValue, GeneFilterSnapshot} from '@core/models/saved-filter.model';

export const toSnapshot = (rawValue: GeneFilterFormValue): GeneFilterSnapshot => {
  return {
    ...rawValue,
    globalSearch: rawValue.globalSearch && rawValue.globalSearch !== '' ? rawValue.globalSearch : null,
    lengthMin: rawValue.length ? rawValue.length.min : null,
    lengthMax: rawValue.length ? rawValue.length.max : null,
    molecularWeightMin: rawValue.molecularWeight ? rawValue.molecularWeight.min : null,
    molecularWeightMax: rawValue.molecularWeight ? rawValue.molecularWeight.max : null,
    goAspect: rawValue.goAspect ?? null,
    goTermId: rawValue.goTermId && rawValue.goTermId !== '' ? rawValue.goTermId : null
  };
};

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
