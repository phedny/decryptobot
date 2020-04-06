package net.phedny.decryptobot.extensions

import net.dv8tion.jda.api.entities.MessageChannel

fun MessageChannel.send(msg:String) = sendMessage("$msg").queue()