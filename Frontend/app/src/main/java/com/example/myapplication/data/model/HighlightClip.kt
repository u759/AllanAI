package com.example.myapplication.data.model

/**
 * Represents a highlight video clip extracted from a match.
 * 
 * Based on the AllanAI architecture guidelines, highlights are extracted from match videos
 * using event timestamps and eventWindow metadata (preMs/postMs).
 * 
 * This model follows the OpenTTGames dataset convention where events have:
 * - timestampMs: The main event timestamp
 * - timestampSeries: Context timestamps (pre + main + post frames)
 * - eventWindow: Buffer times before (preMs) and after (postMs) the event
 */
data class HighlightClip(
    val id: String,              // Event ID from the backend
    val matchId: String,         // Match this highlight belongs to
    val category: HighlightCategory,
    val title: String,
    val description: String,
    val startMs: Long,           // Start timestamp for the clip
    val endMs: Long,             // End timestamp for the clip
    val mainEventMs: Long,       // The primary event timestamp
    val videoUri: String?,       // URI to the extracted clip file (null if not yet extracted)
    val event: Event             // Full event data for metadata display
)

/**
 * Categories of highlights based on event types.
 */
enum class HighlightCategory(val displayName: String) {
    PLAY_OF_THE_GAME("Play of the Game"),
    TOP_RALLY("Top Rally"),
    FASTEST_SHOT("Fastest Shot"),
    BEST_SERVE("Best Serve"),
    SCORE("Score"),
    OTHER("Highlight")
}

/**
 * Helper to map EventType to HighlightCategory.
 */
fun EventType.toHighlightCategory(): HighlightCategory {
    return when (this) {
        EventType.PLAY_OF_THE_GAME -> HighlightCategory.PLAY_OF_THE_GAME
        EventType.RALLY_HIGHLIGHT -> HighlightCategory.TOP_RALLY
        EventType.FASTEST_SHOT -> HighlightCategory.FASTEST_SHOT
        EventType.SERVE_ACE -> HighlightCategory.BEST_SERVE
        EventType.SCORE -> HighlightCategory.SCORE
        EventType.MISS -> HighlightCategory.OTHER
    }
}

/**
 * Mapper to create HighlightClips from Match data.
 * 
 * This follows the backend's buildHighlights() logic which extracts:
 * - playOfTheGame: Event with highest importance
 * - topRallies: Top 3 RALLY_HIGHLIGHT events
 * - fastestShots: Top 3 FASTEST_SHOT events  
 * - bestServes: Top 3 SERVE_ACE events
 */
object HighlightClipExtractor {
    
    /**
     * Extract all highlight clips from a match.
     * 
     * Uses the highlights data structure from the backend which contains
     * eventId references. We look up each event and create a clip with
     * proper time boundaries based on eventWindow.
     */
    fun extractHighlightClips(match: Match): List<HighlightClip> {
        val highlights = match.highlights ?: return emptyList()
        val clips = mutableListOf<HighlightClip>()
        
        // Play of the Game (single highlight)
        highlights.playOfTheGame?.let { eventId ->
            match.events.find { it.id == eventId }?.let { event ->
                clips.add(createClipFromEvent(match.id, event, HighlightCategory.PLAY_OF_THE_GAME))
            }
        }
        
        // Top Rallies (up to 3)
        highlights.topRallies.forEach { eventId ->
            match.events.find { it.id == eventId }?.let { event ->
                clips.add(createClipFromEvent(match.id, event, HighlightCategory.TOP_RALLY))
            }
        }
        
        // Fastest Shots (up to 3)
        highlights.fastestShots.forEach { eventId ->
            match.events.find { it.id == eventId }?.let { event ->
                clips.add(createClipFromEvent(match.id, event, HighlightCategory.FASTEST_SHOT))
            }
        }
        
        // Best Serves (up to 3)
        highlights.bestServes.forEach { eventId ->
            match.events.find { it.id == eventId }?.let { event ->
                clips.add(createClipFromEvent(match.id, event, HighlightCategory.BEST_SERVE))
            }
        }
        
        return clips
    }
    
    /**
     * Create a HighlightClip from an Event.
     * 
     * Time boundaries are calculated using:
     * - ALWAYS use 3 seconds before and 3 seconds after for good context
     * - Ignore the tiny eventWindow values from JSON (33ms/100ms are too short)
     */
    private fun createClipFromEvent(
        matchId: String,
        event: Event,
        category: HighlightCategory
    ): HighlightClip {
        // Always use 3 seconds before and after - ignore JSON eventWindow
        // The JSON has preMs=33, postMs=100 which is too short (only 133ms total)
        val preMs = 3000L   // 3 seconds before
        val postMs = 3000L  // 3 seconds after
        
        // Calculate clip boundaries
        // Architecture guideline: "Use metadata.eventWindow to compute highlight preview segments"
        val startMs = maxOf(0, event.timestampMs - preMs)
        val endMs = event.timestampMs + postMs
        
        return HighlightClip(
            id = event.id,
            matchId = matchId,
            category = category,
            title = event.title,
            description = buildDescription(event, category),
            startMs = startMs,
            endMs = endMs,
            mainEventMs = event.timestampMs,
            videoUri = null,  // Will be set after clip extraction
            event = event
        )
    }
    
    /**
     * Build a descriptive text for the highlight.
     */
    private fun buildDescription(event: Event, category: HighlightCategory): String {
        val parts = mutableListOf<String>()
        val metadata = event.metadata
        
        when (category) {
            HighlightCategory.FASTEST_SHOT -> {
                metadata?.shotSpeed?.let { speed ->
                    parts.add("${speed.toInt()} km/h")
                }
                metadata?.shotType?.let { type ->
                    parts.add(type.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
            HighlightCategory.TOP_RALLY -> {
                metadata?.rallyLength?.let { length ->
                    parts.add("$length shots")
                }
            }
            HighlightCategory.BEST_SERVE -> {
                metadata?.shotSpeed?.let { speed ->
                    parts.add("${speed.toInt()} km/h serve")
                }
            }
            HighlightCategory.SCORE -> {
                metadata?.scoreAfter?.let { score ->
                    parts.add("${score.player1}-${score.player2}")
                }
            }
            else -> {
                if (event.description.isNotEmpty()) {
                    parts.add(event.description)
                }
            }
        }
        
        event.player?.let { player ->
            parts.add("Player $player")
        }
        
        return if (parts.isEmpty()) event.description else parts.joinToString(" • ")
    }
}
