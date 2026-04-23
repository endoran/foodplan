export function parseFraction(input: string): number {
  const s = input.trim();
  if (!s) return NaN;

  // Plain number (integer or decimal)
  if (/^-?\d+(\.\d+)?$/.test(s)) return parseFloat(s);

  // Simple fraction: "1/2", "3/4"
  const fractionMatch = s.match(/^(\d+)\s*\/\s*(\d+)$/);
  if (fractionMatch) {
    const num = parseInt(fractionMatch[1]);
    const den = parseInt(fractionMatch[2]);
    return den === 0 ? NaN : num / den;
  }

  // Mixed fraction: "1 1/2", "2 3/4"
  const mixedMatch = s.match(/^(\d+)\s+(\d+)\s*\/\s*(\d+)$/);
  if (mixedMatch) {
    const whole = parseInt(mixedMatch[1]);
    const num = parseInt(mixedMatch[2]);
    const den = parseInt(mixedMatch[3]);
    return den === 0 ? NaN : whole + num / den;
  }

  // Unicode fractions
  const unicodeFractions: Record<string, number> = {
    "\u00BC": 0.25, "\u00BD": 0.5, "\u00BE": 0.75,
    "\u2153": 1/3, "\u2154": 2/3, "\u2155": 0.2, "\u2156": 0.4,
    "\u2157": 0.6, "\u2158": 0.8, "\u2159": 1/6, "\u215A": 5/6,
    "\u215B": 0.125, "\u215C": 0.375, "\u215D": 0.625, "\u215E": 0.875,
  };

  // "1½" or just "½"
  for (const [char, val] of Object.entries(unicodeFractions)) {
    if (s.endsWith(char)) {
      const prefix = s.slice(0, -1).trim();
      if (!prefix) return val;
      const whole = parseInt(prefix);
      return isNaN(whole) ? NaN : whole + val;
    }
  }

  return NaN;
}
