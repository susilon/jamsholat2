package com.jamsholat2.android.ui.components

import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn

@Composable
fun InfoCard(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    InfoCardInternal(content = htmlContent, isMarkdown = false, modifier = modifier)
}

@Composable
fun MarkdownInfoCard(
    markdownContent: String,
    modifier: Modifier = Modifier
) {
    InfoCardInternal(content = markdownContent, isMarkdown = true, modifier = modifier)
}

@Composable
private fun InfoCardInternal(
    content: String,
    isMarkdown: Boolean,
    modifier: Modifier = Modifier
) {
    // Auto-detect markdown if not explicitly set: contains ** or starts with #
    val autoIsMarkdown = isMarkdown || content.contains("**") || content.trim().startsWith("#")
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0x80000000))
            .padding(12.dp)
            .widthIn(min = 200.dp)
    ) {
        if (autoIsMarkdown) {
            // If content looks like markdown but also contains HTML, try markdown first, fallback to plain
            // For markdown, we render directly; for html-like, it will just show as text
            MarkdownContent(markdown = content)
        } else {
            val plain = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString()
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(content).toString()
                }
            } catch (_: Exception) {
                content
            }
            Text(
                text = plain.ifBlank { " " },
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Right,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MarkdownContent(markdown: String) {
    val lines = markdown.trim().lines()
    Column(modifier = Modifier.fillMaxWidth()) {
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            when {
                line.startsWith("# ") -> {
                    val text = line.removePrefix("# ").trim()
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 70.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                line.startsWith("## ") -> {
                    val text = line.removePrefix("## ").trim()
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                line.startsWith("### ") -> {
                    val text = line.removePrefix("### ").trim()
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    // Handle **bold** inline
                    val annotated = buildAnnotatedString {
                        var currentIndex = 0
                        val regex = Regex("\\*\\*(.*?)\\*\\*")
                        val matches = regex.findAll(line)
                        if (matches.none()) {
                            append(line)
                        } else {
                            for (match in matches) {
                                val start = match.range.first
                                val end = match.range.last + 1
                                if (currentIndex < start) {
                                    append(line.substring(currentIndex, start))
                                }
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                                    append(match.groupValues[1])
                                }
                                currentIndex = end
                            }
                            if (currentIndex < line.length) {
                                append(line.substring(currentIndex))
                            }
                        }
                    }
                    // Check if the whole line is bold (e.g., **Iqomah**)
                    val isBoldLine = line.matches(Regex("\\*\\*.*\\*\\*"))
                    Text(
                        text = annotated,
                        color = Color.White,
                        fontSize = if (isBoldLine) 18.sp else 16.sp,
                        fontWeight = if (isBoldLine) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
