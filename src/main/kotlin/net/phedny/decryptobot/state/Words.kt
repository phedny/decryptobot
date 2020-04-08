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
        val words = dutchWords.shuffled()
        return Pair(words.take(4), words.subList(4, 8))
    }
}