package net.phedny.decryptobot.state

data class Lobby(val guildId: String, val channelId: String, val whitePlayers: List<String>, val blackPlayers: List<String>)

object LobbyRepository {
    val lobbies: MutableList<Lobby> = mutableListOf()

    fun getLobby(guildId: String, channelId: String): Lobby? = lobbies.find { it.guildId == guildId && it.channelId == channelId }

    fun updateLobby(guildId: String, channelId: String, whitePlayers: List<String>? = null, blackPlayers: List<String>? = null): Boolean {
        val lobbyIndex = lobbies.indexOfFirst { it.guildId == guildId && it.channelId == channelId }
        if (lobbyIndex == -1) {
            return false
        }

        val existingLobby = lobbies[lobbyIndex]
        val newLobby = existingLobby.copy(
            whitePlayers = whitePlayers ?: existingLobby.whitePlayers,
            blackPlayers = blackPlayers ?: existingLobby.blackPlayers
        )
        lobbies[lobbyIndex] = newLobby

        return true
    }

    fun removeLobby(guildId: String, channelId: String) {
        lobbies.removeIf { it.guildId == guildId && it.channelId == channelId }
    }

    fun newLobby(guildId: String, channelId: String) {
        val existingLobby = lobbies.find { it.guildId == guildId && it.channelId == channelId }
        if (existingLobby == null) {
            lobbies.add(Lobby(guildId, channelId, emptyList(), emptyList()))
        } else {
            updateLobby(guildId, channelId, emptyList(), emptyList())
        }
    }

}
