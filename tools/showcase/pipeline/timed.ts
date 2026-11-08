/** Runs [step], reporting how long it took, so slow steps stand out in a run's output. */
export async function timed<T>(label: string, step: () => T | Promise<T>): Promise<T> {
  console.error(label);
  const start = Date.now();
  try {
    return await step();
  } finally {
    console.error(`  ${label}: ${Math.round((Date.now() - start) / 1000)} s`);
  }
}
