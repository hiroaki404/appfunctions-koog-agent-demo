package dev.hiroaki404.appfunctions.koogdemo.tripapp.data

import dev.hiroaki404.appfunctions.koogdemo.tripapp.Itinerary
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ItineraryDay
import dev.hiroaki404.appfunctions.koogdemo.tripapp.Spot

/** Demo data shared by the database creation callback and the debug actions on the screen. */
object TripSeed {
    /** The past trip to Hokkaido the demo's agent looks up to avoid places visited last year. */
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

    /** Older trips that fill the records section; none of them is in Hokkaido. */
    private val olderTrips = listOf(
        Itinerary(
            title = "2025年春の京都",
            days = listOf(
                ItineraryDay(
                    date = "2025-04-05",
                    spots = listOf(
                        Spot("清水寺", "東山にある寺。舞台から桜に囲まれた京都の街を眺めた。"),
                        Spot("伏見稲荷大社", "千本鳥居で知られる神社。夕方に鳥居のトンネルを歩いた。"),
                    ),
                ),
                ItineraryDay(
                    date = "2025-04-06",
                    spots = listOf(
                        Spot("嵐山", "竹林の小径と渡月橋を散策した。"),
                    ),
                ),
            ),
        ),
        Itinerary(
            title = "2024年秋の沖縄",
            days = listOf(
                ItineraryDay(
                    date = "2024-10-12",
                    spots = listOf(
                        Spot("首里城", "琉球王国の城。復元工事の様子を見学した。"),
                    ),
                ),
                ItineraryDay(
                    date = "2024-10-13",
                    spots = listOf(
                        Spot("美ら海水族館", "ジンベエザメが泳ぐ大水槽を見た。"),
                    ),
                ),
                ItineraryDay(
                    date = "2024-10-14",
                    spots = listOf(
                        Spot("古宇利島", "橋を渡って行く島。エメラルドグリーンの海を眺めた。"),
                    ),
                ),
            ),
        ),
    )

    /** The past trips every fresh database starts with, newest first; [pastTrip] is the first. */
    val pastTrips: List<Itinerary> = listOf(pastTrip) + olderTrips

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
