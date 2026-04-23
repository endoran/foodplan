import { describe, test, expect } from 'vitest';
import { parseFraction } from './parseFraction';

describe('parseFraction', () => {
  test('plain integers', () => {
    expect(parseFraction('3')).toBe(3);
    expect(parseFraction('0')).toBe(0);
    expect(parseFraction('42')).toBe(42);
  });

  test('decimals', () => {
    expect(parseFraction('2.5')).toBe(2.5);
    expect(parseFraction('0.75')).toBe(0.75);
  });

  test('simple fractions', () => {
    expect(parseFraction('1/2')).toBe(0.5);
    expect(parseFraction('3/4')).toBe(0.75);
    expect(parseFraction('1/3')).toBeCloseTo(0.333, 2);
  });

  test('mixed fractions', () => {
    expect(parseFraction('1 1/2')).toBe(1.5);
    expect(parseFraction('2 3/4')).toBe(2.75);
    expect(parseFraction('10 1/3')).toBeCloseTo(10.333, 2);
  });

  test('unicode fractions', () => {
    expect(parseFraction('\u00BD')).toBe(0.5);
    expect(parseFraction('\u00BC')).toBe(0.25);
    expect(parseFraction('\u00BE')).toBe(0.75);
    expect(parseFraction('1\u00BD')).toBe(1.5);
    expect(parseFraction('2\u00BC')).toBe(2.25);
  });

  test('whitespace handling', () => {
    expect(parseFraction('  1/2  ')).toBe(0.5);
    expect(parseFraction(' 3 ')).toBe(3);
    expect(parseFraction('1  1/2')).toBe(1.5);
  });

  test('rejects negative numbers', () => {
    expect(parseFraction('-1')).toBeNaN();
    expect(parseFraction('-2.5')).toBeNaN();
  });

  test('rejects invalid input', () => {
    expect(parseFraction('')).toBeNaN();
    expect(parseFraction('abc')).toBeNaN();
    expect(parseFraction('1/0')).toBeNaN();
    expect(parseFraction('0/0')).toBeNaN();
    expect(parseFraction('hello world')).toBeNaN();
  });
});
