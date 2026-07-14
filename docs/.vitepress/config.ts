import { defineConfig } from 'vitepress'
import type { DefaultTheme } from 'vitepress'

const base = '/katari/'

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
        text: 'Entry SDK',
        collapsed: false,
        items: [
          { text: 'Overview', link: '/developers/sdk/' },
          { text: 'Data model', link: '/developers/sdk/data-model' },
          { text: 'Content types', link: '/developers/sdk/content-types' },
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
  head: [
    ['link', { rel: 'icon', href: `${base}assets/katari.svg` }],
    ['meta', { name: 'theme-color', content: '#6750a4' }],
  ],
  themeConfig: {
    logo: '/assets/katari.svg',
    siteTitle: 'Katari',
    nav: [
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
