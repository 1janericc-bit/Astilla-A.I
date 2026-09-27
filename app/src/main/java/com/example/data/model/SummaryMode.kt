package com.example.data.model

enum class SummaryMode(val displayName: String, val description: String, val iconName: String) {
    EXECUTIVE(
        displayName = "Executive Summary",
        description = "High-level overview, strategic thesis, and core findings",
        iconName = "Description"
    ),
    TAKEAWAYS(
        displayName = "Key Takeaways & Action Items",
        description = "Bulleted vital takeaways, actionable steps, and decisions",
        iconName = "Checklist"
    ),
    CHAPTERS(
        displayName = "Section & Chapter Breakdown",
        description = "Structured walkthrough broken into sequential themes",
        iconName = "MenuBook"
    ),
    FLASHCARDS(
        displayName = "Flashcards & Quick Brief",
        description = "High-yield Q&A cards and rapid memory points",
        iconName = "Bolt"
    ),
    DEEP_DIVE(
        displayName = "Deep Dive Analysis",
        description = "Comprehensive analysis, arguments, counterpoints & context",
        iconName = "AutoAwesome"
    )
}
