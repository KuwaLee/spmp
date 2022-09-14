package net.zerotask.libraries.android.compose.furigana

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

@Composable
fun TextWithReading(
    textContent: List<TextData>,
    showReadings: Boolean = false,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    highlight_index: Int = -1,
    highlight_modifier: Modifier = Modifier.background(Color.Red)
) {

    val dataWithReadings = remember(textContent) {
        calculateAnnotatedString(
            textContent = textContent,
            showReadings = true,
            fontSize = if (fontSize == TextUnit.Unspecified) style.fontSize else fontSize,
            highlight_index = highlight_index,
            highlight_modifier = highlight_modifier
        )
    }

    val dataWithoutReadings = remember(textContent) {
        calculateAnnotatedString(
            textContent = textContent,
            showReadings = false,
            fontSize = if (fontSize == TextUnit.Unspecified) style.fontSize else fontSize,
            highlight_index = highlight_index,
            highlight_modifier = highlight_modifier
        )
    }

    val data = if (showReadings) dataWithReadings else dataWithoutReadings
    Text(
        data.first,
        modifier,
        color,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        letterSpacing,
        textDecoration,
        textAlign,
        lineHeight,
        overflow,
        softWrap,
        maxLines,
        data.second,
        onTextLayout,
        style
    )
}

fun calculateAnnotatedString(textContent: List<TextData>, showReadings: Boolean, fontSize: TextUnit, highlight_index: Int, highlight_modifier: Modifier):
        Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val inlineContent = mutableMapOf<String, InlineTextContent>()

    return buildAnnotatedString {

        var current_highlight_elem = 0
        var highlight_elem_index: Int? = null
        var total_length = 0

        for (i in 0 until textContent.size) {
            val elem = textContent[i]
            val text = elem.text
            total_length += text.length

            if (highlight_elem_index == null && highlight_index < total_length) {
                highlight_elem_index = i
            }

            val reading = elem.reading

            // If there is not reading available, simply add the text and move to the next element.
            if (reading == null && highlight_index < 0) {
                append(text)
                continue
            }

            // Words larger than one character/kanji need a small amount of additional space in their
            // x-dimension.
            val width = (text.length.toDouble() + (text.length - 1) * 0.05).em
            appendInlineContent(text, text)
            inlineContent[text] = InlineTextContent(
                // TODO: find out why height and width need magic numbers.
                placeholder = Placeholder(
                    width = width,
                    height = 1.97.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom,
                ),
                children = {
                    val readingFontSize = fontSize / 2
                    val boxHeight = with(LocalDensity.current) { readingFontSize.toDp() }

                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(modifier = Modifier.requiredHeight(boxHeight + 3.dp)) {
                            if (showReadings && reading != null) {
                                Text(
                                    modifier = Modifier.wrapContentWidth(unbounded = true),
                                    text = reading,
                                    style = TextStyle.Default.copy(fontSize = readingFontSize)
                                )
                            }
                        }
                        Text(text = text, fontSize = fontSize, modifier = if (current_highlight_elem++ == highlight_elem_index) highlight_modifier else Modifier)
                    }
                }
            )
        }
    } to inlineContent
}

@Preview
@Composable
internal fun PreviewTextWithReading() {
    val textContent = listOf(
        TextData(text = "このルールを"),
        TextData(text = "守", reading = "まも"),
        TextData(text = "らない"),
        TextData(text = "人", reading = "ひと"),
        TextData(text = "は"),
        TextData(text = "旅行", reading = "りょこう"),
        TextData(text = "ができなくなることもあります。"),
    )

    MaterialTheme {
        TextWithReading(textContent = textContent, showReadings = true)
    }
}

@Preview
@Composable
internal fun PreviewTextWithoutReading() {
    val textContent = listOf(
        TextData(text = "このルールを"),
        TextData(text = "守", reading = "まも"),
        TextData(text = "らない"),
        TextData(text = "人", reading = "ひと"),
        TextData(text = "は"),
        TextData(text = "旅行", reading = "りょこう"),
        TextData(text = "ができなくなることもあります。"),
    )

    MaterialTheme {
        TextWithReading(textContent = textContent, showReadings = true)
    }
}