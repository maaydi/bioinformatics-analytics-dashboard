import {describe, expect, it} from 'vitest';
import {ExportFormat} from '@core/models/export-pipeline.model';
import {estimateExportBytes, formatExportBytes} from './export-size-estimator';

describe('estimateExportBytes', () => {
  it('estimates a 10-character primary gene name for 1000 CSV rows as exactly 15000 bytes / 15 KB', () => {
    const bytes = estimateExportBytes(['geneNamePrimary'], 1000, 'CSV');
    expect(bytes).toBe(15000);
    expect(formatExportBytes(bytes)).toBe('15 KB');
  });

  // Contract keys from ProteinDetailDto, including collections and long payload fields.
  it.each([
    ['accession', 10], ['entryName', 24], ['reviewed', 5],
    ['integratedDate', 10], ['sequenceDate', 10], ['updatedDate', 10],
    ['sequenceVersion', 5], ['entryVersion', 5],
    ['proteinFullName', 80], ['proteinShortName', 24], ['proteinEcNumber', 16],
    ['geneNamePrimary', 10], ['geneNameSynonyms', 40], ['geneOrfNames', 40], ['geneOrderedLocus', 40],
    ['organismName', 60], ['organismCommonName', 40], ['taxid', 10], ['lineage', 200],
    ['length', 6], ['molecularWeight', 10], ['sequenceChecksum', 16], ['sequence', 400],
    ['evidenceLevel', 32], ['metadataJsonb', 2048], ['keywords', 160], ['features', 256],
    ['goTerms', 256], ['crossReferences', 1024], ['hostOrganisms', 120], ['comments', 2048], ['publications', 256],
  ] as const)('models %s as %i bytes per row before format overhead', (fieldName, bytes) => {
    expect(estimateExportBytes([fieldName], 1, 'CSV')).toBe(bytes * 1.5);
  });

  const formats: ReadonlyArray<{ readonly format: ExportFormat; readonly coefficient: number }> = [
    {format: 'CSV', coefficient: 1.5},
    {format: 'TSV', coefficient: 1.25},
    {format: 'JSON', coefficient: 2.5},
    {format: 'EXCEL', coefficient: 2},
  ];

  it.each(formats)('sums selected field sizes before applying the $format coefficient', ({format, coefficient}) => {
    const selected = Object.freeze(['geneNamePrimary', 'accession', 'length']);
    expect(estimateExportBytes(selected, 1000, format)).toBe(26 * 1000 * coefficient);
  });

  it.each(formats)('returns zero for empty fields or zero rows in $format', ({format}) => {
    expect(estimateExportBytes([], 1000, format)).toBe(0);
    expect(estimateExportBytes(['sequence'], 0, format)).toBe(0);
  });

  it.each([-1, Number.NaN, Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITY])(
    'returns zero for an unusable row count (%s)', (rows) => {
      expect(estimateExportBytes(['geneNamePrimary'], rows, 'CSV')).toBe(0);
    },
  );

  it.each(['newBackendField', 'geneName', 'toString', '__proto__'])('uses a conservative 4096-byte fallback for %s', (fieldName) => {
    expect(estimateExportBytes([fieldName, 'geneNamePrimary'], 1000, 'CSV')).toBe((4096 + 10) * 1000 * 1.5);
  });

  it('counts each selected field only once without mutating the selection', () => {
    const selected = Object.freeze(['geneNamePrimary', 'geneNamePrimary']);
    expect(estimateExportBytes(selected, 1000, 'CSV')).toBe(15000);
    expect(selected).toEqual(['geneNamePrimary', 'geneNamePrimary']);
  });
});

describe('formatExportBytes', () => {
  it.each([
    [0, '0 B'], [999, '999 B'], [1000, '1 KB'], [15000, '15 KB'],
    [12500, '12.5 KB'], [1234567, '1.23 MB'], [1e9, '1 GB'], [1e12, '1 TB'],
  ] as const)('formats %s bytes in decimal units as %s', (bytes, formatted) => {
    expect(formatExportBytes(bytes)).toBe(formatted);
  });

  it.each([-1, Number.NaN, Number.POSITIVE_INFINITY])('formats invalid or negative bytes (%s) safely', (bytes) => {
    expect(formatExportBytes(bytes)).toBe('0 B');
  });

  it('uses the supplied locale for decimal formatting', () => {
    expect(formatExportBytes(12500, 'de-DE')).toBe('12,5 KB');
  });
});
