package net.phedny.decryptobot.state

import net.phedny.decryptobot.SheetsClient
import java.nio.charset.StandardCharsets.UTF_8

object Words {
    val dutchWords: List<String> = SheetsClient::class.java.getResourceAsStream("/words_nl.txt")
        .reader(UTF_8)
        .readLines()
        .filter { it.isNotBlank() }
        .map { it.toUpperCase() }

    fun pickWords(): Pair<List<String>, List<String>> {
        val words = mutableSetOf<String>()
        while (words.size < 8) {
            words.add(dutchWords.random())
        }

        return Pair(words.take(4), words.drop(4))
    }
}