package net.phedny.decryptobot

import kotlin.random.Random

fun main() {
//    val gameId = Random.nextInt()
//    val sheetsClient = SheetsClient()
//    println("New spreadsheet @ ${sheetsClient.initializeNewSpreadsheet(gameId)}")

    val token = System.getenv("DISCORD_BOT_TOKEN") ?: throw IllegalStateException("Bot token not provided")
    Launcher(token).launch()
}
