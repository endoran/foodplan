type CleanupFn = () => Promise<void>;

const registry: CleanupFn[] = [];

export function registerCleanup(fn: CleanupFn): void {
  registry.push(fn);
}

export async function runCleanup(): Promise<void> {
  while (registry.length > 0) {
    const fn = registry.pop()!;
    try {
      await fn();
    } catch {
      // swallow — entity may already be deleted by the test
    }
  }
}
