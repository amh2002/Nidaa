package com.nidaa.prayer.update

import java.net.URL

/**
 * The complete, closed set of hosts the updater may ever contact.
 *
 * Enforced for the initial request **and** for every redirect hop. HTTPS is
 * mandatory, credentials in the URL are refused, and only the default port
 * (443) is accepted. This is the only network code in the app.
 */
object HostAllowlist {

    val HOSTS: Set<String> = linkedSetOf(
        "api.github.com",
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com"
    )

    fun isAllowed(url: URL): Boolean {
        if (!url.protocol.equals("https", ignoreCase = true)) return false
        if (url.userInfo != null) return false
        if (url.port != -1 && url.port != 443) return false
        return normalize(url.host) in HOSTS
    }

    fun isAllowed(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val url = runCatching { URL(raw) }.getOrNull() ?: return false
        return isAllowed(url)
    }

    /**
     * Resolves a redirect against the current URL and returns it only when the
     * target also passes the allowlist. Null means "refuse the redirect".
     */
    fun redirect(base: URL, location: String?): URL? {
        if (location.isNullOrBlank()) return null
        val next = runCatching { URL(base, location) }.getOrNull() ?: return null
        return next.takeIf(::isAllowed)
    }

    private fun normalize(host: String?): String =
        host?.trim()?.trimEnd('.')?.lowercase().orEmpty()
}
