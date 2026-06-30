import {describe, expect, it} from 'vitest';
import {getDefaultFormValue, isEqual, toForm, toSnapshot} from './gene-filter.utils';
import {GeneFilterFormValue, GeneFilterSnapshot} from '@core/models/saved-filter.model';

describe('gene-filter.utils', () => {
  describe('toSnapshot', () => {
    it('should convert form values to snapshot', () => {
      const rawValue: GeneFilterFormValue = {
        globalSearch: 'search',
        accession: 'P12345',
        entryName: '',
        geneNamePrimary: null,
        proteinFullName: undefined as any,
        reviewed: true,
        organism: 'Human',
        taxid: 9606,
        lineage: '',
        length: {min: 10, max: 20},
        molecularWeight: {min: null, max: 100},
        evidenceLevels: [1, 2],
        keywords: ['Membrane'],
        goTermId: 'GO:0000001',
        goAspect: 'C',
        featureType: '',
        crossRefSource: ''
      };

      const result = toSnapshot(rawValue);
      expect(result).toEqual({
        globalSearch: 'search',
        accession: 'P12345',
        entryName: null,
        geneNamePrimary: null,
        proteinFullName: null,
        reviewed: true,
        organism: 'Human',
        taxid: 9606,
        lineage: null,
        lengthMin: 10,
        lengthMax: 20,
        molecularWeightMin: null,
        molecularWeightMax: 100,
        evidenceLevels: [1, 2],
        keywords: ['Membrane'],
        goTermId: 'GO:0000001',
        goAspect: 'C',
        featureType: null,
        crossRefSource: null
      });
    });
  });

  describe('toForm', () => {
    it('should convert snapshot to form partial', () => {
      const snapshot: GeneFilterSnapshot = {
        globalSearch: null,
        accession: 'P12345',
        entryName: null,
        geneNamePrimary: null,
        proteinFullName: null,
        reviewed: false,
        organism: null,
        taxid: null,
        lineage: null,
        lengthMin: null,
        lengthMax: 50,
        molecularWeightMin: 10,
        molecularWeightMax: null,
        evidenceLevels: null,
        keywords: null,
        goTermId: null,
        goAspect: null,
        featureType: null,
        crossRefSource: null
      };

      const result = toForm(snapshot);
      expect(result).toEqual({
        globalSearch: '',
        accession: 'P12345',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: false,
        organism: '',
        taxid: null,
        lineage: '',
        length: {min: null, max: 50},
        molecularWeight: {min: 10, max: null},
        evidenceLevels: [],
        keywords: [],
        goTermId: '',
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      });
    });
  });

  describe('getDefaultFormValue', () => {
    it('should return default empty form value', () => {
      const result = getDefaultFormValue();
      expect(result).toEqual({
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
        crossRefSource: ''
      });
    });
  });

  describe('isEqual', () => {
    it('should return true for identical objects', () => {
      const obj1 = {a: 1, b: {c: [1, 2]}};
      const obj2 = {a: 1, b: {c: [1, 2]}};
      expect(isEqual(obj1, obj2)).toBe(true);
    });

    it('should return true for same primitive', () => {
      expect(isEqual(1, 1)).toBe(true);
      expect(isEqual('a', 'a')).toBe(true);
      expect(isEqual(null, null)).toBe(true);
    });

    it('should return false for different fields', () => {
      const obj1 = {a: 1};
      const obj2 = {a: 2};
      expect(isEqual(obj1, obj2)).toBe(false);
    });

    it('should return false for missing fields', () => {
      const obj1 = {a: 1};
      const obj2 = {a: 1, b: 2};
      expect(isEqual(obj1, obj2)).toBe(false);
    });

    it('should handle falsy values', () => {
      expect(isEqual(null, undefined)).toBe(false);
      expect(isEqual(0, false)).toBe(false);
    });
  });
});

