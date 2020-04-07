package net.phedny.decryptobot

fun main() {
    val token = System.getenv("DISCORD_BOT_TOKEN") ?: throw IllegalStateException("Bot token not provided")
    Launcher(token).launch()
}
