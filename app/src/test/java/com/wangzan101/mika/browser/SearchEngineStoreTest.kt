package com.wangzan101.mika.browser

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SearchEngineStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun custom(name: String = "Example") = CustomSearchEngine.create(name, "https://s.example.com/?q=%s")!!

    @Test
    fun defaultsToGoogle() {
        val choices = SearchEngineStore(tmp.root).choices.value
        assertEquals(SearchEngine.Google, choices.selected)
        assertEquals(emptyList<CustomSearchEngine>(), choices.custom)
    }

    @Test
    fun remembersTheChoice() {
        SearchEngineStore(tmp.root).set(SearchEngine.Google)
        assertEquals(SearchEngine.Google, SearchEngineStore(tmp.root).choices.value.selected)
    }

    @Test
    fun customEnginesPersistAndAreSelected() {
        val engine = custom()
        SearchEngineStore(tmp.root).addCustom(engine)
        val reloaded = SearchEngineStore(tmp.root).choices.value
        assertEquals(engine, reloaded.selected)
        assertEquals(listOf(engine), reloaded.custom)
        assertEquals(engine, reloaded.all.last())
    }

    @Test
    fun removingTheSelectedCustomFallsBackToGoogle() {
        val store = SearchEngineStore(tmp.root)
        val engine = custom()
        store.addCustom(engine)
        store.removeCustom(engine.id)
        assertEquals(SearchEngine.Google, store.choices.value.selected)
        assertEquals(emptyList<CustomSearchEngine>(), store.choices.value.custom)
        assertEquals(SearchEngine.Google, SearchEngineStore(tmp.root).choices.value.selected)
    }

    @Test
    fun removingAnotherCustomKeepsTheSelection() {
        val store = SearchEngineStore(tmp.root)
        val a = custom("A")
        val b = custom("B")
        store.addCustom(a)
        store.addCustom(b)
        store.set(SearchEngine.Google)
        store.removeCustom(a.id)
        assertEquals(SearchEngine.Google, store.choices.value.selected)
        assertEquals(listOf(b), store.choices.value.custom)
    }

    @Test
    fun unknownOrBrokenFileFallsBackToGoogle() {
        File(tmp.root, "browser_search_engines.json").writeText("nope")
        val choices = SearchEngineStore(tmp.root).choices.value
        assertEquals(SearchEngine.Google, choices.selected)
        assertEquals(emptyList<CustomSearchEngine>(), choices.custom)
    }

    @Test
    fun invalidCustomEntriesAreSkipped() {
        File(tmp.root, "browser_search_engines.json").writeText(
            """{"selected":"custom:bad","custom":[{"id":"custom:bad","name":"Bad","template":"https://x.example.com/?q="},{"id":"custom:ok","name":"OK","template":"https://x.example.com/?q=%s"}]}""",
        )
        val choices = SearchEngineStore(tmp.root).choices.value
        assertEquals(SearchEngine.Google, choices.selected)
        assertEquals(listOf("OK"), choices.custom.map { it.displayName })
    }
}
