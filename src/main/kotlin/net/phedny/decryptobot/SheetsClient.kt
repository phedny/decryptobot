package net.phedny.decryptobot

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import net.phedny.decryptobot.state.Game
import net.phedny.decryptobot.state.Round
import net.phedny.decryptobot.state.Team
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader

object SheetsClient {

    private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    private val JSON_FACTORY = JacksonFactory.getDefaultInstance()
    private const val APPLICATION_NAME = "Decrypto Bot"

    /**
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private val SCOPES: List<String> = listOf(DriveScopes.DRIVE_FILE, SheetsScopes.SPREADSHEETS)
    private val SECRET_WORD_REGEX = """\uD83D\uDD11 \d -- """.toRegex()
    private const val CREDENTIALS_FILE_PATH = "/credentials.json"
    private const val TOKENS_DIRECTORY_PATH = "tokens"
    private const val TEMPLATE_SPREADSHEET_ID = "1KXP68tPMVIf_Il0RLe55R4xuPmfdfl0B_VUIJ71q4wg"

    private val credentials = run {
        // Load client secrets.
        val `in`: InputStream = SheetsClient::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
            ?: throw FileNotFoundException("Resource not found: $CREDENTIALS_FILE_PATH")
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

        // Build flow and trigger user authorization request.
        val flow = GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()
        val receiver: LocalServerReceiver = LocalServerReceiver.Builder().setPort(8888).build()
        AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    private val driveService = Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
        .setApplicationName(APPLICATION_NAME)
        .build()

    private val sheetsService = Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
        .setApplicationName(APPLICATION_NAME)
        .build()

    fun checkTemplateExistence() {
        if (!spreadsheetExists(TEMPLATE_SPREADSHEET_ID)) {
            throw IllegalStateException("Template spreadsheet is missing.")
        }
    }

    private fun spreadsheetExists(spreadsheetId: String) : Boolean {
        return try {
            sheetsService.spreadsheets().get(spreadsheetId).execute()
            true
        } catch (e: GoogleJsonResponseException) {
            false
        }
    }

    fun initializeNewSpreadsheet(): String {
        val spreadsheet = Spreadsheet()
        spreadsheet.properties = SpreadsheetProperties().setTitle("Decrypto game")
        val spreadsheetId = sheetsService.spreadsheets().create(spreadsheet)
            .setFields("spreadsheetId")
            .execute()
            .spreadsheetId

        val copyRequest = CopySheetToAnotherSpreadsheetRequest()
        copyRequest.destinationSpreadsheetId = spreadsheetId
        val copyResult = sheetsService.spreadsheets().sheets().copyTo(TEMPLATE_SPREADSHEET_ID, 0, copyRequest).execute()
        println("Copy request: $copyResult")


        val batchUpdateRequest = BatchUpdateSpreadsheetRequest()
        batchUpdateRequest.requests = listOf(
            Request().setDeleteSheet(DeleteSheetRequest().setSheetId(0)),
            Request().setUpdateSheetProperties(UpdateSheetPropertiesRequest().setProperties(SheetProperties().setSheetId(copyResult.sheetId).setTitle("Decrypto game")).setFields("title")),
            Request().setAddProtectedRange(AddProtectedRangeRequest().setProtectedRange(ProtectedRange().setDescription("HiddenData").setRange(GridRange().setSheetId(copyResult.sheetId).setStartRowIndex(39).setEndRowIndex(139)).setEditors(Editors().setUsers(listOf("decryptobot@gmail.com"))))),
            Request().setUpdateDimensionProperties(UpdateDimensionPropertiesRequest().setRange(DimensionRange().setSheetId(copyResult.sheetId).setDimension("ROWS").setStartIndex(39).setEndIndex(139)).setProperties(DimensionProperties().setHiddenByUser(true)).setFields("hiddenByUser"))
        )
        println("Batch request: " + sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute())

        println("Make public request: " + driveService.Permissions().create(spreadsheetId, Permission().setType("anyone").setRole("writer")).execute())

        return spreadsheetId
    }

    private const val GENERAL_INFO_RANGE = "B70:B73"
    private const val PLAYER_RANGE = "G70:G139"
    private const val ENCRYPTOR_RANGE = "B74:C81"

    private fun getSecretWordsRanges(color: String): Pair<String, String> {
        return when (color) {
            "BLACK" -> Pair("M22:N22", "R22:S22")
            "WHITE" -> Pair("B22:C22", "G22:H22")
            else -> throw IllegalAccessException("Unknown color: $color")
        }
    }

    private fun getEncryptorRange(round: Int): String {
        if (round !in 1..8)
            throw IllegalArgumentException("Roundnumber must be between 1 and 8")
        return "B${73+round}:C${73+round}"
    }

    private fun getRoundDataRange(color: String, round: Int): String {
        return when (Pair(color, round)) {
            Pair("BLACK", 1) -> "M3:P5"
            Pair("WHITE", 1) -> "B3:E5"
            Pair("BLACK", 2) -> "M8:P10"
            Pair("WHITE", 2) -> "B8:E10"
            Pair("BLACK", 3) -> "M13:P15"
            Pair("WHITE", 3) -> "B13:E15"
            Pair("BLACK", 4) -> "M18:P20"
            Pair("WHITE", 4) -> "B18:E20"
            Pair("BLACK", 5) -> "R3:U5"
            Pair("WHITE", 5) -> "G3:J5"
            Pair("BLACK", 6) -> "R8:U10"
            Pair("WHITE", 6) -> "G8:J10"
            Pair("BLACK", 7) -> "R13:U15"
            Pair("WHITE", 7) -> "G13:J15"
            Pair("BLACK", 8) -> "R18:U20"
            Pair("WHITE", 8) -> "G18:J20"
            else -> throw IllegalArgumentException("No such color or round")
        }
    }

    fun writeGameInfo(spreadsheetId: String, opponentSpreadsheetId: String, guildId: String, channelId: String, color: String, players: List<String>, secretWords: List<String>) {
        val (codewordsRange1, codewordsRange2) = getSecretWordsRanges(color)

        val data = listOf(
            ValueRange()
                .setRange(GENERAL_INFO_RANGE)
                .setValues(listOf(listOf(opponentSpreadsheetId), listOf(guildId), listOf(channelId), listOf(color))),
            ValueRange()
                .setRange(PLAYER_RANGE)
                .setValues(players.map { listOf(it) }),
            ValueRange()
                .setRange(codewordsRange1)
                .setValues(listOf(secretWords.take(2).mapIndexed { i, s -> "\uD83D\uDD11 ${i + 1} -- $s" })),
            ValueRange()
                .setRange(codewordsRange2)
                .setValues(listOf(secretWords.drop(2).mapIndexed { i, s -> "\uD83D\uDD11 ${i + 3} -- $s" }))
        )

        val request = BatchUpdateValuesRequest()
            .setValueInputOption("RAW")
            .setData(data)

        println("Write game info: " + sheetsService.spreadsheets().values().batchUpdate(spreadsheetId, request).execute())
    }

    fun writeEncryptorData(spreadsheetId: String, encryptor: String, answer: List<Int>, round: Int) {
        val data = listOf(listOf(encryptor, answer.joinToString(" ")))
        println("Write encryptor data: " + sheetsService.spreadsheets().values().update(spreadsheetId,
            getEncryptorRange(round), ValueRange().setValues(data)).setValueInputOption("RAW").execute())
    }

    fun writeRound(game: Game, round: Int) {
        val blackRound = game.black.rounds[round - 1]
        val whiteRound = game.white.rounds[round - 1]

        fun guess(guess: List<Int?>): List<Int>? = guess.filterNotNull().let { if (it.size == 3) it else null }
        fun answer(answer: List<Int>): List<Int>? = if (blackRound.finished && whiteRound.finished) answer else null

        writeRound(game.black.spreadsheetId, round, blackRound.hints, whiteRound.hints, guess(blackRound.teamGuess), guess(whiteRound.opponentGuess), answer(blackRound.answer), answer(whiteRound.answer))
        writeRound(game.white.spreadsheetId, round, blackRound.hints, whiteRound.hints, guess(blackRound.opponentGuess), guess(whiteRound.teamGuess), answer(blackRound.answer), answer(whiteRound.answer))
    }

    fun writeRound(spreadsheetId: String, round: Int, blackHints: List<String?>, whiteHints: List<String?>, blackGuess: List<Int>?, whiteGuess: List<Int>?, blackAnswer: List<Int>?, whiteAnswer: List<Int>?) {
        val blackValues = listOf(
            blackHints,
            emptyList(),
            blackGuess ?: emptyList(),
            blackAnswer ?: emptyList()
        )
        val blackData = ValueRange().setRange(getRoundDataRange("BLACK", round)).setValues(blackValues).setMajorDimension("COLUMNS")

        val whiteValues = listOf(
            whiteHints,
            emptyList(),
            whiteGuess ?: emptyList(),
            whiteAnswer ?: emptyList()
        )
        val whiteData = ValueRange().setRange(getRoundDataRange("WHITE", round)).setValues(whiteValues).setMajorDimension("COLUMNS")

        val request = BatchUpdateValuesRequest()
            .setValueInputOption("RAW")
            .setData(listOf(blackData, whiteData))

        println("Write round data: " + sheetsService.spreadsheets().values().batchUpdate(spreadsheetId, request).execute())
    }

    fun readGameInfo(spreadsheetId: String): Game? {
        if (!spreadsheetExists(spreadsheetId)) {
            return null
        }

        val ranges = mutableListOf(GENERAL_INFO_RANGE, PLAYER_RANGE, ENCRYPTOR_RANGE)
        ranges.addAll(getSecretWordsRanges("WHITE").toList())
        ranges.addAll(getSecretWordsRanges("BLACK").toList())
        for (i in 1..8) {
            ranges.add(getRoundDataRange("WHITE", i))
            ranges.add(getRoundDataRange("BLACK", i))
        }
        val valueRanges = readRanges(spreadsheetId, ranges)

        val (opponentSpreadsheetId, guildId, channelId, color) = valueRanges.getByRange(GENERAL_INFO_RANGE)?.map { it.first().toString() } ?: throw IllegalStateException("Something is wrong with the game information in the provided spreadsheet")
        if (!spreadsheetExists(opponentSpreadsheetId)) {
            return null
        }
        val opponentValueRanges = readRanges(opponentSpreadsheetId, ranges)

        val (black, white) = when (color) {
            "BLACK" -> Pair(readTeam(valueRanges, opponentValueRanges, spreadsheetId, "BLACK"), readTeam(opponentValueRanges, valueRanges, opponentSpreadsheetId, "WHITE"))
            "WHITE" -> Pair(readTeam(opponentValueRanges, valueRanges, opponentSpreadsheetId, "BLACK"), readTeam(valueRanges, opponentValueRanges, spreadsheetId, "WHITE"))
            else -> throw IllegalStateException("Unknown color: $color")
        }

        return if (black == null || white == null) null else Game(guildId, channelId, black, white)
    }

    private fun readTeam(valueRanges: List<ValueRange>, opponentValueRanges: List<ValueRange>, spreadsheetId: String, color: String): Team? {
        val (secretWordsRange1, secretWordsRange2) = getSecretWordsRanges(color)
        val secretWords1 = valueRanges.getByRange(secretWordsRange1) ?: throw IllegalStateException("Something is wrong with the game information in the provided spreadsheet")
        val secretWords2 = valueRanges.getByRange(secretWordsRange2) ?: throw IllegalStateException("Something is wrong with the game information in the provided spreadsheet")
        val secretWords = secretWords1.plus(secretWords2).flatMap { values -> values.map { it.toString().replaceFirst(SECRET_WORD_REGEX, "") } }
        println("secretWords: $secretWords")

        val playersResult = valueRanges.getByRange(PLAYER_RANGE) ?: throw IllegalStateException("Something is wrong with the game information in the provided spreadsheet")
        val players = playersResult.map { it.first().toString() }
        println("players: $players")

        val rounds = readRounds(valueRanges, opponentValueRanges, color)

        return Team(spreadsheetId, secretWords, players, rounds)
    }

    private fun readRounds(valueRanges: List<ValueRange>, opponentValueRanges: List<ValueRange>, color: String): List<Round> =
        valueRanges.getByRange(ENCRYPTOR_RANGE)?.mapIndexed { index, encryptorDataRound ->
            val i = index + 1
            val encryptor = encryptorDataRound[0].toString()
            val answer = encryptorDataRound[1].toString().split(" ").map { it.toInt() }
            println("encryptor round $i: $encryptor")
            println("answer round $i: $answer")

            val roundData = valueRanges.getByRange(getRoundDataRange(color, i))
            val hints = roundData?.map { it[0]?.toString() } ?: listOf(null, null, null)
            val teamGuess = roundData?.map { it.getOrNull(2)?.toString()?.toInt() } ?: listOf(null, null, null)
            val opponentGuess = opponentValueRanges.getByRange(getRoundDataRange(color, i))?.map { it.getOrNull(2)?.toString()?.toIntOrNull() } ?: listOf(null, null, null)
            println("hints $i: $hints")
            println("team guess $i: $teamGuess")
            println("opponent guess $i: $opponentGuess")
            Round(index == 0, answer, encryptor, hints, teamGuess, opponentGuess)
        } ?: emptyList()

    fun readHints(spreadsheetId: String, color: String, round: Int): List<String> = readRange(spreadsheetId, getRoundDataRange(color, round)).map { it.first().toString() }

    fun readGuesses(spreadsheetId: String, round: Int): Pair<List<Int>?, List<Int>?> {
        val values = readRanges(spreadsheetId, listOf(getRoundDataRange("BLACK", round), getRoundDataRange("WHITE", round)))
        val blackGuess = values.getByRange(getRoundDataRange("BLACK", round))?.mapNotNull { it.getOrNull(2)?.toString()?.toInt() }.let { if (it?.size == 3) it else null }
        val whiteGuess = values.getByRange(getRoundDataRange("WHITE", round))?.mapNotNull { it.getOrNull(2)?.toString()?.toInt() }.let { if (it?.size == 3) it else null }
        return Pair(blackGuess, whiteGuess)
    }

    private fun readRange(spreadsheetId: String, range: String) = sheetsService.spreadsheets().values().get(spreadsheetId, range).setValueRenderOption("FORMULA").execute().getValues()

    private fun readRanges(spreadsheetId: String, ranges: List<String>) = sheetsService.spreadsheets().values().batchGet(spreadsheetId).setRanges(ranges).setValueRenderOption("FORMULA").execute().valueRanges

}

private fun List<ValueRange>.getByRange(range: String) = this.find { it.range == "'Decrypto game'!$range" }?.getValues()
