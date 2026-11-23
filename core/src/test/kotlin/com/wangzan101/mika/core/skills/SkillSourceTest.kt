package com.wangzan101.mika.core.skills

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SkillSourceTest {
    private lateinit var server: MockWebServer
    private val client = OkHttpClient()

    @Before fun setUp() { server = MockWebServer().also { it.start() } }

    @After fun tearDown() { server.shutdown() }

    @Test fun fetchesATextSkillAndDropsTheByteOrderMark() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "text/plain; charset=utf-8")
            .setBody("\uFEFF---\nname: tidy-inbox\n---\nSort the inbox.\n"))
        val result = SkillSource.fetch(server.url("/SKILL.md").toString(), client)
        assertEquals(
            SkillSource.Result.Fetched("---\nname: tidy-inbox\n---\nSort the inbox.", server.url("/SKILL.md").toString()),
            result,
        )
    }

    @Test fun aFileLinkIsTurnedIntoItsRawDownloadLink() {
        assertEquals(
            "https://raw.githubusercontent.com/acme/skills/main/email/SKILL.md",
            SkillSource.rawUrl("https://github.com/acme/skills/blob/main/email/SKILL.md"),
        )
        assertEquals(
            "https://raw.githubusercontent.com/acme/skills/main/SKILL.md",
            SkillSource.rawUrl("https://github.com/acme/skills/raw/main/SKILL.md"),
        )
        // 查看模式与普通链接都只是去掉查询串,主机和路径不动
        assertEquals("https://example.com/skill/SKILL.md", SkillSource.rawUrl("https://example.com/skill/SKILL.md?plain=1"))
        assertEquals("https://example.com/a/SKILL.md", SkillSource.rawUrl("https://example.com/a/SKILL.md#readme"))
    }

    @Test fun onlyHttpAndHttpsAreAccepted() = runTest {
        assertEquals(
            SkillSource.Result.Failed(SkillSource.Problem.BadScheme),
            SkillSource.fetch("file:///storage/emulated/0/SKILL.md", client),
        )
        assertEquals(
            SkillSource.Result.Failed(SkillSource.Problem.BadScheme),
            SkillSource.fetch("not a url", client),
        )
        assertEquals(0, server.requestCount)
    }

    @Test fun anErrorStatusIsReportedAsAnHttpError() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))
        assertEquals(
            SkillSource.Result.Failed(SkillSource.Problem.HttpError),
            SkillSource.fetch(server.url("/missing.md").toString(), client),
        )
    }

    @Test fun aBinaryPayloadIsNotMistakenForASkill() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/pdf").setBody("%PDF-1.7"))
        assertEquals(
            SkillSource.Result.Failed(SkillSource.Problem.NotText),
            SkillSource.fetch(server.url("/skill.pdf").toString(), client),
        )
    }

    @Test fun anOversizedReplyIsRefusedWithoutReadingItAll() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "text/plain")
            .setBody("x".repeat(SkillSource.MAX_BYTES + 1)))
        assertEquals(
            SkillSource.Result.Failed(SkillSource.Problem.TooLarge),
            SkillSource.fetch(server.url("/huge.md").toString(), client),
        )
    }

    @Test fun anEmptyReplyIsRefused() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "text/plain").setBody("   \n"))
        assertEquals(
            SkillSource.Result.Failed(SkillSource.Problem.Empty),
            SkillSource.fetch(server.url("/blank.md").toString(), client),
        )
    }

    @Test fun aMarkdownContentTypeIsAccepted() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/markdown").setBody("---\nname: x\n---\nDo it."))
        val result = SkillSource.fetch(server.url("/skill").toString(), client)
        assertTrue(result is SkillSource.Result.Fetched)
    }
}
