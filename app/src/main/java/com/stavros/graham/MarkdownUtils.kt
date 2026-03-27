package com.stavros.graham

// Markdown syntax is meaningless noise when read aloud by TTS, so we strip it
// before passing text to the speech engine. This is intentionally lossy — we
// preserve the words, not the formatting.
fun stripMarkdown(text: String): String {
    return text
        // Fenced code blocks: remove the opening/closing fence lines but keep the body.
        .replace(Regex("```[\\w]*\\n?"), "")
        // Inline code backticks: remove the backticks, keep the content.
        .replace(Regex("`([^`]*)`"), "$1")
        // Bold+italic (must come before bold and italic individually).
        .replace(Regex("\\*\\*\\*(.+?)\\*\\*\\*"), "$1")
        .replace(Regex("___(.+?)___"), "$1")
        // Bold.
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("__(.+?)__"), "$1")
        // Italic.
        .replace(Regex("\\*(.+?)\\*"), "$1")
        .replace(Regex("_(.+?)_"), "$1")
        // ATX headings at the start of a line.
        .replace(Regex("(?m)^#{1,6}\\s+"), "")
        // Images before links so the `![` prefix is handled first.
        .replace(Regex("!\\[([^]]*)]\\([^)]*\\)"), "$1")
        // Links.
        .replace(Regex("\\[([^]]*)]\\([^)]*\\)"), "$1")
        // Unordered list markers.
        .replace(Regex("(?m)^[\\-\\*]\\s+"), "")
        // Collapse runs of blank lines down to a single blank line.
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
