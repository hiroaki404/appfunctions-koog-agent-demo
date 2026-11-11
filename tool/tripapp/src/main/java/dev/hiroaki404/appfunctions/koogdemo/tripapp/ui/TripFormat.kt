package dev.hiroaki404.appfunctions.koogdemo.tripapp.ui

import dev.hiroaki404.appfunctions.koogdemo.tripapp.Itinerary
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

// Text for the trip screen. Dates arrive as `yyyy-MM-dd` strings written by the AI, so anything
// that does not parse is shown as it is instead of failing.

private val weekdays = listOf("月", "火", "水", "木", "金", "土", "日")

fun parseDate(date: String): LocalDate? = try {
    LocalDate.parse(date)
} catch (_: DateTimeParseException) {
    null
}

/** The Japanese one-letter day of the week, e.g. 「土」. */
fun weekdayOf(date: LocalDate): String = weekdays[date.dayOfWeek.value - 1]

/** 「12/5 土」 for a timeline day; the raw text if it is not a date. */
fun dayLabel(date: String): String =
    parseDate(date)?.let { "${it.monthValue}/${it.dayOfMonth} ${weekdayOf(it)}" } ?: date

private fun monthDayWithWeekday(date: LocalDate): String =
    "${date.monthValue}/${date.dayOfMonth}（${weekdayOf(date)}）"

/** 「12/5（土）〜 12/7（月）・2泊3日」, or 「12/5（土）・日帰り」 for a one-day trip. */
fun tripPeriodText(itinerary: Itinerary): String {
    val first = itinerary.days.firstOrNull()?.date ?: return ""
    val last = itinerary.days.last().date
    val start = parseDate(first)
    val end = parseDate(last)
    if (start == null || end == null) return if (first == last) first else "$first 〜 $last"
    val nights = ChronoUnit.DAYS.between(start, end).toInt()
    return if (nights <= 0) {
        "${monthDayWithWeekday(start)}・日帰り"
    } else {
        // The full-width parenthesis already leaves room before 「〜」.
        "${monthDayWithWeekday(start)}〜 ${monthDayWithWeekday(end)}・${nights}泊${nights + 1}日"
    }
}

/** Days from [today] to the first day of the trip; 0 once it has started; null without a date. */
fun daysUntil(itinerary: Itinerary, today: LocalDate): Int? {
    val start = itinerary.days.firstOrNull()?.date?.let(::parseDate) ?: return null
    return ChronoUnit.DAYS.between(today, start).toInt().coerceAtLeast(0)
}

/** 「2025/8/10–11」, 「2025/7/31–8/1」 or 「2024/12/31–2025/1/1」 for a past trip. */
fun recordDateText(itinerary: Itinerary): String {
    val first = itinerary.days.firstOrNull()?.date ?: return ""
    val last = itinerary.days.last().date
    val start = parseDate(first)
    val end = parseDate(last)
    if (start == null || end == null) return if (first == last) first else "$first–$last"
    val from = "${start.year}/${start.monthValue}/${start.dayOfMonth}"
    return when {
        start == end -> from
        start.year != end.year -> "$from–${end.year}/${end.monthValue}/${end.dayOfMonth}"
        start.monthValue != end.monthValue -> "$from–${end.monthValue}/${end.dayOfMonth}"
        else -> "$from–${end.dayOfMonth}"
    }
}

/** The trip's spot names in visiting order, joined with 「・」. */
fun spotsText(itinerary: Itinerary): String =
    itinerary.days.flatMap { it.spots }.joinToString("・") { it.name }
