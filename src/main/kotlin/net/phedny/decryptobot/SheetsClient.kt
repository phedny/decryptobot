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
import net.phedny.decryptobot.state.Team
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader

object SheetsClient {

    private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    private val JSON_FACTORY = JacksonFactory.getDefaultInstance()
    private val APPLICATION_NAME = "Decrypto Bot"

    /**
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private val SCOPES: List<String> = listOf(DriveScopes.DRIVE_FILE, SheetsScopes.SPREADSHEETS)
    private val CREDENTIALS_FILE_PATH = "/credentials.json"
    private val TOKENS_DIRECTORY_PATH = "tokens"
    private val TEMPLATE_SPREADSHEET_ID = "1KXP68tPMVIf_Il0RLe55R4xuPmfdfl0B_VUIJ71q4wg"

    private val GENERAL_INFO_RANGE = "B70:B73"
    private val PLAYER_RANGE = "G70:G139"
    val SECRETWORDS1_BLACK_RANGE = "M22:N22"
    val SECRETWORDS2_BLACK_RANGE = "R22:S22"
    val SECRETWORDS1_WHITE_RANGE = "B22:C22"
    val SECRETWORDS2_WHITE_RANGE = "G22:H22"

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
        println("Copy request: " + copyResult)


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

    private fun getSecretWordsRanges(color: String): Pair<String, String> {
        return when (color) {
            "BLACK" -> Pair(SECRETWORDS1_BLACK_RANGE, SECRETWORDS2_BLACK_RANGE)
            "WHITE" -> Pair(SECRETWORDS1_WHITE_RANGE, SECRETWORDS2_WHITE_RANGE)
            else -> throw IllegalAccessException("Unknown color: $color")
        }
    }

    fun writeEncryptorData(spreadsheetId: String, encryptor: String, answer: List<Int>) {
        val data = listOf(listOf(encryptor), listOf(answer.joinToString(" ")))
        println("Write encryptor data: " + sheetsService.spreadsheets().values().update(spreadsheetId, "B74", ValueRange().setValues(data)).setValueInputOption("RAW").execute())
    }

    fun writeHints(spreadsheetId: String, color: String, round: Int, hints: List<String?>) {
        println("Write hints: " + sheetsService.spreadsheets().values().update(spreadsheetId, getHintRange(color, round), ValueRange().setValues(hints.map { listOf(it) })).setValueInputOption("RAW").execute())
    }

    fun readGeneralInfo(spreadsheetId: String): List<String> {
        val result = sheetsService.spreadsheets().values().get(spreadsheetId, GENERAL_INFO_RANGE).setValueRenderOption("FORMULA").execute()
        println("gameinfo: " + result.getValues().map { it.first().toString() })
        return result.getValues().map { it.first().toString() }
    }

    fun readTeam(spreadsheetId: String, color: String): Team? {
        val (secretwordsRange1, secretwordsRange2) = getSecretWordsRanges(color)
        val secretWords1 = sheetsService.spreadsheets().values().get(spreadsheetId, secretwordsRange1).setValueRenderOption("FORMULA").execute()
        val secretWords2 = sheetsService.spreadsheets().values().get(spreadsheetId, secretwordsRange2).setValueRenderOption("FORMULA").execute()
        println("secretWords: " + secretWords1.getValues().plus(secretWords2.getValues()).flatMap { values -> values.map { it.toString().replaceFirst(Regex("\uD83D\uDD11 \\d -- "),"") } })
        val players = sheetsService.spreadsheets().values().get(spreadsheetId, PLAYER_RANGE).setValueRenderOption("FORMULA").execute()
        println("players: " + players.getValues().map { it.first().toString() })
        val rounds = null
        return null
    }

    fun readHints(spreadsheetId: String, color: String, round: Int): List<String> {
        val result = sheetsService.spreadsheets().values().get(spreadsheetId, getHintRange(color, round)).setValueRenderOption("FORMULA").execute()
        return result.getValues().map { it.first().toString() }
    }

    private fun getHintRange(color: String, round: Int): String {
        return when (Pair(color, round)) {
            Pair("BLACK", 1) -> "M3:M5"
            Pair("WHITE", 1) -> "B3:B5"
            Pair("BLACK", 2) -> "M8:M10"
            Pair("WHITE", 2) -> "B8:B10"
            Pair("BLACK", 3) -> "M13:M15"
            Pair("WHITE", 3) -> "B13:B15"
            Pair("BLACK", 4) -> "M18:M20"
            Pair("WHITE", 4) -> "B18:B20"
            Pair("BLACK", 5) -> "R3:R5"
            Pair("WHITE", 5) -> "G3:G5"
            Pair("BLACK", 6) -> "R8:R10"
            Pair("WHITE", 6) -> "G8:G10"
            Pair("BLACK", 7) -> "R13:R15"
            Pair("WHITE", 7) -> "G13:G15"
            Pair("BLACK", 8) -> "R18:R20"
            Pair("WHITE", 8) -> "G18:G20"
            else -> throw IllegalArgumentException("No such color or round")
        }
    }

    fun checkTemplateExistence() {
        try {
            sheetsService.spreadsheets().get(TEMPLATE_SPREADSHEET_ID).execute()
        } catch (e: GoogleJsonResponseException){
            throw IllegalStateException("Template spreadsheet is missing.", e)
        }
    }

    fun readGameInfo(spreadsheetId: String): Game? {
        try {
            sheetsService.spreadsheets().get(spreadsheetId).execute()
        } catch (e: GoogleJsonResponseException){
            return null
        }
        val (opponentSpreadsheetId, guildId, channelId, color) = readGeneralInfo(spreadsheetId)
        val white = (if (color == "WHITE") readTeam(spreadsheetId, color) else readTeam(opponentSpreadsheetId, color)) ?: return null
        val black = (if (color == "BLACK") readTeam(spreadsheetId, color) else readTeam(opponentSpreadsheetId, color)) ?: return null
        val game = Game(guildId, channelId, black, white)
        return null
    }

}