package net.phedny.decryptobot.extensions

import net.dv8tion.jda.api.entities.Member

fun Member.send(msg:String) = user.send(msg)