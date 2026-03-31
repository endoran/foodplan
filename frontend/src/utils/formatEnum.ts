export function formatEnum(value: string): string {
  return value
    .replace(/_/g, ' ')
    .split(' ')
    .map(w => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');
}
