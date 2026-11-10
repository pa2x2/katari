Chromium for Katari's desktop web view

These are the files of the Chromium Embedded Framework (CEF) that JetBrains builds for the JCEF of the JetBrains
Runtime, unchanged, taken from the runtime Katari's desktop app runs on.

- CEF.txt: the licence of the Chromium Embedded Framework, https://github.com/chromiumembedded/cef
- JCEF.txt: the licence of JetBrains' JCEF, whose cef_server and jcef_helper programs are included,
  https://github.com/JetBrains/jcef
- Chromium.txt: the licence of Chromium, https://www.chromium.org

Chromium includes software of many other projects. Their licences are in resources.pak and are shown by the page
chrome://credits, which Katari's web view opens.

libcef.so includes FFmpeg, licensed under the GNU Lesser General Public License version 2.1 or later
(https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html). Its source as built into this Chromium is at
https://chromium.googlesource.com/chromium/third_party/ffmpeg, at the revision Chromium's DEPS file names for the
Chromium version in this archive's name.
