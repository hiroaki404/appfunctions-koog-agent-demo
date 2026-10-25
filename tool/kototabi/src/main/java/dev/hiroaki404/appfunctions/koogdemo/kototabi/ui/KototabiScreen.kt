package dev.hiroaki404.appfunctions.koogdemo.kototabi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hiroaki404.appfunctions.koogdemo.kototabi.FlightBooking
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.KototabiSeed
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.SavedFlightBooking
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.SavedItinerary
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.theme.Bg
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.theme.Ink
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.theme.Ink2
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.theme.KototabiTheme
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.theme.Sea
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.theme.Sun
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.theme.SunC
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.theme.SurfaceBg

private val TicketShape = RoundedCornerShape(18.dp)
private val TicketRingShape = RoundedCornerShape(21.dp)
private val CardShape = RoundedCornerShape(16.dp)
private val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Ink)

/** Stateless so the layout stays independent of Room and AppFunctions. */
@Composable
fun KototabiScreen(
    state: KototabiUiState,
    onSeedUpcomingTrip: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AppBar(onSeedUpcomingTrip = onSeedUpcomingTrip, onReset = onReset) }
        if (state.flightBookings.isNotEmpty()) {
            item { SectionLabel("航空券") }
            items(state.flightBookings) { saved -> FlightTicket(saved) }
        }
        item { SectionLabel("旅程") }
        if (state.itineraries.isEmpty()) {
            item { Text("旅程はまだありません", fontSize = 14.sp, color = Ink2) }
        }
        items(state.itineraries) { saved -> ItineraryCard(saved) }
    }
}

@Composable
private fun AppBar(onSeedUpcomingTrip: () -> Unit, onReset: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("kototabi", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("ことたび — 旅のきろく", fontSize = 12.sp, color = Ink2)
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
        modifier = modifier.padding(top = 4.dp),
        fontSize = 13.sp,
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
            .padding(horizontal = 8.dp, vertical = 2.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
}

@Composable
private fun FlightTicket(saved: SavedFlightBooking, modifier: Modifier = Modifier) {
    val ticket = @Composable {
        // The body sets the height; the strip is overlaid at that height. (IntrinsicSize.Min doesn't work
        // here because the rotated "BOARDING" label reports unusable intrinsic sizes.)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TicketShape)
                .background(Bg)
                .border(2.dp, Sea, TicketShape),
        ) {
            TicketBody(saved, modifier = Modifier.fillMaxWidth().padding(start = TicketStripWidth))
            Box(modifier = Modifier.matchParentSize()) { TicketStrip() }
        }
    }
    if (saved.addedByAi) {
        // Outer ring: a 3dp sunC gap inside a 2dp sun border.
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(SunC, TicketRingShape)
                .border(2.dp, Sun, TicketRingShape)
                .padding(3.dp),
        ) { ticket() }
    } else {
        Box(modifier = modifier) { ticket() }
    }
}

private val TicketStripWidth = 64.dp

@Composable
private fun TicketStrip(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(TicketStripWidth)
            .fillMaxHeight()
            .background(Sea),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.Flight,
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .rotate(45f),
            tint = Color.White,
        )
        Text(
            "BOARDING",
            modifier = Modifier
                .padding(top = 20.dp)
                .rotatedVertically(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color.White,
            softWrap = false,
        )
    }
}

/** Rotates 90 degrees clockwise and reports the swapped size, so the rotated text takes the right space. */
private fun Modifier.rotatedVertically(): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(Constraints())
        layout(placeable.height, placeable.width) {
            placeable.place((placeable.height - placeable.width) / 2, (placeable.width - placeable.height) / 2)
        }
    }
    .rotate(90f)

@Composable
private fun TicketBody(saved: SavedFlightBooking, modifier: Modifier = Modifier) {
    val booking = saved.booking
    Column(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (saved.addedByAi) AiChip("AIが予約")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(booking.from, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Sea,
            )
            Text(booking.to, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledValue("日付", booking.date)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledValue("便名", booking.flightNumber)
            LabeledValue("予約", booking.bookingId)
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 11.sp, color = Ink2)
        Text(value, style = MonoStyle, fontSize = 13.sp)
    }
}

@Composable
private fun ItineraryCard(saved: SavedItinerary, modifier: Modifier = Modifier) {
    val itinerary = saved.itinerary
    val background = if (saved.addedByAi) {
        Modifier
            .background(SunC, CardShape)
            .border(2.dp, Sun, CardShape)
    } else {
        Modifier.background(SurfaceBg, CardShape)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (saved.addedByAi) AiChip("AIが追加")
        Text(itinerary.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
        itinerary.days.forEach { day ->
            Row {
                Text(
                    monthDay(day.date),
                    modifier = Modifier.width(52.dp),
                    style = MonoStyle,
                    fontSize = 12.sp,
                    color = Ink2,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    day.spots.forEach { spot -> Text(spot.name, fontSize = 14.sp, color = Ink) }
                }
            }
        }
    }
}

/** Turns `2026-12-20` into `12/20`; anything unexpected is shown as is. */
private fun monthDay(date: String): String =
    if (date.length == 10) date.substring(5).replace('-', '/') else date

@Preview(showBackground = true, backgroundColor = 0xFFFBFAF7)
@Composable
private fun KototabiScreenPreview() {
    KototabiTheme {
        KototabiScreen(
            state = KototabiUiState(
                itineraries = listOf(
                    SavedItinerary(KototabiSeed.upcomingTrip, addedByAi = true),
                    SavedItinerary(KototabiSeed.pastTrip, addedByAi = false),
                ),
                flightBookings = listOf(
                    SavedFlightBooking(FlightBooking("KTB-0001", "KT501", "東京", "札幌", "2026-12-20"), addedByAi = true),
                    SavedFlightBooking(FlightBooking("KTB-0002", "KT502", "札幌", "東京", "2026-12-22"), addedByAi = false),
                ),
            ),
            onSeedUpcomingTrip = {},
            onReset = {},
        )
    }
}
