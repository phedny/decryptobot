package net.phedny.decryptobot.state

import net.phedny.decryptobot.SheetsClient
import java.nio.charset.StandardCharsets.UTF_8

object Words {
    private val words: Map<String, List<String>> = listOf("nl", "en")
        .map { Pair(it, SheetsClient::class.java.getResourceAsStream("/words_$it.txt").reader(UTF_8).readLines().filter { it.isNotBlank() }.map { it.toUpperCase() }) }
        .toMap()

    val wordLists: List<String>
        get() = words.keys.toList().sorted()

    fun pickWords(listName: String): Pair<List<String>, List<String>>? {
        val words = words[listName]?.shuffled() ?: return null
        return Pair(words.take(4), words.subList(4, 8))
    }
}