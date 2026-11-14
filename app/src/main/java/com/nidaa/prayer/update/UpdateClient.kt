package com.nidaa.prayer.update

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class UntrustedHostException(host: String) :
    Exception("Refused non-allowlisted or non-HTTPS host: $host")

class UpdateNetworkException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class MissingChecksumException :
    Exception("Release does not publish a SHA-256 checksum")

class ChecksumMismatchException(expected: String, actual: String) :
    Exception("SHA-256 mismatch: expected $expected, got $actual")

/**
 * The app's only network client. Plain [HttpURLConnection], no third-party
 * library, no cookies, no auth, no telemetry.
 *
 * Security rules:
 *  - every request and every redirect is checked against [HostAllowlist];
 *  - redirects are followed manually (at most [MAX_REDIRECTS]) so a
 *    cross-host jump can never bypass the allowlist;
 *  - the APK is streamed into the app's own cache directory and hashed while
 *    it downloads; a mismatch deletes the file before it can be installed.
 */
class UpdateClient(
    private val userAgent: String = USER_AGENT,
    private val connectTimeoutMs: Int = 20_000,
    private val readTimeoutMs: Int = 30_000
) {

    fun latestRelease(): ReleaseInfo {
        val connection = open(URL(LATEST_RELEASE_URL))
        try {
            val body = connection.inputStream.bufferedReader().use { reader ->
                val builder = StringBuilder()
                val buffer = CharArray(8_192)
                while (builder.length < MAX_JSON_CHARS) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    builder.append(buffer, 0, read)
                }
                builder.toString()
            }
            return ReleaseInfo.parseGitHubRelease(body)
                ?: throw UpdateNetworkException("Release JSON contains no usable APK asset")
        } finally {
            connection.disconnect()
        }
    }

    fun download(
        release: ReleaseInfo,
        target: File,
        onProgress: (Int) -> Unit = {}
    ): File {
        val expected = release.sha256 ?: throw MissingChecksumException()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, target.name + ".part")
        if (temporary.exists()) temporary.delete()

        val digest = MessageDigest.getInstance("SHA-256")
        val connection = open(URL(release.apkUrl))
        var total = -1L
        try {
            total = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        done += read
                        onProgress(
                            if (total > 0) {
                                ((done * 100) / total).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                        )
                    }
                    output.fd.sync()
                }
            }
        } catch (error: Throwable) {
            temporary.delete()
            throw if (error is UpdateNetworkException) error
            else UpdateNetworkException("Download failed", error)
        } finally {
            connection.disconnect()
        }

        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expected, ignoreCase = true)) {
            temporary.delete()
            throw ChecksumMismatchException(expected, actual)
        }

        if (target.exists()) target.delete()
        if (!temporary.renameTo(target)) {
            temporary.delete()
            throw UpdateNetworkException("Could not finalize the downloaded APK")
        }
        onProgress(100)
        return target
    }

    /**
     * Opens a validated HTTPS connection, following only allowlisted
     * redirects. The returned connection is at a 2xx response.
     */
    private fun open(url: URL): HttpURLConnection {
        var current = url
        var hops = 0
        while (true) {
            if (!HostAllowlist.isAllowed(current)) {
                throw UntrustedHostException(current.host)
            }
            val connection = current.openConnection() as? HttpURLConnection
                ?: throw UpdateNetworkException("Not an HTTP connection")
            connection.instanceFollowRedirects = false
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty(
                "Accept",
                "application/vnd.github+json, application/octet-stream"
            )

            val code = try {
                connection.responseCode
            } catch (error: Throwable) {
                connection.disconnect()
                throw UpdateNetworkException("Connection failed", error)
            }

            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (++hops > MAX_REDIRECTS) {
                    throw UpdateNetworkException("Too many redirects")
                }
                current = HostAllowlist.redirect(current, location)
                    ?: throw UntrustedHostException(location ?: "(missing Location)")
                continue
            }
            if (code !in 200..299) {
                connection.disconnect()
                throw UpdateNetworkException("HTTP $code")
            }
            return connection
        }
    }

    companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/amh2002/Nidaa/releases/latest"
        private const val USER_AGENT = "Nidaa-Android-Updater"
        private const val MAX_REDIRECTS = 5
        private const val MAX_JSON_CHARS = 2_000_000

        /** Pure helper, also used by the tests to verify a checksum. */
        fun sha256Hex(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
