package dev.hiroaki404.appfunctions.koogdemo.kototabi.data

import dev.hiroaki404.appfunctions.koogdemo.kototabi.Itinerary
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ItineraryDay
import dev.hiroaki404.appfunctions.koogdemo.kototabi.Spot

/** Demo data shared by the database creation callback and the debug actions on the screen. */
object KototabiSeed {
    /** The past trip every fresh database starts with. */
    val pastTrip = Itinerary(
        title = "2025年夏の北海道",
        days = listOf(
            ItineraryDay(
                date = "2025-08-10",
                spots = listOf(
                    Spot("時計台", "札幌のシンボル。明治時代に建てられた時計塔を見学した。"),
                    Spot("大通公園", "札幌の中心部にある公園。とうもろこしを食べながら散策した。"),
                ),
            ),
            ItineraryDay(
                date = "2025-08-11",
                spots = listOf(
                    Spot("ラベンダー畑", "富良野の花畑。ラベンダーが見頃で、ソフトクリームも楽しんだ。"),
                ),
            ),
        ),
    )

    /** A sample of the trip the agent plans in the demo: December in Hokkaido. */
    val upcomingTrip = Itinerary(
        title = "2026年12月の北海道",
        days = listOf(
            ItineraryDay(
                date = "2026-12-20",
                spots = listOf(
                    Spot("さっぽろホワイトイルミネーション", "札幌の大通公園などを光で彩る冬の風物詩。"),
                    Spot("札幌ラーメン横丁", "すすきのにあるラーメン店街。味噌ラーメンで体を温める。"),
                ),
            ),
            ItineraryDay(
                date = "2026-12-21",
                spots = listOf(
                    Spot("小樽運河", "ガス灯と石造り倉庫が並ぶ運河。冬は雪景色の中の夜景が美しい。"),
                ),
            ),
            ItineraryDay(
                date = "2026-12-22",
                spots = listOf(
                    Spot("旭山動物園", "旭川にある人気の動物園。冬はペンギンの散歩が見どころ。"),
                ),
            ),
        ),
    )
}
