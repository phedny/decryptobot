package net.phedny.decryptobot.extensions

import net.dv8tion.jda.api.entities.User

fun User.send(msg:String) = openPrivateChannel().map { it.sendMessage(msg).queue() }.queue()