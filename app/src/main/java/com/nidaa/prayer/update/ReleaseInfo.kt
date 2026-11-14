package com.nidaa.prayer.update

import org.json.JSONObject

/**
 * One published Nidaa release, reduced to what the updater needs.
 *
 * [sha256] is the checksum parsed from the release body's `SHA-256: <hex>`
 * line. It is allowed to be null (the release simply cannot be installed); a
 * mismatch is always fatal.
 */
data class ReleaseInfo(
    val tag: String,
    val version: SemanticVersion,
    val pageUrl: String,
    val apkUrl: String,
    val apkName: String,
    val sha256: String?
) {

    fun storageJson(): String = JSONObject()
        .put("tag", tag)
        .put("pageUrl", pageUrl)
        .put("apkUrl", apkUrl)
        .put("apkName", apkName)
        .put("sha256", sha256 ?: JSONObject.NULL)
        .toString()

    companion object {
        /** Browser fallback target. Never fetched by the app itself. */
        const val RELEASES_PAGE = "https://github.com/amh2002/Nidaa/releases"

        private val SHA_LINE = Regex(
            "SHA[- ]?256\\s*[:=]\\s*([A-Fa-f0-9]{64})",
            RegexOption.IGNORE_CASE
        )
        private val SAFE_NAME = Regex("[^A-Za-z0-9._-]")

        /**
         * Parses the GitHub `releases/latest` JSON. Returns null when the tag
         * is not a semantic version, when no HTTPS APK asset on an allowed
         * host exists, or when the payload is malformed.
         */
        fun parseGitHubRelease(json: String): ReleaseInfo? {
            val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
            val tag = root.optString("tag_name").trim().takeIf { it.isNotEmpty() } ?: return null
            val version = SemanticVersion.parse(tag) ?: return null

            val assets = root.optJSONArray("assets") ?: return null
            var apkUrl: String? = null
            var apkName: String? = null
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name").trim()
                if (!name.endsWith(".apk", ignoreCase = true)) continue
                val url = asset.optString("browser_download_url").trim()
                if (!HostAllowlist.isAllowed(url)) continue
                apkUrl = url
                apkName = name
                break
            }
            val resolvedApkUrl = apkUrl ?: return null

            val htmlUrl = root.optString("html_url").trim()
            val pageUrl = if (HostAllowlist.isAllowed(htmlUrl)) htmlUrl else RELEASES_PAGE

            val body = root.optString("body")
            val sha256 = SHA_LINE.find(body)?.groupValues?.get(1)?.lowercase()

            return ReleaseInfo(
                tag = tag,
                version = version,
                pageUrl = pageUrl,
                apkUrl = resolvedApkUrl,
                apkName = SAFE_NAME.replace(apkName ?: "Nidaa.apk", "_"),
                sha256 = sha256
            )
        }

        /** Reads a [ReleaseInfo] back from [storageJson]. */
        fun fromStorage(json: String?): ReleaseInfo? {
            if (json.isNullOrBlank()) return null
            val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
            val tag = root.optString("tag").takeIf { it.isNotEmpty() } ?: return null
            val version = SemanticVersion.parse(tag) ?: return null
            val pageUrl = root.optString("pageUrl")
            val apkUrl = root.optString("apkUrl")
            if (!HostAllowlist.isAllowed(apkUrl)) return null
            return ReleaseInfo(
                tag = tag,
                version = version,
                pageUrl = if (HostAllowlist.isAllowed(pageUrl)) pageUrl else RELEASES_PAGE,
                apkUrl = apkUrl,
                apkName = SAFE_NAME.replace(root.optString("apkName", "Nidaa.apk"), "_"),
                sha256 = root.optString("sha256").takeIf { it.matches(Regex("[a-f0-9]{64}")) }
            )
        }
    }
}
