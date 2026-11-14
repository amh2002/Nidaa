package com.mawaqiti.plus

import com.nidaa.prayer.update.HostAllowlist
import com.nidaa.prayer.update.ReleaseInfo
import com.nidaa.prayer.update.SemanticVersion
import com.nidaa.prayer.update.UpdateClient
import com.nidaa.prayer.update.UpdatePolicy
import java.io.File
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** beta 7.95: pure updater rules (no Android, no network). */
class UpdateCheckerTest {

    // ---------------------------------------------------------------- version

    @Test
    fun `v prefix is optional and 1_0 equals 1_0_0`() {
        assertEquals(SemanticVersion.parse("1.0.0"), SemanticVersion.parse("v1.0.0"))
        assertEquals(SemanticVersion.parse("1.0.0"), SemanticVersion.parse("V1.0.0"))
        assertEquals(SemanticVersion.parse("1.0.0"), SemanticVersion.parse("1.0"))
        assertEquals(SemanticVersion.parse("1.0.0"), SemanticVersion.parse("1"))
        assertEquals(0, SemanticVersion.parse("1.0")!!.compareTo(SemanticVersion.parse("1.0.0")!!))
    }

    @Test
    fun `release ordering follows semantic version rules`() {
        val beta79 = SemanticVersion.parse("1.0.0-beta7.9")!!
        val beta795 = SemanticVersion.parse("1.0.0-beta7.95")!!
        val beta710 = SemanticVersion.parse("1.0.0-beta7.10")!!
        val stable = SemanticVersion.parse("1.0.0")!!

        assertTrue(beta795 > beta79)
        assertTrue(beta710 > beta79)
        assertTrue(beta795 > beta710)
        assertTrue(stable > beta795)
        assertTrue(SemanticVersion.parse("1.0.1")!! > stable)
    }

    @Test
    fun `build metadata is ignored and malformed tags are rejected`() {
        assertEquals(SemanticVersion.parse("1.2.3"), SemanticVersion.parse("v1.2.3+42"))
        assertNull(SemanticVersion.parse(""))
        assertNull(SemanticVersion.parse("v"))
        assertNull(SemanticVersion.parse("release"))
        assertNull(SemanticVersion.parse("1.2.3.4"))
        assertNull(SemanticVersion.parse("1.x.0"))
    }

    // -------------------------------------------------------------- allowlist

    @Test
    fun `only the four HTTPS hosts are allowed`() {
        for (host in HostAllowlist.HOSTS) {
            assertTrue(host, HostAllowlist.isAllowed("https://$host/some/path"))
        }
        assertFalse(HostAllowlist.isAllowed("http://api.github.com/repos/x"))
        assertFalse(HostAllowlist.isAllowed("https://evil.example/app.apk"))
        assertFalse(HostAllowlist.isAllowed("https://api.github.com.evil.example/x"))
        assertFalse(HostAllowlist.isAllowed("https://user:pass@api.github.com/x"))
        assertFalse(HostAllowlist.isAllowed("https://api.github.com:8443/x"))
        assertFalse(HostAllowlist.isAllowed(null))
    }

    @Test
    fun `redirects are validated hop by hop`() {
        val github = URL("https://github.com/amh2002/Nidaa/releases/download/x.apk")
        assertNotNull(
            HostAllowlist.redirect(github, "https://objects.githubusercontent.com/blob")
        )
        assertNotNull(
            HostAllowlist.redirect(github, "https://release-assets.githubusercontent.com/blob")
        )
        assertNull(HostAllowlist.redirect(github, "https://evil.example/blob"))
        assertNull(HostAllowlist.redirect(github, "http://objects.githubusercontent.com/blob"))
        assertNull(HostAllowlist.redirect(github, "//evil.example/blob"))
        assertNull(HostAllowlist.redirect(github, null))
    }

    // ----------------------------------------------------------------- parse

    private val validJson = """
        {
          "tag_name": "v1.0.0-beta7.95",
          "html_url": "https://github.com/amh2002/Nidaa/releases/tag/v1.0.0-beta7.95",
          "body": "Release notes\n\nSHA-256: 2CF24DBA5FB0A30E26E83B2AC5B9E29E1B161E5C1FA7425E73043362938B9824",
          "assets": [
            {
              "name": "notes.txt",
              "browser_download_url": "https://github.com/amh2002/Nidaa/releases/download/v1/notes.txt"
            },
            {
              "name": "Nidaa-1.0.0-beta7.95.apk",
              "browser_download_url": "https://github.com/amh2002/Nidaa/releases/download/v1/Nidaa.apk"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `release json yields tag, apk asset and sha line`() {
        val release = ReleaseInfo.parseGitHubRelease(validJson)
        assertNotNull(release)
        release!!
        assertEquals("v1.0.0-beta7.95", release.tag)
        assertEquals(SemanticVersion.parse("1.0.0-beta7.95"), release.version)
        assertEquals("Nidaa-1.0.0-beta7.95.apk", release.apkName)
        assertTrue(release.apkUrl.endsWith("Nidaa.apk"))
        assertTrue(release.pageUrl.startsWith("https://github.com/"))
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            release.sha256
        )
        assertTrue(
            release.version > SemanticVersion.parse("1.0.0-beta7.9")!!
        )
    }

    @Test
    fun `release without a published sha is parsed but cannot install`() {
        val release = ReleaseInfo.parseGitHubRelease(
            validJson.replace(
                "SHA-256: 2CF24DBA5FB0A30E26E83B2AC5B9E29E1B161E5C1FA7425E73043362938B9824",
                "No checksum today"
            )
        )
        assertNotNull(release)
        assertNull(release!!.sha256)
    }

    @Test
    fun `untrusted or missing apk assets are refused`() {
        assertNull(
            ReleaseInfo.parseGitHubRelease(
                validJson.replace(
                    "https://github.com/amh2002/Nidaa/releases/download/v1/Nidaa.apk",
                    "https://evil.example/Nidaa.apk"
                )
            )
        )
        assertNull(
            ReleaseInfo.parseGitHubRelease(
                validJson.replace(
                    "https://github.com/amh2002/Nidaa/releases/download/v1/Nidaa.apk",
                    "http://github.com/amh2002/Nidaa/releases/download/v1/Nidaa.apk"
                )
            )
        )
        assertNull(ReleaseInfo.parseGitHubRelease("""{"tag_name":"v1.0.0","assets":[]}"""))
        assertNull(ReleaseInfo.parseGitHubRelease("not json"))
    }

    @Test
    fun `storage round trip keeps the release intact`() {
        val release = ReleaseInfo.parseGitHubRelease(validJson)!!
        val restored = ReleaseInfo.fromStorage(release.storageJson())!!
        assertEquals(release, restored)
        assertEquals(release.sha256, restored.sha256)
    }

    // -------------------------------------------------------------- checksum

    @Test
    fun `sha256 helper identifies pass and mismatch`() {
        val file = File.createTempFile("nidaa-checksum", ".bin").apply {
            writeText("hello")
            deleteOnExit()
        }
        val expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
        assertEquals(expected, UpdateClient.sha256Hex(file))
        assertNotEquals(expected, UpdateClient.sha256Hex(file).reversed())
    }

    // --------------------------------------------------------------- cadence

    @Test
    fun `automatic checks run at most once per 24 hours and never when off`() {
        val day = UpdatePolicy.CHECK_INTERVAL_MS
        assertFalse(UpdatePolicy.shouldAutoCheck(false, 0L, 10_000L))
        assertTrue(UpdatePolicy.shouldAutoCheck(true, 0L, 10_000L))
        assertFalse(UpdatePolicy.shouldAutoCheck(true, 10_000L, 10_000L + day - 1))
        assertTrue(UpdatePolicy.shouldAutoCheck(true, 10_000L, 10_000L + day))
        // Clock moved backwards: do not lock the updater out.
        assertTrue(UpdatePolicy.shouldAutoCheck(true, 10_000L, 5_000L))
    }
}
