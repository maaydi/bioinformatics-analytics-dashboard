import {describe, expect, it} from 'vitest';
import {formatDate} from './date-formatter';

describe('date-formatter', () => {
  it('should format valid ISO string properly', () => {
    const result = formatDate('2023-05-15T10:00:00Z');
    expect(result).toBe('May 15, 2023');
  });

  it('should return a dash for null', () => {
    expect(formatDate(null)).toBe('–');
  });

  it('should return a dash for undefined', () => {
    expect(formatDate(undefined)).toBe('–');
  });

  it('should return a dash for empty string', () => {
    expect(formatDate('')).toBe('–');
  });
});

