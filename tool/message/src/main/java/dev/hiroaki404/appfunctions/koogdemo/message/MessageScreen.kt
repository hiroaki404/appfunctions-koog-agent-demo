package dev.hiroaki404.appfunctions.koogdemo.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hiroaki404.appfunctions.koogdemo.message.ui.theme.Bg
import dev.hiroaki404.appfunctions.koogdemo.message.ui.theme.Ink
import dev.hiroaki404.appfunctions.koogdemo.message.ui.theme.Ink2
import dev.hiroaki404.appfunctions.koogdemo.message.ui.theme.Line
import dev.hiroaki404.appfunctions.koogdemo.message.ui.theme.MessageTheme
import dev.hiroaki404.appfunctions.koogdemo.message.ui.theme.Sea
import dev.hiroaki404.appfunctions.koogdemo.message.ui.theme.SeaContainer
import dev.hiroaki404.appfunctions.koogdemo.message.ui.theme.Sun
import dev.hiroaki404.appfunctions.koogdemo.message.ui.theme.Surface2
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("M月d日")

/**
 * Chat thread view of the most recent contact. Every message was sent by the AI through the
 * AppFunction, so all bubbles are AI-styled. The compose bar is decorative.
 */
@Composable
fun MessageScreen(state: MessageUiState, modifier: Modifier = Modifier) {
    val thread = state.currentThread
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .imePadding(),
    ) {
        ThreadTopBar(contact = thread?.contact)
        HorizontalDivider(color = Line)
        if (thread == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "送信したメッセージはまだありません", color = Ink2, fontSize = 14.sp)
            }
        } else {
            ThreadMessages(messages = thread.messages, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = Line)
        ComposeBar()
    }
}

@Composable
private fun ThreadTopBar(contact: Contact?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (contact == null) {
            Text(text = "メッセージ", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            return
        }
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(SeaContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = contact.name.take(1),
                color = Sea,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column {
            Text(text = contact.name, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            contact.aliases.firstOrNull()?.let { alias ->
                Text(text = alias, color = Ink2, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ThreadMessages(messages: List<SentMessage>, modifier: Modifier = Modifier) {
    // Reversed layout keeps the list bottom-aligned with the newest message at the bottom.
    val items = remember(messages) { messages.toThreadItems().asReversed() }
    val today = remember { LocalDate.now() }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * 0.8f
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(items) { item ->
                when (item) {
                    is ThreadItem.DateSeparator -> DateSeparator(item.date, today)
                    is ThreadItem.Message -> AiMessage(item.message, maxBubbleWidth)
                }
            }
        }
    }
}

@Composable
private fun DateSeparator(date: LocalDate, today: LocalDate) {
    Text(
        text = if (date == today) "今日" else dateFormatter.format(date),
        modifier = Modifier.fillMaxWidth(),
        color = Ink2,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AiMessage(message: SentMessage, maxBubbleWidth: Dp) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "AIが送信",
            modifier = Modifier
                .clip(CircleShape)
                .background(Sun)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = message.text,
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 4.dp, bottomStart = 18.dp))
                .background(Sun)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            color = Color.White,
            fontSize = 15.sp,
            lineHeight = 22.5.sp,
        )
        Text(
            text = timeFormatter.format(Instant.ofEpochMilli(message.sentAtMillis)),
            color = Ink2,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ComposeBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(CircleShape)
                .background(Surface2)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text = "メッセージ", color = Ink2, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Sea),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = ArrowUpwardIcon, contentDescription = null, tint = Color.White)
        }
    }
}

// Material "arrow_upward"; drawn here to avoid pulling in material-icons-extended.
private val ArrowUpwardIcon: ImageVector = ImageVector.Builder(
    name = "ArrowUpward",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(4f, 12f)
        lineToRelative(1.41f, 1.41f)
        lineTo(11f, 7.83f)
        verticalLineTo(20f)
        horizontalLineToRelative(2f)
        verticalLineTo(7.83f)
        lineToRelative(5.58f, 5.59f)
        lineTo(20f, 12f)
        lineToRelative(-8f, -8f)
        lineToRelative(-8f, 8f)
        close()
    }
}.build()

@Preview(showBackground = true)
@Composable
private fun MessageScreenPreview() {
    val tom = demoContacts.first()
    MessageTheme {
        MessageScreen(
            state = listOf(
                SentMessage(tom, "12月の北海道旅行、予定は空いてる?", System.currentTimeMillis()),
            ).toUiState(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageScreenEmptyPreview() {
    MessageTheme { MessageScreen(state = MessageUiState()) }
}
