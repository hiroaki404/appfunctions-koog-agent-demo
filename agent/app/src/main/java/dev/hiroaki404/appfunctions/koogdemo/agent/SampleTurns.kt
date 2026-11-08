package dev.hiroaki404.appfunctions.koogdemo.agent

// Fixed conversation for checking the chat UI without calling the LLM. Loaded only from the debug-build
// long press on the title (see AgentChatScreen); the package names and display names stand in for what
// the real run reads from the provider apps.
private const val TRIP_PACKAGE = "dev.hiroaki404.appfunctions.koogdemo.tripapp"
private const val MESSAGE_PACKAGE = "dev.hiroaki404.appfunctions.koogdemo.message"

private fun tripCall(name: String, displayName: String, arguments: String, status: ToolCallStatus, result: String? = null) =
    ToolCallUiModel(
        null,
        name,
        arguments,
        status,
        result = result,
        appLabel = "旅行",
        packageName = TRIP_PACKAGE,
        displayName = displayName,
    )

/** One turn per state the chat screen can show: answered, a failed tool, an error, and still running. */
val SampleTurns: List<ChatTurn> = listOf(
    ChatTurn(
        id = -4,
        prompt = "12月に北海道へ行きたい。おすすめを調べて旅程を保存して",
        toolCalls = ToolCallUiState(
            listOf(
                tripCall(
                    "getTravelInfo",
                    "観光スポットの調査",
                    """{"area":"北海道","month":12}""",
                    ToolCallStatus.Succeeded,
                    result = """"[{"name":"さっぽろホワイトイルミネーション","description":"札幌の大通公園などを光で彩る冬の風物詩。"},{"name":"小樽運河","description":"ガス灯と石造り倉庫が並ぶ運河。"},{"name":"旭山動物園","description":"冬はペンギンの散歩が見どころ。"},{"name":"定山渓温泉","description":"札幌近郊の温泉地。"}]"""",
                ),
                tripCall(
                    "saveItinerary",
                    "旅程の保存",
                    """{"itinerary":{"title":"12月の北海道","days":[{"date":"2026-12-12","spots":[{"name":"さっぽろホワイトイルミネーション"}]},{"date":"2026-12-13","spots":[{"name":"小樽運河"}]}]}}""",
                    ToolCallStatus.Succeeded,
                    result = "\"「12月の北海道」を保存しました\"",
                ),
            ),
        ),
        response = """
            12月の北海道の旅程を保存しました。

            ## 12月の北海道
            1. **2026-12-12**: さっぽろホワイトイルミネーション
            2. **2026-12-13**: 小樽運河

            ### ほかのおすすめ
            * **旭山動物園**: 冬はペンギンの散歩が見どころ。
            * **定山渓温泉**: 札幌から日帰りで行ける温泉地。
        """.trimIndent(),
        isRunning = false,
    ),
    ChatTurn(
        id = -3,
        prompt = "トムくんに旅程を送って",
        toolCalls = ToolCallUiState(
            listOf(
                ToolCallUiModel(
                    null,
                    "sendMessage",
                    """{"recipient":"トムくん","body":"12月の北海道の旅程です。"}""",
                    ToolCallStatus.Failed,
                    error = "recipient must be one of: トム, ハナ",
                    appLabel = "メッセージ",
                    packageName = MESSAGE_PACKAGE,
                    displayName = "メッセージの送信",
                ),
                ToolCallUiModel(
                    null,
                    "sendMessage",
                    """{"recipient":"トム","body":"12月の北海道の旅程です。12/12 さっぽろホワイトイルミネーション、12/13 小樽運河。"}""",
                    ToolCallStatus.Succeeded,
                    result = "\"トムに送信しました\"",
                    appLabel = "メッセージ",
                    packageName = MESSAGE_PACKAGE,
                    displayName = "メッセージの送信",
                ),
            ),
        ),
        response = "トムさんに旅程を送りました。",
        isRunning = false,
    ),
    ChatTurn(
        id = -2,
        prompt = "東京から札幌の航空券も取って",
        error = "HTTP 429 RESOURCE_EXHAUSTED",
        isRunning = false,
    ),
    ChatTurn(
        id = -1,
        prompt = "過去の旅程も見せて",
        toolCalls = ToolCallUiState(listOf(tripCall("searchItineraries", "旅程の検索", "{}", ToolCallStatus.Running))),
        isRunning = true,
    ),
)
