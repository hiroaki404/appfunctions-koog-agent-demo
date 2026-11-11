package dev.hiroaki404.appfunctions.koogdemo.tripapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HotTub
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.TempleBuddhist
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hiroaki404.appfunctions.koogdemo.tripapp.FlightBooking
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ItineraryDay
import dev.hiroaki404.appfunctions.koogdemo.tripapp.Spot
import dev.hiroaki404.appfunctions.koogdemo.tripapp.TravelRules
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.SavedFlightBooking
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.SavedItinerary
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.TripSeed
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.Ink
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.Ink2
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.Leaf
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.LeafC
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.Line
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.Sea
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.SeaC
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.Sun
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.SunC
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.Surface2
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.SurfaceBg
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.theme.TripTheme
import java.time.LocalDate
import kotlin.random.Random

private val CardShape = RoundedCornerShape(20.dp)
private val RecordShape = RoundedCornerShape(14.dp)

/** Stateless so the layout stays independent of Room and AppFunctions. */
@Composable
fun TripScreen(
    state: TripUiState,
    onSeedUpcomingTrip: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AppBar(onSeedUpcomingTrip = onSeedUpcomingTrip, onReset = onReset) }
        if (state.upcoming.isEmpty() && state.past.isEmpty() && state.flightBookings.isEmpty()) {
            item { Text("旅程はまだありません", fontSize = 14.sp, color = Ink2) }
        }
        items(state.upcoming) { saved -> UpcomingTripCard(saved, today = state.today) }
        items(state.flightBookings) { saved -> BoardingPass(saved) }
        if (state.past.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("記録")
                    state.past.forEach { saved -> PastTripRow(saved) }
                }
            }
        }
    }
}

@Composable
private fun AppBar(onSeedUpcomingTrip: () -> Unit, onReset: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("旅行", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("旅のきろく", fontSize = 12.sp, color = Ink2)
        }
        // The debug actions live in the overflow menu so the projected screen stays clean.
        var menuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "メニュー", tint = Ink2)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("今回の旅程（12月の北海道）を入れる") },
                    onClick = {
                        menuExpanded = false
                        onSeedUpcomingTrip()
                    },
                )
                DropdownMenuItem(
                    text = { Text("初期状態に戻す") },
                    onClick = {
                        menuExpanded = false
                        onReset()
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 2.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = Ink2,
    )
}

/** The pill shown on anything the AI wrote through an AppFunction. */
@Composable
private fun AiChip(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        modifier = modifier
            .background(Sun, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 1.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
}

/** A thick orange frame on what the AI wrote, a thin line on the rest. */
private fun Modifier.aiFrame(addedByAi: Boolean): Modifier =
    if (addedByAi) border(2.dp, Sun, CardShape) else border(1.dp, Line, CardShape)

// region Upcoming trip

/** A trip that has not ended: a blue header with the countdown over a dot-and-line timeline. */
@Composable
private fun UpcomingTripCard(saved: SavedItinerary, today: LocalDate, modifier: Modifier = Modifier) {
    val itinerary = saved.itinerary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.White)
            .aiFrame(saved.addedByAi),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Sea)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (saved.addedByAi) AiChip("AIが追加", modifier = Modifier.padding(bottom = 2.dp))
                Text(itinerary.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(tripPeriodText(itinerary), fontSize = 12.sp, color = SeaC)
            }
            daysUntil(itinerary, today)?.let { Countdown(it) }
        }
        Timeline(itinerary.days)
    }
}

@Composable
private fun Countdown(days: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Explicit line heights: the theme's body style (24sp) would push 「日」 out of the circle.
        if (days > 0) {
            Text("出発まで", fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Medium, color = Sea)
            Text("$days", fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = Sea)
            Text("日", fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Medium, color = Sea)
        } else {
            Text("旅行中", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sea)
        }
    }
}

private val MarkerColumnWidth = 28.dp
private val TimelinePadding = 16.dp

/** Day markers on one vertical line, each followed by that day's spots. */
@Composable
private fun Timeline(days: List<ItineraryDay>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // From the first day's dot down to the last spot's icon.
                val x = (TimelinePadding + MarkerColumnWidth / 2).toPx()
                drawLine(
                    color = SeaC,
                    start = Offset(x, (10.dp + 11.dp).toPx()),
                    end = Offset(x, size.height - (10.dp + 15.dp).toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            .padding(horizontal = TimelinePadding, vertical = 10.dp),
    ) {
        days.forEachIndexed { index, day ->
            DayHeader(number = index + 1, date = day.date, modifier = Modifier.padding(top = if (index == 0) 0.dp else 4.dp))
            day.spots.forEach { spot -> SpotRow(spot) }
        }
    }
}

@Composable
private fun DayHeader(number: Int, date: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.width(MarkerColumnWidth), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Sea),
            )
        }
        Text("${number}日目", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Sea)
        Text(dayLabel(date), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun SpotRow(spot: Spot, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .heightIn(min = 30.dp)
            .padding(start = MarkerColumnWidth + 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SpotIcon(spotKindOf(spot))
        Text(
            spot.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class SpotStyle(val icon: ImageVector, val tint: Color, val background: Color)

private fun SpotKind.style(): SpotStyle = when (this) {
    SpotKind.HotSpring -> SpotStyle(Icons.Rounded.HotTub, Sun, SunC)
    SpotKind.Animals -> SpotStyle(Icons.Rounded.Pets, Leaf, LeafC)
    SpotKind.Water -> SpotStyle(Icons.Rounded.Water, Sea, SeaC)
    SpotKind.Lights -> SpotStyle(Icons.Rounded.AutoAwesome, Sun, SunC)
    SpotKind.Food -> SpotStyle(Icons.Rounded.RamenDining, Ink2, Surface2)
    SpotKind.Landmark -> SpotStyle(Icons.Rounded.TempleBuddhist, Ink2, Surface2)
    SpotKind.Other -> SpotStyle(Icons.Rounded.Place, Ink2, Surface2)
}

@Composable
private fun SpotIcon(kind: SpotKind, modifier: Modifier = Modifier) {
    val style = kind.style()
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(style.background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(style.icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = style.tint)
    }
}

// endregion

// region Boarding pass

/** A booking as a boarding pass: the route in airport codes, with a tear-off stub holding the barcode. */
@Composable
private fun BoardingPass(saved: SavedFlightBooking, modifier: Modifier = Modifier) {
    val booking = saved.booking
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(CardShape)
            .background(Color.White)
            .aiFrame(saved.addedByAi),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Sea)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "BOARDING PASS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = Color.White,
                )
                if (saved.addedByAi) AiChip("AIが予約")
            }
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Endpoint(booking.fromCity, alignEnd = false)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DashedLine(SeaC, Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.Flight,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(90f),
                        tint = Sea,
                    )
                    DashedLine(SeaC, Modifier.weight(1f))
                }
                Endpoint(booking.toCity, alignEnd = true)
            }
            Row(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp)) {
                PassField("日付", booking.date, Modifier.weight(1f))
                PassField("便名", booking.flightNumber, Modifier.weight(1f))
            }
        }
        Stub(booking.bookingId)
    }
}

/** A city as its airport code over the city name, or just the name for a city without a code. */
@Composable
private fun Endpoint(city: String, alignEnd: Boolean, modifier: Modifier = Modifier) {
    val code = TravelRules.airportCodeOf(city)
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        if (code != null) {
            Text(code, fontSize = 30.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(city, fontSize = 11.sp, color = Ink2)
        } else {
            Text(city, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
    }
}

@Composable
private fun PassField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = Ink2)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun DashedLine(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(2.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                )
            },
    )
}

/** The tear-off part: a dashed edge, a barcode drawn from the booking id, and the id itself. */
@Composable
private fun Stub(bookingId: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(76.dp)
            .fillMaxHeight()
            .drawBehind {
                drawLine(
                    color = Line,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                )
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Barcode(bookingId, Modifier.size(width = 40.dp, height = 72.dp))
        Text(bookingId, fontSize = 9.sp, letterSpacing = 1.sp, color = Ink2)
    }
}

/** Horizontal bars whose widths come from [seed], so a booking always shows the same pattern. */
@Composable
private fun Barcode(seed: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val random = Random(seed.hashCode())
        val unit = 1.5.dp.toPx()
        var y = 0f
        while (y < size.height) {
            val bar = unit * random.nextInt(1, 4)
            drawRect(Ink, topLeft = Offset(0f, y), size = Size(size.width, minOf(bar, size.height - y)))
            y += bar + unit * random.nextInt(1, 3)
        }
    }
}

// endregion

/** A past trip as two lines: the title and dates, then the spots. */
@Composable
private fun PastTripRow(saved: SavedItinerary, modifier: Modifier = Modifier) {
    val itinerary = saved.itinerary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RecordShape)
            .background(SurfaceBg)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                itinerary.title,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text(recordDateText(itinerary), fontSize = 12.sp, color = Ink2)
        }
        Text(
            spotsText(itinerary),
            fontSize = 12.sp,
            color = Ink2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFBFAF7, heightDp = 900)
@Composable
private fun TripScreenPreview() {
    TripTheme {
        TripScreen(
            state = tripUiState(
                itineraries = listOf(SavedItinerary(TripSeed.upcomingTrip, addedByAi = true)) +
                    TripSeed.pastTrips.map { SavedItinerary(it, addedByAi = false) },
                flightBookings = listOf(
                    SavedFlightBooking(FlightBooking("TRP-0001", "TR501", "東京", "札幌", "2026-12-20"), addedByAi = true),
                ),
                today = LocalDate.of(2026, 11, 14),
            ),
            onSeedUpcomingTrip = {},
            onReset = {},
        )
    }
}
