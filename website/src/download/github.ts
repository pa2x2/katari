/** Reads GitHub's release API while the site builds, signed in when the workflow provides a token. */
export interface GitHubAsset {
  name: string;
  size: number;
  browser_download_url: string;
}

export interface GitHubRelease {
  tag_name: string;
  html_url: string;
  published_at: string;
  draft: boolean;
  prerelease: boolean;
  assets: GitHubAsset[];
}

/** The body of a GitHub API or download address; a stalled answer fails instead of holding the whole build. */
export async function fetchFromGitHub(url: string): Promise<string> {
  const token = process.env.GITHUB_TOKEN;
  const api = url.startsWith("https://api.github.com/");
  const response = await fetch(url, {
    headers: api ? { accept: "application/vnd.github+json", ...(token ? { authorization: `Bearer ${token}` } : {}) } : {},
    signal: AbortSignal.timeout(15_000),
  });
  if (!response.ok) throw new Error(`GitHub answered ${response.status} for ${url}`);
  return response.text();
}
