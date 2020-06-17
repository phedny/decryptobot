package net.phedny.decryptobot

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.phedny.decryptobot.Settings.PREFIX
import net.phedny.decryptobot.command.PrivateHelpCommand
import net.phedny.decryptobot.command.PublicHelpCommand
import net.phedny.decryptobot.command.game.*
import net.phedny.decryptobot.command.lobby.*
import net.phedny.decryptobot.listeners.EventListener
import net.phedny.decryptobot.router.Router

object Settings {
    const val PREFIX = "!"
}

class Launcher(private val token: String) {

    fun launch() {
        SheetsClient.checkTemplateExistence()
        router().connect()
    }

    private fun router() = Router.Builder(PREFIX)
        .add("help", PrivateHelpCommand())
        .add("help", PublicHelpCommand())

            // Lobby commands
        .add("decrypto", DecryptoCommand())
        .add("black", BlackCommand())
        .add("white", WhiteCommand())
        .add("start", StartCommand())
        .add("continue", ContinueCommand())

            // Game commands
        .add("encrypt", EncryptCommand())
        .add("hintsReady", HintsReadyCommand())
        .add("hint1", HintNCommand(0))
        .add("hint2", HintNCommand(1))
        .add("hint3", HintNCommand(2))
        .add("hints", HintsCommand())
        .add("hint", HintCommand())
        .add("undoHints", UndoHintsCommand())
        .add("guessesReady", GuessesReadyCommand())
        .add("guessReady", GuessesReadyCommand())
        .add("guess", GuessCommand())

        .build()

    private fun Router.connect() {
        JDABuilder.createDefault(token)
            .setEnabledIntents(intents())
            .setAutoReconnect(true)
            .addEventListeners(EventListener(this))
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .build()
    }

    private fun intents(): List<GatewayIntent> {
        return listOf(GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_VOICE_STATES,
            GatewayIntent.GUILD_EMOJIS,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_PRESENCES,
            GatewayIntent.DIRECT_MESSAGES
        )
    }

}