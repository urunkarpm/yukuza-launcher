package com.yukuza.launcher.data.repository

import com.yukuza.launcher.data.remote.GithubReleasesApi
import com.yukuza.launcher.data.remote.dto.GithubAssetDto
import com.yukuza.launcher.data.remote.dto.GithubReleaseDto
import com.yukuza.launcher.domain.model.UpdateInfo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateRepositoryTest {

    private val api = mockk<GithubReleasesApi>()
    private val repo = UpdateRepository(api, currentVersion = "1.2")

    private fun release(
        tag: String,
        assetName: String = "yukuza-release.apk",
        url: String = "https://example.com/yukuza-release.apk",
        body: String = "Release notes",
    ) = GithubReleaseDto(
        tagName = tag,
        body = body,
        assets = listOf(GithubAssetDto(name = assetName, browserDownloadUrl = url)),
    )

    @Test
    fun `returns UpdateInfo when remote version is newer`() = runTest {
        coEvery { api.getLatestRelease() } returns release("v1.3")
        val result = repo.checkForUpdate()
        assertEquals(UpdateInfo("1.3", "https://example.com/yukuza-release.apk", "Release notes"), result)
    }

    @Test
    fun `returns null when remote version equals current`() = runTest {
        coEvery { api.getLatestRelease() } returns release("v1.2")
        assertNull(repo.checkForUpdate())
    }

    @Test
    fun `returns null when remote version is older`() = runTest {
        coEvery { api.getLatestRelease() } returns release("v1.1")
        assertNull(repo.checkForUpdate())
    }

    @Test
    fun `correctly compares minor versions above 9`() = runTest {
        val repoV9 = UpdateRepository(api, currentVersion = "1.9")
        coEvery { api.getLatestRelease() } returns release("v1.10")
        val result = repoV9.checkForUpdate()
        assertEquals("1.10", result?.latestVersion)
    }

    @Test
    fun `pads missing segments — 1_3 equals 1_3_0`() = runTest {
        val repoV130 = UpdateRepository(api, currentVersion = "1.3.0")
        coEvery { api.getLatestRelease() } returns release("v1.3")
        assertNull(repoV130.checkForUpdate())
    }

    @Test
    fun `returns null when tag cannot be parsed`() = runTest {
        coEvery { api.getLatestRelease() } returns release("v1.3-beta")
        assertNull(repo.checkForUpdate())
    }

    @Test
    fun `prefers asset with release in name`() = runTest {
        val releaseWithMultiple = GithubReleaseDto(
            tagName = "v1.3",
            body = "notes",
            assets = listOf(
                GithubAssetDto("yukuza-debug.apk", "https://example.com/debug.apk"),
                GithubAssetDto("yukuza-release.apk", "https://example.com/release.apk"),
            ),
        )
        coEvery { api.getLatestRelease() } returns releaseWithMultiple
        assertEquals("https://example.com/release.apk", repo.checkForUpdate()?.downloadUrl)
    }

    @Test
    fun `returns null when no apk asset found`() = runTest {
        val noApk = GithubReleaseDto("v1.3", "notes", listOf(GithubAssetDto("source.zip", "https://example.com/source.zip")))
        coEvery { api.getLatestRelease() } returns noApk
        assertNull(repo.checkForUpdate())
    }

    @Test
    fun `returns null on network exception`() = runTest {
        coEvery { api.getLatestRelease() } throws RuntimeException("network error")
        assertNull(repo.checkForUpdate())
    }

    @Test
    fun `handles empty release notes`() = runTest {
        coEvery { api.getLatestRelease() } returns release("v1.3", body = "")
        val result = repo.checkForUpdate()
        assertEquals("", result?.releaseNotes)
    }
}
