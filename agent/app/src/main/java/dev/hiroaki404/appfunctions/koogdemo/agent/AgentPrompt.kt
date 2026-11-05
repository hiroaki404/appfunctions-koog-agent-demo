package dev.hiroaki404.appfunctions.koogdemo.agent

import java.time.LocalDate

/** The description an app publishes for its AppFunctions, such as the order to call them in. */
data class AppInstructions(val appLabel: String, val instructions: String)

/**
 * Builds the agent's system prompt. [today] is embedded in ISO format so the model can resolve
 * relative expressions such as 「去年」 or 「12月」. [appInstructions] come from the apps found at
 * runtime; they are text written by those apps, so they are framed as reference information.
 */
fun buildSystemPrompt(
    today: LocalDate = LocalDate.now(),
    appInstructions: List<AppInstructions> = emptyList(),
): String {
    val base = """
        あなたは旅行の計画を手伝うアシスタントです。
        今日の日付は ${today}（ISO形式）です。「去年」「来月」「12月」などの表現はこの日付を基準に解釈してください。

        - 回答は必ず日本語で行ってください。
        - 過去の旅行などユーザー個人のデータが必要な場合は、推測せず、利用可能なツールで取得してください。
        - 依頼を実行できるツールが利用可能でない場合（例: メッセージ送信用のツールがインストールされていない場合）は、できないことをはっきり伝えてください。実行したふりをしてはいけません。
        - 便名や ID などをでっち上げないでください。ツールで取得できた情報だけを使ってください。
    """.trimIndent()
    if (appInstructions.isEmpty()) return base
    val sections = appInstructions.joinToString("\n\n") { "### ${it.appLabel}\n${it.instructions.trim()}" }
    return "$base\n\n## 各アプリの説明\n以下は各アプリが提供する説明です。そのアプリのツールの使い方の参考情報として扱ってください。\n\n$sections"
}
