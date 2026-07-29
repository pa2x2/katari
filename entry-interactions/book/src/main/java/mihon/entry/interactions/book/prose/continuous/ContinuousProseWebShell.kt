package mihon.entry.interactions.book.prose.continuous

internal val CONTINUOUS_PROSE_WEB_SHELL: String = """
    <!doctype html>
    <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
      <meta http-equiv="Content-Security-Policy"
            content="default-src 'none'; img-src https://reader.katari.invalid; font-src https://reader.katari.invalid; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'none'; frame-src 'none';">
      <style id="resource-fonts"></style>
      <style>
        :root {
          color-scheme: light dark;
          --reader-background: #ffffff;
          --reader-foreground: #202124;
          --reader-font: serif;
          --reader-size: 100%;
          --reader-line-height: 160%;
          --reader-margin: 20px;
          --reader-align: start;
        }
        * { box-sizing: border-box; }
        html, body {
          margin: 0;
          padding: 0;
          background: var(--reader-background);
          color: var(--reader-foreground);
          font-family: var(--reader-font);
          font-size: var(--reader-size);
          line-height: var(--reader-line-height);
          overflow-wrap: anywhere;
        }
        body { min-height: 100vh; }
        #document { width: 100%; }
        .section {
          padding: 1em var(--reader-margin);
          content-visibility: auto;
          contain-intrinsic-size: auto 1000px;
        }
        .section:not(.current), .transition { user-select: none; -webkit-user-select: none; }
        .block {
          margin: 0 0 1em;
          padding: 0;
          text-align: var(--reader-align);
          white-space: pre-wrap;
          content-visibility: auto;
          contain-intrinsic-size: auto 3lh;
        }
        .block:last-child { margin-bottom: 0; }
        h1.block, h2.block, h3.block, h4.block, h5.block, h6.block {
          line-height: 1.25;
          font-weight: 700;
        }
        ol.block, ul.block { list-style: none; padding-inline-start: 0; }
        .list-marker { display: inline-block; min-width: 1.5em; white-space: pre; }
        blockquote.block {
          border-inline-start: 0.2em solid color-mix(in srgb, currentColor 35%, transparent);
          padding-inline-start: 1em;
        }
        pre.block { overflow-x: auto; overflow-wrap: normal; }
        figure.block { text-align: center; }
        figure img { display: block; max-width: 100%; height: auto; margin-inline: auto; }
        figcaption { margin-top: .5em; opacity: .82; font-size: .875em; }
        table { border-collapse: collapse; display: block; overflow-x: auto; max-width: 100%; }
        caption { margin-bottom: .5em; font-weight: 700; }
        td, th { border: 1px solid color-mix(in srgb, currentColor 30%, transparent); padding: .5em; }
        th { background: color-mix(in srgb, currentColor 10%, transparent); }
        details > summary { cursor: pointer; font-weight: 700; padding-block: .5em; }
        details > .disclosure-body { padding-inline-start: 1em; }
        hr.block { margin-block: 1.5em; border: 0; border-top: 1px solid currentColor; opacity: .55; }
        .unsupported { font-style: italic; opacity: .8; }
        .transition {
          min-height: 34vh;
          padding: 2em var(--reader-margin);
          display: grid;
          place-content: center;
          text-align: center;
          border-block: 1px solid color-mix(in srgb, currentColor 25%, transparent);
          content-visibility: auto;
          contain-intrinsic-size: auto 34vh;
        }
        .transition button {
          margin-top: 1em;
          padding: .6em 1em;
          color: inherit;
          background: transparent;
          border: 1px solid currentColor;
          border-radius: .5em;
        }
        a { color: inherit; text-decoration: underline; }
      </style>
    </head>
    <body>
      <main id="document" aria-live="off"></main>
      <script>
      (() => {
        'use strict';
        let port = null;
        let generation = 0;
        let currentSectionKey = '';
        let projection = null;
        let lastSectionKey = '';
        let lastTransitionKey = '';
        let scrollTimer = 0;

        const root = document.getElementById('document');
        const send = (message) => {
          if (!port) return;
          message.generation = generation;
          port.postMessage(JSON.stringify(message));
        };
        const element = (tag, className) => {
          const node = document.createElement(tag);
          if (className) node.className = className;
          return node;
        };
        const cssColor = (value) => {
          if (!value || !/^#[0-9a-fA-F]{8}$/.test(value)) return '';
          return value;
        };
        const applyFontFamily = (node, family) => {
          if (!family) return;
          if (/^(serif|sans-serif|monospace)$/.test(family) || /^katari-[0-9a-f]{32}$/.test(family)) {
            node.style.fontFamily = family;
          }
        };
        const applyStyle = (node, style) => {
          if (!style) return;
          const foreground = cssColor(style.foreground);
          const background = cssColor(style.background);
          if (foreground) node.style.color = foreground;
          if (background) node.style.backgroundColor = background;
          if (style.alignment === 'start' || style.alignment === 'center' || style.alignment === 'end') {
            node.style.textAlign = style.alignment;
          }
          if (style.whiteSpace === 'normal' || style.whiteSpace === 'pre-wrap' || style.whiteSpace === 'pre') {
            node.style.whiteSpace = style.whiteSpace;
          }
          if (Number.isFinite(style.paddingEm)) node.style.padding = Math.max(0, Math.min(4, style.paddingEm)) + 'em';
          if (Number.isFinite(style.fontSizeScale)) node.style.fontSize = Math.max(.75, Math.min(1.5, style.fontSizeScale)) + 'em';
          if (style.bold) node.style.fontWeight = '700';
          if (style.italic) node.style.fontStyle = 'italic';
          applyFontFamily(node, style.fontFamily);
          if (style.border && Number.isFinite(style.border.width)) {
            const borderColor = cssColor(style.border.color) || 'currentColor';
            const borderStyle = /^(solid|dashed|dotted)$/.test(style.border.style) ? style.border.style : 'solid';
            node.style.border = Math.max(.5, Math.min(8, style.border.width)) + 'px ' + borderStyle + ' ' + borderColor;
          }
        };
        const activeRange = (ranges, offset) => (ranges || []).find((range) => range.start <= offset && offset < range.end);
        const richText = (text, links, styles) => {
          const fragment = document.createDocumentFragment();
          const length = text.length;
          const boundaries = new Set([0, length]);
          (links || []).forEach((range) => {
            boundaries.add(Math.max(0, Math.min(length, range.start)));
            boundaries.add(Math.max(0, Math.min(length, range.end)));
          });
          (styles || []).forEach((range) => {
            boundaries.add(Math.max(0, Math.min(length, range.start)));
            boundaries.add(Math.max(0, Math.min(length, range.end)));
          });
          const ordered = Array.from(boundaries).sort((a, b) => a - b);
          for (let index = 0; index < ordered.length - 1; index += 1) {
            const start = ordered[index];
            const end = ordered[index + 1];
            if (end <= start) continue;
            let node = document.createTextNode(text.slice(start, end));
            const style = activeRange(styles, start);
            if (style) {
              const span = element('span');
              applyStyle(span, style.style);
              span.appendChild(node);
              node = span;
            }
            const link = activeRange(links, start);
            if (link) {
              const anchor = element('a');
              anchor.href = link.targetType === 'anchor' ? '#' + encodeURIComponent(link.target) : link.target;
              anchor.dataset.targetType = link.targetType;
              anchor.dataset.target = link.target;
              anchor.appendChild(node);
              node = anchor;
            }
            fragment.appendChild(node);
          }
          return fragment;
        };
        const tagFor = (block) => {
          if (block.role === 'heading') return 'h' + Math.max(1, Math.min(6, block.level || 2));
          if (block.role === 'quote') return 'blockquote';
          if (block.role === 'preformatted') return 'pre';
          if (block.role === 'note' || block.role === 'callout') return 'aside';
          return 'p';
        };
        const renderBlock = (block) => {
          const content = block.content;
          let node;
          if (content.kind === 'list') {
            node = element(content.ordered ? 'ol' : 'ul', 'block');
            if (content.ordered && Number.isFinite(content.start)) node.start = content.start;
            (content.items || []).forEach((item) => {
              const listItem = element('li');
              const marker = element('span', 'list-marker');
              const depth = Number.isFinite(item.depth) ? Math.max(0, Math.min(8, item.depth)) : 0;
              marker.textContent =
                '  '.repeat(depth) + (item.marker == null ? '•' : item.marker) + ' ';
              listItem.appendChild(marker);
              listItem.appendChild(richText(item.text, item.links, item.inlineStyles));
              node.appendChild(listItem);
            });
          } else if (content.kind === 'figure') {
            node = element('figure', 'block');
            const image = element('img');
            image.src = content.source;
            image.alt = content.alternativeText || '';
            if (Number.isFinite(content.width)) image.width = content.width;
            if (Number.isFinite(content.height)) image.height = content.height;
            node.appendChild(image);
            if (content.caption) {
              const caption = element('figcaption');
              caption.textContent = content.caption;
              node.appendChild(caption);
            }
          } else if (content.kind === 'table') {
            node = element('table', 'block');
            if (content.caption) {
              const caption = element('caption');
              caption.appendChild(richText(content.caption, content.captionLinks, []));
              node.appendChild(caption);
            }
            const body = element('tbody');
            (content.rows || []).forEach((row) => {
              const tr = element('tr');
              row.forEach((cell) => {
                const td = element(cell.header ? 'th' : 'td');
                td.colSpan = Math.max(1, Math.min(24, cell.columnSpan || 1));
                td.rowSpan = Math.max(1, Math.min(24, cell.rowSpan || 1));
                td.appendChild(richText(cell.text, cell.links, []));
                tr.appendChild(td);
              });
              body.appendChild(tr);
            });
            node.appendChild(body);
          } else if (content.kind === 'disclosure') {
            node = element('details', 'block');
            node.open = !!content.expanded;
            node.dataset.summaryLength = String(content.summary.length);
            const summary = element('summary');
            summary.textContent = content.summary;
            node.appendChild(summary);
            const body = element('div', 'disclosure-body');
            (content.body || []).forEach((child) => body.appendChild(renderBlock(child)));
            node.appendChild(body);
          } else if (content.kind === 'break') {
            node = element('hr', 'block');
          } else {
            node = element(tagFor(block), 'block' + (content.kind === 'unsupported' ? ' unsupported' : ''));
            node.appendChild(richText(content.text || '', block.links, block.inlineStyles));
          }
          node.dataset.blockId = block.id;
          node.dataset.logicalStart = String(block.logicalStart);
          node.dataset.logicalLength = String(block.logicalLength);
          node.dataset.locatorBlockId = block.locatorBlockId;
          node.dataset.locatorOffsetBase = String(block.locatorOffsetBase || 0);
          node.dataset.locatorLogicalStart = String(block.locatorLogicalStart);
          applyStyle(node, block.style);
          return node;
        };
        const renderSection = (item) => {
          const section = element('section', 'section' + (item.key === currentSectionKey ? ' current' : ''));
          section.dataset.sectionKey = item.key;
          section.dataset.itemKey = 'section:' + item.key;
          section.dataset.signature = item.resourceId + ':' + item.signature;
          section.dataset.logicalExtent = String(item.logicalExtent);
          section.setAttribute('aria-label', item.title);
          (item.blocks || []).forEach((block) => section.appendChild(renderBlock(block)));
          return section;
        };
        const renderTransition = (item) => {
          const transition = element('section', 'transition');
          transition.dataset.transitionKey = item.key;
          transition.dataset.itemKey = item.key;
          transition.dataset.signature = [
            item.direction,
            item.label,
            item.toKey || '',
            item.loadState,
            item.message || '',
          ].join(':');
          transition.dataset.destinationSectionKey = item.toKey || '';
          transition.dataset.fromSectionKey = item.fromKey;
          transition.dataset.direction = item.direction;
          const label = element('div');
          label.textContent = item.label;
          transition.appendChild(label);
          if (item.loadState === 'loading') {
            const status = element('div');
            status.textContent = item.loadingLabel;
            transition.appendChild(status);
          } else if (item.loadState === 'failed') {
            const status = element('div');
            status.textContent = item.message || item.loadFailedLabel;
            transition.appendChild(status);
            if (item.toKey) {
              const retry = element('button');
              retry.type = 'button';
              retry.textContent = item.retryLabel;
              retry.dataset.retrySectionKey = item.toKey;
              transition.appendChild(retry);
            }
          }
          return transition;
        };
        const installFonts = (fonts) => {
          document.getElementById('resource-fonts').textContent = (fonts || []).map((font) => {
            if (!/^katari-[0-9a-f]{32}$/.test(font.family)) return '';
            if (!/^https:\/\/reader\.katari\.invalid\/resource\/[0-9a-f]{32}$/.test(font.source)) return '';
            return '@font-face{font-family:' + font.family + ';src:url("' + font.source + '")}';
          }).join('');
        };
        const applySettings = (settings) => {
          const style = document.documentElement.style;
          style.setProperty('--reader-background', cssColor(settings.background) || '#ffffff');
          style.setProperty('--reader-foreground', cssColor(settings.foreground) || '#202124');
          if (/^(serif|sans-serif|monospace)$/.test(settings.fontFamily)) {
            style.setProperty('--reader-font', settings.fontFamily);
          }
          style.setProperty('--reader-size', Math.max(50, Math.min(300, settings.fontSizePercent)) + '%');
          style.setProperty('--reader-line-height', Math.max(80, Math.min(300, settings.lineHeightPercent)) + '%');
          style.setProperty('--reader-margin', (20 * Math.max(0, Math.min(300, settings.pageMarginsPercent)) / 100) + 'px');
          if (/^(start|left|right|justify)$/.test(settings.textAlignment)) {
            style.setProperty('--reader-align', settings.textAlignment);
          }
        };
        const textNodes = (container) => {
          const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
          const result = [];
          let node;
          while ((node = walker.nextNode())) result.push(node);
          return result;
        };
        const setCaret = (block, requestedOffset) => {
          const nodes = textNodes(block);
          let remaining = Math.max(0, requestedOffset);
          for (const node of nodes) {
            if (remaining <= node.data.length) return { node, offset: remaining };
            remaining -= node.data.length;
          }
          const fallback = nodes[nodes.length - 1];
          return fallback ? { node: fallback, offset: fallback.data.length } : { node: block, offset: 0 };
        };
        const seek = (sectionKey, blockId, offset, smooth, viewportOffset) => {
          const section = root.querySelector('.section[data-section-key="' + CSS.escape(sectionKey) + '"]');
          if (!section) return false;
          const candidates = Array.from(
            section.querySelectorAll('.block[data-locator-block-id="' + CSS.escape(blockId) + '"]'),
          ).filter((candidate) => Number(candidate.dataset.locatorOffsetBase) <= offset);
          const block = candidates.sort(
            (first, second) =>
              Number(second.dataset.locatorOffsetBase) - Number(first.dataset.locatorOffsetBase),
          )[0] || section.querySelector('.block[data-block-id="' + CSS.escape(blockId) + '"]') ||
            section.firstElementChild;
          if (!block) return false;
          const localOffset = Math.max(0, offset - (Number(block.dataset.locatorOffsetBase) || 0));
          if (
            block instanceof HTMLDetailsElement &&
            localOffset > (Number(block.dataset.summaryLength) || 0)
          ) {
            block.open = true;
          }
          let disclosure = block.parentElement && block.parentElement.closest('details');
          while (disclosure) {
            disclosure.open = true;
            disclosure = disclosure.parentElement && disclosure.parentElement.closest('details');
          }
          const caret = setCaret(block, localOffset);
          const range = document.createRange();
          range.setStart(caret.node, caret.offset);
          range.collapse(true);
          const rect = range.getBoundingClientRect();
          const target = window.scrollY + rect.top - (Number.isFinite(viewportOffset) ? viewportOffset : window.innerHeight / 2);
          window.scrollTo({ top: Math.max(0, target), behavior: smooth ? 'smooth' : 'auto' });
          return true;
        };
        const offsetInBlock = (block, node, offset) => {
          const range = document.createRange();
          range.setStart(block, 0);
          try { range.setEnd(node, offset); } catch (_) { return 0; }
          return range.toString().length;
        };
        const viewportAnchor = () => {
          let target = document.elementFromPoint(window.innerWidth / 2, window.innerHeight / 2);
          if (!target) return null;
          const transition = target.closest('.transition');
          if (transition) return { transition };
          const block = target.closest('.block');
          const section = target.closest('.section');
          if (!block || !section) return null;
          const range = document.caretRangeFromPoint
            ? document.caretRangeFromPoint(window.innerWidth / 2, window.innerHeight / 2)
            : null;
          const displayedOffset = range && block.contains(range.startContainer)
            ? offsetInBlock(block, range.startContainer, range.startOffset)
            : 0;
          const logicalLength = Number(block.dataset.logicalLength) || 1;
          const locatorOffsetBase = Number(block.dataset.locatorOffsetBase) || 0;
          const logicalOffset = locatorOffsetBase + Math.max(0, Math.min(logicalLength, displayedOffset));
          const logicalStart = Number(block.dataset.locatorLogicalStart) || 0;
          const logicalExtent = Number(section.dataset.logicalExtent) || 1;
          return {
            section,
            block,
            offset: logicalOffset,
            progression: Math.max(0, Math.min(1, (logicalStart + logicalOffset) / logicalExtent)),
          };
        };
        const reportViewport = () => {
          const atDocumentEnd =
            window.scrollY + window.innerHeight >= document.documentElement.scrollHeight - 1;
          const terminalTransition = atDocumentEnd
            ? root.querySelector(
              '.transition[data-direction="next"][data-destination-section-key=""]',
            )
            : null;
          if (terminalTransition) {
            const sectionKey = terminalTransition.dataset.fromSectionKey;
            const section = (projection.items || []).find(
              (item) => item.type === 'section' && item.key === sectionKey,
            );
            const block = section && section.blocks && section.blocks[section.blocks.length - 1];
            if (block) {
              send({
                type: 'position',
                sectionKey,
                blockId: block.locatorBlockId,
                offset: block.logicalLength,
                progression: 1,
              });
            }
          }
          const anchor = viewportAnchor();
          if (!anchor) return;
          if (anchor.transition) {
            const key = anchor.transition.dataset.transitionKey;
            if (
              anchor.transition.dataset.direction === 'next' &&
              !anchor.transition.dataset.destinationSectionKey
            ) {
              const sectionKey = anchor.transition.dataset.fromSectionKey;
              const section = (projection.items || []).find(
                (item) => item.type === 'section' && item.key === sectionKey,
              );
              const block = section && section.blocks && section.blocks[section.blocks.length - 1];
              if (block) {
                send({
                  type: 'position',
                  sectionKey,
                  blockId: block.locatorBlockId,
                  offset: block.logicalLength,
                  progression: 1,
                });
              }
            }
            if (key !== lastTransitionKey && anchor.transition.dataset.destinationSectionKey) {
              lastTransitionKey = key;
              send({
                type: 'transition-reached',
                destinationSectionKey: anchor.transition.dataset.destinationSectionKey,
              });
            }
            return;
          }
          lastTransitionKey = '';
          const sectionKey = anchor.section.dataset.sectionKey;
          send({
            type: 'position',
            sectionKey,
            blockId: anchor.block.dataset.locatorBlockId,
            offset: anchor.offset,
            progression: anchor.progression,
          });
          if (sectionKey !== lastSectionKey) {
            lastSectionKey = sectionKey;
            send({
              type: 'chapter-entered',
              sectionKey,
              blockId: anchor.block.dataset.locatorBlockId,
              offset: anchor.offset,
              viewportOffset: anchor.block.getBoundingClientRect().top,
            });
          }
        };
        const normalizeSelection = () => {
          const selection = window.getSelection();
          if (!selection || selection.rangeCount === 0 || selection.isCollapsed) {
            send({ type: 'selection-cleared' });
            return;
          }
          const current = root.querySelector('.section.current');
          if (!current) return;
          const range = selection.getRangeAt(0);
          if (!current.contains(range.startContainer) || !current.contains(range.endContainer)) {
            const replacement = document.createRange();
            replacement.selectNodeContents(current);
            if (current.contains(range.startContainer)) {
              replacement.setStart(range.startContainer, range.startOffset);
            }
            if (current.contains(range.endContainer)) {
              replacement.setEnd(range.endContainer, range.endOffset);
            }
            selection.removeAllRanges();
            selection.addRange(replacement);
          }
          const activeRange = selection.getRangeAt(0);
          const text = selection.toString();
          if (!text.trim()) {
            send({ type: 'selection-cleared' });
            return;
          }
          const rect = activeRange.getBoundingClientRect();
          const nodeAddress = (node) => {
            const address = [];
            let cursor = node;
            while (cursor && cursor !== current) {
              const parent = cursor.parentNode;
              if (!parent) break;
              address.push(Array.prototype.indexOf.call(parent.childNodes, cursor));
              cursor = parent;
            }
            return address.reverse().join('.');
          };
          let textHash = 2166136261;
          for (let index = 0; index < text.length; index += 1) {
            textHash ^= text.charCodeAt(index);
            textHash = Math.imul(textHash, 16777619);
          }
          send({
            type: 'selection',
            identity: [
              currentSectionKey,
              nodeAddress(activeRange.startContainer),
              activeRange.startOffset,
              nodeAddress(activeRange.endContainer),
              activeRange.endOffset,
              (textHash >>> 0).toString(16),
            ].join(':'),
            text,
            left: rect.left,
            top: rect.top,
            right: rect.right,
            bottom: rect.bottom,
          });
        };
        const render = (command) => {
          const previousCurrentSectionKey = currentSectionKey;
          const previousAnchor = viewportAnchor();
          const previousAnchorElement = previousAnchor &&
            (previousAnchor.block || previousAnchor.transition);
          const previousAnchorTop = previousAnchorElement
            ? previousAnchorElement.getBoundingClientRect().top
            : null;
          generation = command.generation;
          projection = command.projection;
          currentSectionKey = projection.currentSectionKey;
          applySettings(command.settings);
          installFonts(projection.fonts);
          const existing = new Map(Array.from(root.children).map((node) => [node.dataset.itemKey, node]));
          (projection.items || []).forEach((item) => {
            const itemKey = item.type === 'section' ? 'section:' + item.key : item.key;
            let node = existing.get(itemKey);
            if (item.type === 'section') {
              const signature = item.resourceId + ':' + item.signature;
              if (node && node.dataset.signature !== signature) {
                node.remove();
                node = null;
              }
              if (!node) node = renderSection(item);
              node.classList.toggle('current', item.key === currentSectionKey);
            } else {
              const signature = [
                item.direction,
                item.label,
                item.toKey || '',
                item.loadState,
                item.message || '',
              ].join(':');
              if (node && node.dataset.signature !== signature) {
                node.remove();
                node = null;
              }
              if (!node) node = renderTransition(item);
            }
            existing.delete(itemKey);
            root.appendChild(node);
          });
          existing.forEach((node) => node.remove());
          if (previousCurrentSectionKey && previousCurrentSectionKey !== currentSectionKey) {
            const selection = window.getSelection();
            if (selection) selection.removeAllRanges();
          }
          lastSectionKey = currentSectionKey;
          requestAnimationFrame(() => {
            if (
              previousAnchor &&
              previousAnchor.block &&
              Number.isFinite(previousAnchorTop)
            ) {
              const section = root.querySelector(
                '.section[data-section-key="' +
                  CSS.escape(previousAnchor.section.dataset.sectionKey) +
                  '"]',
              );
              const block = section && section.querySelector(
                '.block[data-block-id="' +
                  CSS.escape(previousAnchor.block.dataset.blockId) +
                  '"]',
              );
              if (block) {
                const delta = block.getBoundingClientRect().top - previousAnchorTop;
                if (Math.abs(delta) > .5) window.scrollBy(0, delta);
              } else {
                seek(
                  command.initial.sectionKey,
                  command.initial.blockId,
                  command.initial.offset,
                  false,
                  command.initial.offset === 0 ? 0 : window.innerHeight / 2,
                );
              }
            } else if (
              previousAnchor &&
              previousAnchor.transition &&
              Number.isFinite(previousAnchorTop)
            ) {
              const transition = root.querySelector(
                '.transition[data-item-key="' +
                  CSS.escape(previousAnchor.transition.dataset.itemKey) +
                  '"]',
              );
              if (transition) {
                const delta = transition.getBoundingClientRect().top - previousAnchorTop;
                if (Math.abs(delta) > .5) window.scrollBy(0, delta);
              } else {
                seek(
                  command.initial.sectionKey,
                  command.initial.blockId,
                  command.initial.offset,
                  false,
                  command.initial.offset === 0 ? 0 : window.innerHeight / 2,
                );
              }
            } else {
              seek(
                command.initial.sectionKey,
                command.initial.blockId,
                command.initial.offset,
                false,
                command.initial.offset === 0 ? 0 : window.innerHeight / 2,
              );
            }
            requestAnimationFrame(() => {
              reportViewport();
              send({ type: 'prepared' });
            });
          });
        };
        const receive = (event) => {
          let command;
          try { command = JSON.parse(event.data); } catch (_) { return; }
          if (!command || !Number.isSafeInteger(command.generation)) return;
          if (command.type === 'render') {
            render(command);
          } else if (command.generation === generation && command.type === 'seek') {
            seek(command.sectionKey, command.blockId, command.offset, !!command.smooth, window.innerHeight / 2);
          } else if (command.generation === generation && command.type === 'clear-selection') {
            const selection = window.getSelection();
            if (selection) selection.removeAllRanges();
          }
        };
        window.addEventListener('message', (event) => {
          if (!event.ports || event.ports.length !== 1 || port) return;
          const announcedGeneration = Number(event.data);
          if (!Number.isSafeInteger(announcedGeneration)) return;
          generation = announcedGeneration;
          port = event.ports[0];
          port.onmessage = receive;
          port.start();
          send({ type: 'ready' });
        });
        window.addEventListener('scroll', () => {
          clearTimeout(scrollTimer);
          scrollTimer = setTimeout(reportViewport, 40);
        }, { passive: true });
        document.addEventListener('selectionchange', () => {
          clearTimeout(normalizeSelection.timer);
          normalizeSelection.timer = setTimeout(normalizeSelection, 80);
        });
        document.addEventListener('toggle', (event) => {
          if (event.target instanceof HTMLDetailsElement) setTimeout(reportViewport, 0);
        }, true);
        document.addEventListener('click', (event) => {
          const retry = event.target.closest && event.target.closest('button[data-retry-section-key]');
          if (retry) {
            event.preventDefault();
            send({ type: 'transition-retry', destinationSectionKey: retry.dataset.retrySectionKey });
            return;
          }
          const anchor = event.target.closest && event.target.closest('a');
          if (anchor) {
            event.preventDefault();
            if (anchor.dataset.targetType === 'anchor') {
              const section = projection.items.find((item) => item.type === 'section' && item.key === currentSectionKey);
              const target = section && section.anchors[anchor.dataset.target];
              if (target) seek(currentSectionKey, target.blockId, target.offset, true, window.innerHeight / 3);
            } else if (anchor.dataset.targetType === 'external') {
              send({ type: 'external-link', url: anchor.dataset.target });
            }
            return;
          }
          const selection = window.getSelection();
          if (selection && !selection.isCollapsed) return;
          send({ type: 'tap', fraction: Math.max(0, Math.min(1, event.clientX / window.innerWidth)) });
        });
      })();
      </script>
    </body>
    </html>
""".trimIndent()
