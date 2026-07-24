import { defineConfig } from 'vitepress'
import type { DefaultTheme } from 'vitepress'

const base = '/katari/'
const repositoryUrl = 'https://github.com/katariapp/katari'

async function getLatestReleaseTag(): Promise<string | undefined> {
  const headers: Record<string, string> = {
    Accept: 'application/vnd.github+json',
    'X-GitHub-Api-Version': '2022-11-28',
  }
  const githubToken = process.env.GITHUB_TOKEN
  if (githubToken) headers.Authorization = `Bearer ${githubToken}`

  try {
    const response = await fetch(`${repositoryUrl.replace('https://github.com/', 'https://api.github.com/repos/')}/releases/latest`, { headers })
    if (!response.ok) return undefined

    const release = await response.json() as { tag_name?: unknown }
    return typeof release.tag_name === 'string' ? release.tag_name : undefined
  } catch {
    return undefined
  }
}

const latestReleaseTag = await getLatestReleaseTag()

const sidebar: DefaultTheme.SidebarItem[] = [
  {
    text: 'Get started',
    items: [
      { text: 'About project', link: '/about-project' },
      { text: 'Migrating to Katari', link: '/migrating-to-katari' },
      { text: 'Backup and restore', link: '/differences/backup-and-restore' },
      { text: 'Inherited Mihon features', link: '/inherited-features' },
    ],
  },
  {
    text: 'Features',
    items: [
      { text: 'Content type support', link: '/features/content-type-reference' },
      { text: 'Unified library', link: '/features/unified-library' },
      { text: 'Profiles', link: '/features/profiles' },
      { text: 'Merged entries', link: '/features/merged-entries' },
      { text: 'Feeds and discovery', link: '/features/feeds-and-discovery' },
      { text: 'Book reading', link: '/features/book-reading' },
      { text: 'Video playback', link: '/features/video-playback' },
      { text: 'Extensions and compatibility', link: '/differences/extensions-and-compatibility' },
      { text: 'Builds, telemetry, and privacy', link: '/differences/builds-telemetry-and-privacy' },
    ],
  },
  {
    text: 'Developers',
    items: [
      { text: 'Developer documentation', link: '/developers/' },
      {
        text: 'Extension development',
        collapsed: false,
        items: [
          { text: 'Overview', link: '/extensions/' },
          { text: 'Getting started', link: '/extensions/getting-started' },
          { text: 'HTTP and parsing', link: '/extensions/http-and-parsing' },
          {
            text: 'Media cookbooks',
            collapsed: true,
            items: [
              { text: 'Image media', link: '/extensions/image-media' },
              { text: 'Playback media', link: '/extensions/playback-media' },
              { text: 'Book media', link: '/extensions/book-media' },
            ],
          },
          { text: 'Migrating from Mihon', link: '/extensions/migrating-from-mihon' },
          { text: 'Publishing and maintenance', link: '/extensions/publishing' },
        ],
      },
      {
        text: 'Application architecture',
        collapsed: false,
        items: [
          { text: 'Entry Features', link: '/developers/feature-architecture' },
        ],
      },
      {
        text: 'Entry SDK',
        collapsed: false,
        items: [
          { text: 'Overview', link: '/developers/sdk/' },
          { text: 'Data model', link: '/developers/sdk/data-model' },
          { text: 'Content types', link: '/developers/sdk/content-types' },
          { text: 'Viewer settings', link: '/developers/sdk/viewer-settings' },
          { text: 'Book API architecture', link: '/developers/sdk/book-api' },
          { text: 'Capabilities', link: '/developers/sdk/capabilities' },
          { text: 'Compatibility and versioning', link: '/developers/sdk/versioning' },
          { text: 'Local SDK development', link: '/developers/sdk/local-development' },
          { text: 'SDK changelog', link: '/developers/sdk/changelog' },
          {
            text: 'Entry Source API reference',
            link: '/developers/sdk/api/index.html',
            target: '_self',
          },
          {
            text: 'Book API reference',
            link: '/developers/sdk/api/book/index.html',
            target: '_self',
          },
        ],
      },
    ],
  },
]

export default defineConfig({
  lang: 'en-US',
  title: 'Katari documentation',
  description:
    'Katari is an open-source Android app for reading manga, watching anime, and discovering stories in one unified library. Built on Mihon.',
  base,
  cleanUrls: true,
  srcExclude: ['**/AGENTS.md'],
  sitemap: {
    hostname: 'https://katariapp.github.io/katari/',
  },
  markdown: {
    config(md) {
      md.core.ruler.after('inline', 'github-mentions', (state) => {
        if (state.env.relativePath !== 'changelogs.md') return

        const mentionPattern = /(?<![\w.+-])@([a-z\d](?:[a-z\d-]{0,37}[a-z\d])?)(?![a-z\d-])/gi

        for (const token of state.tokens) {
          if (token.type !== 'inline' || !token.children) continue

          const linkedChildren = []
          let linkDepth = 0

          for (const child of token.children) {
            if (child.type === 'link_open') {
              linkDepth++
              linkedChildren.push(child)
              continue
            }
            if (child.type === 'link_close') {
              linkDepth = Math.max(0, linkDepth - 1)
              linkedChildren.push(child)
              continue
            }
            if (child.type !== 'text' || linkDepth > 0) {
              linkedChildren.push(child)
              continue
            }

            let cursor = 0
            for (const match of child.content.matchAll(mentionPattern)) {
              const start = match.index ?? 0
              if (start > cursor) {
                const text = new state.Token('text', '', 0)
                text.content = child.content.slice(cursor, start)
                linkedChildren.push(text)
              }

              const linkOpen = new state.Token('link_open', 'a', 1)
              linkOpen.attrSet('href', `https://github.com/${match[1]}`)
              linkedChildren.push(linkOpen)

              const mention = new state.Token('text', '', 0)
              mention.content = match[0]
              linkedChildren.push(mention)
              linkedChildren.push(new state.Token('link_close', 'a', -1))

              cursor = start + match[0].length
            }

            if (cursor === 0) {
              linkedChildren.push(child)
            } else if (cursor < child.content.length) {
              const text = new state.Token('text', '', 0)
              text.content = child.content.slice(cursor)
              linkedChildren.push(text)
            }
          }

          token.children = linkedChildren
        }
      })
    },
  },
  head: [
    ['link', { rel: 'icon', href: `${base}assets/katari.svg` }],
    ['meta', { name: 'theme-color', content: '#6750a4' }],
  ],
  themeConfig: {
    logo: '/assets/katari.svg',
    siteTitle: 'Katari',
    nav: [
      {
        text: latestReleaseTag ? `Get ${latestReleaseTag}` : 'Get Katari',
        activeMatch: '^/changelogs',
        items: [
          { text: 'Download', link: `${repositoryUrl}/releases/latest` },
          { text: 'Changelogs', link: '/changelogs' },
        ],
      },
      { text: 'Get started', link: '/about-project', activeMatch: '^/(about-project|migrating-to-katari|inherited-features|differences/backup-and-restore)' },
      { text: 'Features', link: '/features/content-type-reference', activeMatch: '^/(features|differences/(extensions-and-compatibility|builds-telemetry-and-privacy))/' },
      { text: 'Developers', link: '/developers/', activeMatch: '^/(developers|extensions)/' },
    ],
    sidebar,
    outline: {
      level: [2, 3],
      label: 'On this page',
    },
    search: {
      provider: 'local',
    },
    editLink: {
      pattern: 'https://github.com/katariapp/katari/edit/main/docs/:path',
      text: 'Edit this page on GitHub',
    },
    socialLinks: [
      { icon: 'github', link: 'https://github.com/katariapp/katari', ariaLabel: 'Katari on GitHub' },
    ],
    docFooter: {
      prev: 'Previous page',
      next: 'Next page',
    },
    returnToTopLabel: 'Return to top',
    sidebarMenuLabel: 'Documentation',
  },
})
