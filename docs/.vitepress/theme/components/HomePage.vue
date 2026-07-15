<script setup lang="ts">
import { withBase } from 'vitepress'
</script>

<template>
  <main class="katari-home-content">
    <section class="katari-home-hero">
      <img :src="withBase('/assets/katari.svg')" alt="Katari">
      <div>
        <h1>Katari</h1>
        <p class="katari-home-lead">Manga, anime, and books in one library.</p>
        <p class="katari-home-summary">Katari (語り, “storytelling”) is an open-source Android app and an independent fork of Mihon.</p>
        <div class="katari-home-actions">
          <a class="katari-button katari-button--primary" href="https://github.com/katariapp/katari/releases/latest" target="_blank" rel="noreferrer">Download for Android</a>
          <a class="katari-button" :href="withBase('/migrating-to-katari')">Migrate from Mihon</a>
        </div>
      </div>
    </section>

    <section class="katari-doc-groups">
      <div>
        <h2>Get started</h2>
        <ul>
          <li><a :href="withBase('/about-project')">About project</a></li>
          <li><a :href="withBase('/migrating-to-katari')">Migrate from Mihon</a></li>
          <li><a :href="withBase('/inherited-features')">Features inherited from Mihon</a></li>
          <li><a :href="withBase('/differences/backup-and-restore')">Backup and restore differences</a></li>
        </ul>
      </div>

      <div>
        <h2>Explore Katari</h2>
        <ul>
          <li><a :href="withBase('/features/unified-library')">Unified library</a></li>
          <li><a :href="withBase('/features/content-type-reference')">Content type support</a></li>
          <li><a :href="withBase('/features/profiles')">Profiles</a></li>
          <li><a :href="withBase('/features/merged-entries')">Merged entries</a></li>
          <li><a :href="withBase('/features/book-reading')">Book reading</a></li>
          <li><a :href="withBase('/features/video-playback')">Video playback</a></li>
          <li><a :href="withBase('/features/feeds-and-discovery')">Feeds and discovery</a></li>
        </ul>
      </div>

      <div>
        <h2>Developers</h2>
        <ul>
          <li><a :href="withBase('/developers/')">Developer documentation</a></li>
          <li><a :href="withBase('/extensions/')">Extension development guide</a></li>
          <li><a :href="withBase('/developers/sdk/')">Entry SDK</a></li>
          <li><a :href="withBase('/differences/extensions-and-compatibility')">Compatibility with Mihon extensions</a></li>
        </ul>
      </div>
    </section>
  </main>
</template>
