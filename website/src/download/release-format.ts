/** How the site writes a release's files and date, the same for both apps. */

/** One file of a release, as GitHub lists it. */
export interface ReleaseFile {
  url: string;
  bytes: number;
}

// Sized the way the GitHub release page the buttons link to sizes it: binary megabytes to three significant digits,
// so 81,700,586 bytes read 77.9 MB in both places rather than 82 MB here.
export function megabytes(file: ReleaseFile): string {
  return `${Number((file.bytes / 1024 ** 2).toPrecision(3))} MB`;
}

export function releaseDate(date: Date): string {
  return date.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric", timeZone: "UTC" });
}
