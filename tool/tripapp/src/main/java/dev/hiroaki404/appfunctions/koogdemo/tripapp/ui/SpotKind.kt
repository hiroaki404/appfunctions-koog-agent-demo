package dev.hiroaki404.appfunctions.koogdemo.tripapp.ui

import dev.hiroaki404.appfunctions.koogdemo.tripapp.Spot

/** What kind of place a spot is, which picks its icon and color on the itinerary card. */
enum class SpotKind {
    HotSpring,
    Animals,
    Water,
    Lights,
    Food,
    Landmark,
    Other,
}

/**
 * Words that mark each kind, checked from the top: a spot whose name has both 「水族館」 and 「海」 is
 * [SpotKind.Animals]. The AI writes the spot names, so this looks for words inside them rather than
 * matching whole names.
 */
private val kindWords: List<Pair<SpotKind, List<String>>> = listOf(
    SpotKind.HotSpring to listOf("温泉", "湯"),
    SpotKind.Animals to listOf("動物園", "水族館"),
    SpotKind.Water to listOf("運河", "海", "湖", "川", "島"),
    SpotKind.Lights to listOf("イルミネーション", "夜景", "祭"),
    SpotKind.Food to listOf("ラーメン", "横丁", "市場", "グルメ"),
    SpotKind.Landmark to listOf("寺", "神社", "大社", "稲荷", "城", "時計台"),
)

/** Decides the kind from the spot's name, then from its description; [SpotKind.Other] if neither tells. */
fun spotKindOf(spot: Spot): SpotKind =
    kindIn(spot.name) ?: kindIn(spot.description) ?: SpotKind.Other

private fun kindIn(text: String): SpotKind? =
    kindWords.firstOrNull { (_, words) -> words.any { it in text } }?.first
