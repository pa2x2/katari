package mihon.script.runtime

import okhttp3.OkHttpClient

/**
 * Where a script's `fetch` goes. [userAgent] is sent when the script sets no `User-Agent` of its own; the app passes
 * its own client and user-agent setting, which its WebView uses too, so cookies and Cloudflare clearance carry over.
 */
class ScriptNetwork(val client: OkHttpClient, val userAgent: () -> String)
