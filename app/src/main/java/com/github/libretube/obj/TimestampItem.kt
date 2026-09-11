package com.github.libretube.obj

import java.util.regex.Pattern

enum class ChapterCategory {
    ALL,
    QUES,
    ANS,
    EXPLAIN,
    OTHER
}

data class TimestampItem(
    val timeMs: Long,
    val timeFormatted: String,
    val note: String,
    val category: ChapterCategory = ChapterCategory.OTHER
) {
    companion object {
        private val TIMESTAMP_PATTERN = Pattern.compile("(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})")

        fun parseToSeconds(str: String): Long {
            if (str.isBlank()) return 0L
            val parts = str.trim().split(':').mapNotNull { it.toLongOrNull() }
            return when (parts.size) {
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                2 -> parts[0] * 60 + parts[1]
                1 -> parts[0]
                else -> 0L
            }
        }

        fun parseFromText(text: String): List<TimestampItem> {
            if (text.isBlank()) return emptyList()

            val items = mutableListOf<TimestampItem>()
            val lines = text.lines()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                // Check for markdown table format from AI output:
                // | Q# | Question Start | Correct Option Timestamp | Answer Start | Correct Option |
                if (trimmed.startsWith('|')) {
                    val lower = trimmed.lowercase()
                    if (lower.contains("question start") || lower.contains("correct option") || lower.matches(Regex("^\\|?[\\s\\-:\\|]+\\|?$"))) {
                        continue
                    }
                    val cells = trimmed.split('|').map { it.trim() }.filter { it.isNotEmpty() }
                    if (cells.size >= 2) {
                        val qNum = if (cells[0].startsWith("Q", ignoreCase = true)) cells[0] else "Q${cells[0]}"
                        val qStart = cells[1]
                        val optTs = cells.getOrNull(2).orEmpty()
                        val ansStart = cells.getOrNull(3).orEmpty()
                        val optLetter = cells.getOrNull(4).orEmpty()

                        val mQ = TIMESTAMP_PATTERN.matcher(qStart)
                        if (mQ.find()) {
                            val timeStr = mQ.group(0) ?: ""
                            items.add(TimestampItem(parseToSeconds(timeStr) * 1000L, timeStr, "$qNum: Question Start", ChapterCategory.QUES))
                        }

                        val mOpt = TIMESTAMP_PATTERN.matcher(optTs)
                        if (mOpt.find() && !optTs.equals("N/A", ignoreCase = true)) {
                            val timeStr = mOpt.group(0) ?: ""
                            val optNote = if (optLetter.isNotEmpty() && !optLetter.equals("N/A", ignoreCase = true) && !optLetter.equals("Unclear", ignoreCase = true)) {
                                "$qNum: Correct Option ($optLetter)"
                            } else {
                                "$qNum: Correct Option"
                            }
                            items.add(TimestampItem(parseToSeconds(timeStr) * 1000L, timeStr, optNote, ChapterCategory.ANS))
                        }

                        val mAns = TIMESTAMP_PATTERN.matcher(ansStart)
                        if (mAns.find() && !ansStart.equals("N/A", ignoreCase = true)) {
                            val timeStr = mAns.group(0) ?: ""
                            items.add(TimestampItem(parseToSeconds(timeStr) * 1000L, timeStr, "$qNum: Answer Explanation", ChapterCategory.EXPLAIN))
                        }
                        continue
                    }
                }

                // Standard timestamp lines: e.g. "01:23 Topic Description"
                val matcher = TIMESTAMP_PATTERN.matcher(trimmed)
                if (matcher.find()) {
                    val timeString = matcher.group(0) ?: ""
                    val totalMs = parseToSeconds(timeString) * 1000L

                    var note = trimmed.removeRange(matcher.start(), matcher.end()).trim()
                    note = note.trimStart('-', ':', '|', '•', '–', '—').trim()
                    if (note.isEmpty()) {
                        note = "Timestamp at $timeString"
                    }

                    val lower = note.lowercase()
                    val cat = when {
                        lower.contains("question") || lower.contains("ques") || lower.contains("q#") -> ChapterCategory.QUES
                        lower.contains("correct option") || lower.contains("option") || lower.contains("ans:") -> ChapterCategory.ANS
                        lower.contains("explanation") || lower.contains("explain") || lower.contains("answer") -> ChapterCategory.EXPLAIN
                        else -> ChapterCategory.OTHER
                    }

                    items.add(TimestampItem(totalMs, timeString, note, cat))
                }
            }
            return items.sortedBy { it.timeMs }
        }
    }
}