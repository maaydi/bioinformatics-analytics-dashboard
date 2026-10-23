import {ExportFormat} from '@core/models/export-pipeline.model';

/**
 * Simulated average payload bytes per row, keyed by ProteinDetailDto's @ExportField names.
 * Text assumes mostly ASCII (one byte per character); arrays include all their values.
 * These are planning assumptions, not measured averages or maximum field lengths.
 * Keep this table aligned with the available-fields contract when export fields change.
 */
const FIELD_BYTE_ESTIMATES: Readonly<Partial<Record<string, number>>> = {
  accession: 10,
  entryName: 24,
  reviewed: 5,
  integratedDate: 10,
  sequenceDate: 10,
  updatedDate: 10,
  sequenceVersion: 5,
  entryVersion: 5,
  proteinFullName: 80,
  proteinShortName: 24,
  proteinEcNumber: 16,
  geneNamePrimary: 10,
  geneNameSynonyms: 40,
  geneOrfNames: 40,
  geneOrderedLocus: 40,
  organismName: 60,
  organismCommonName: 40,
  taxid: 10,
  lineage: 200,
  length: 6,
  molecularWeight: 10,
  sequenceChecksum: 16,
  sequence: 400,
  evidenceLevel: 32,
  metadataJsonb: 2048,
  keywords: 160,
  features: 256,
  goTerms: 256,
  crossReferences: 1024,
  hostOrganisms: 120,
  comments: 2048,
  publications: 256,
};

// Deliberately larger than every modeled field, but not a guarantee for unbounded data.
const UNKNOWN_FIELD_BYTES = 4096;

/**
 * Simulation multipliers for serialization overhead, not compression ratios:
 * CSV allows quoting/delimiters; TSV assumes less quoting; JSON allows repeated keys
 * and structure; Excel allows workbook/cell overhead without predicting ZIP compression.
 * Fixed file/header overhead is intentionally excluded, including for zero-row exports.
 */
const FORMAT_COEFFICIENTS: Readonly<Record<ExportFormat, number>> = {
  CSV: 1.5,
  TSV: 1.25,
  JSON: 2.5,
  EXCEL: 2,
};

/** Estimates locally without inspecting records or requesting additional data. */
export function estimateExportBytes(
  selectedFields: readonly string[],
  estimatedRows: number,
  format: ExportFormat,
): number {
  if (selectedFields.length === 0 || !Number.isFinite(estimatedRows) || estimatedRows <= 0) {
    return 0;
  }

  const bytesPerRow = [...new Set(selectedFields)].reduce((total, fieldName) => {
    const bytes = Object.hasOwn(FIELD_BYTE_ESTIMATES, fieldName)
      ? FIELD_BYTE_ESTIMATES[fieldName] ?? UNKNOWN_FIELD_BYTES
      : UNKNOWN_FIELD_BYTES;
    return total + bytes;
  }, 0);

  return bytesPerRow * estimatedRows * FORMAT_COEFFICIENTS[format];
}

/** Uses decimal units (1 KB = 1000 bytes), so 15000 bytes displays as 15 KB. */
export function formatExportBytes(bytes: number, locale = 'en-US'): string {
  const safeBytes = Number.isFinite(bytes) && bytes > 0 ? bytes : 0;
  const units = [
    {divisor: 1e12, label: 'TB'},
    {divisor: 1e9, label: 'GB'},
    {divisor: 1e6, label: 'MB'},
    {divisor: 1e3, label: 'KB'},
    {divisor: 1, label: 'B'},
  ] as const;
  const unit = units.find(({divisor}) => safeBytes >= divisor) ?? units[4];
  const value = new Intl.NumberFormat(locale, {maximumFractionDigits: 2}).format(safeBytes / unit.divisor);
  return `${value} ${unit.label}`;
}
