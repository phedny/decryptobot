package net.phedny.decryptobot.command.lobby

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.phedny.decryptobot.SheetsClient
import net.phedny.decryptobot.command.Command
import net.phedny.decryptobot.extensions.send
import net.phedny.decryptobot.state.GameRepository
import net.phedny.decryptobot.state.LobbyRepository
import net.phedny.decryptobot.state.Team
import net.phedny.decryptobot.state.Words

class ContinueCommand() : Command {
    override fun execute(event: GuildMessageReceivedEvent, prefix:String) {
        if (LobbyRepository.getLobby(event.guild.id, event.channel.id) == null) {
            LobbyRepository.newLobby(event.guild.id, event.channel.id)
        }
        val lobby = LobbyRepository.getLobby(event.guild.id, event.channel.id)!!

        val spreadsheetId = Regex("docs.google.com/spreadsheets/d/(\\w+)").find(event.message.contentRaw)?.groupValues?.get(1)
        if (spreadsheetId?.isNotEmpty() == true) {
            val game = SheetsClient.readGameInfo(spreadsheetId)
        }

    }

    private fun updateChannelMessage(event: GuildMessageReceivedEvent, channelMessageId: String, channelMessagePrefix: String) {
        event.channel.editMessageById(channelMessageId, channelMessagePrefix +
                "Everybody should have received a link to the Google spreadsheet and the secret words.\n" +
                "If you're not sure to play the game on Discord, you can send me the `!help` command and I'll help you out. Enjoy your game!")
            .queue()
    }
}