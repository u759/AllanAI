# Frontend Updates - Real-Time Stats & Highlights

## Summary
Updated the Android frontend to display comprehensive real-time statistics and highlights using actual API endpoints, removing all hardcoded/mock data. The implementation is inspired by `visualizer.js` which successfully parses and displays match data.

## Changes Made

### 1. **Domain Models (`Match.kt`)**
- ✅ Already properly structured with all nested statistics models
- ✅ Added missing `ShotType` enum values to match backend output:
  - `LOB`, `DRIVE`, `RALLY`, `SMASH`, `TOPSPIN`

### 2. **Response Mapping (`ResponseMapper.kt`)**
- ✅ Already complete with full mapping of nested statistics
- Maps all these properly:
  - `RallyMetrics` → `RallyMetrics`
  - `ShotSpeedMetrics` → `ShotSpeedMetrics`
  - `ServeMetrics` → `ServeMetrics`
  - `ReturnMetrics` → `ReturnMetrics`
  - `ShotTypeAggregate` → `ShotTypeAggregate`
  - `PlayerBreakdown` → `PlayerBreakdown`
  - `MomentumTimeline` → `MomentumTimeline`

### 3. **Highlights Screen (`HighlightsViewModel.kt`)**
- ✅ Fetches highlights from ALL matches via API
- ✅ Uses backend's curated highlights:
  - Play of the Game
  - Top 3 Rallies
  - Top 3 Fastest Shots
  - Top 3 Best Serves
- ✅ **Removed hardcoded data:**
  - Removed `R.raw.test_2` reference
  - Removed placeholder thumbnail URLs
  - Removed `HighlightThumbnailGenerator` import
- ✅ Now uses `match.thumbnailPath` from API

### 4. **Match Detail Screen (`MatchDetailScreen.kt`)**
- ✅ **Comprehensive Real-Time Stats Display:**
  - Current Score (updates in real-time)
  - Current Shot Details (speed, type, accuracy, result)
  - Rally Statistics (total, average, longest)
  - Shot Speed Analysis (fastest, average, incoming/outgoing)
  - Serve Performance (success rate, speed)
  - Return Performance (success rate, speed)
  - Shot Type Breakdown (count, avg speed, accuracy per type)
  - Player Performance (points, shots, speed, accuracy)
  - Live Stats (cumulative up to current video position)

### 5. **Match Detail ViewModel (`MatchDetailViewModel.kt`)**
- ✅ Fixed momentum timeline access (`.samples`)
- ✅ Fixed score state mapping
- ✅ Real-time stats calculation:
  - Tracks current position
  - Updates shot detection
  - Updates event detection
  - Calculates live cumulative stats
  - Updates score from momentum timeline

## How It Works (Like visualizer.js)

### 1. **Data Sources**
```
API Endpoints → MatchRepository → ViewModels → UI
```

### 2. **Real-Time Synchronization**
```kotlin
fun updatePosition(positionMs: Long) {
    _currentPositionMs.value = positionMs
    updateRealTimeStats(match, positionMs)
}
```

- As video plays, `currentPositionMs` updates
- ViewModel finds matching shots/events within time windows
- UI components observe these StateFlows and update automatically

### 3. **Statistics Hierarchy**
```
Match Statistics (from backend)
├── Rally Metrics
├── Shot Speed Metrics  
├── Serve Metrics
├── Return Metrics
├── Shot Type Breakdown
├── Player Breakdown
└── Momentum Timeline

Live Stats (calculated client-side)
├── Total Shots (up to current time)
├── Max/Avg Speed (up to current time)
└── Current Score (from momentum timeline)
```

### 4. **Highlights**
```
Highlights API Response
├── playOfTheGame (eventId)
├── topRallies (list of eventIds)
├── fastestShots (list of eventIds)
└── bestServes (list of eventIds)

↓ (ViewModel looks up actual events)

Highlight Items
├── Event details (title, description, timestamp)
├── Match context
└── Thumbnail (from match.thumbnailPath)
```

## Testing Checklist

### Highlights Tab
- [ ] Shows highlights from all completed matches
- [ ] Displays Play of the Game
- [ ] Displays Top 3 Rallies
- [ ] Displays Top 3 Fastest Shots
- [ ] Displays Top 3 Best Serves
- [ ] Uses actual thumbnails (not placeholders)

### Match Detail Screen - Real-Time Stats
- [ ] Score updates as video plays
- [ ] Current shot info displays when ball is hit
- [ ] Rally stats show correct values
- [ ] Speed metrics match backend data
- [ ] Serve/Return stats display correctly
- [ ] Shot type breakdown shows all types (LOB, DRIVE, etc.)
- [ ] Player performance metrics display
- [ ] Live cumulative stats update in sync with video

### Data Integrity
- [ ] No "0" values when real data exists
- [ ] All stats display correct units (km/h, mph, %, seconds)
- [ ] Timestamps format correctly (MM:SS)
- [ ] Player 1 vs Player 2 stats differentiated

## Known Issues & Future Improvements

1. **Thumbnail Generation**: Currently uses match-level thumbnails. Could generate event-specific thumbnails on backend.

2. **Video Sync Tolerance**: Uses ±50ms window for shots, ±100ms for events. May need tuning based on actual video playback behavior.

3. **Momentum Timeline**: Requires backend to include `momentumTimeline.samples` in response for score updates.

## Files Modified

1. `Match.kt` - Added shot type enum values
2. `ResponseMapper.kt` - Already complete (no changes needed)
3. `HighlightsViewModel.kt` - Removed hardcoded data, use API thumbnails
4. `MatchDetailScreen.kt` - Added comprehensive real-time stats display
5. `MatchDetailViewModel.kt` - Fixed timeline access and added live stats calculation
6. `NetworkModule.kt` - Fixed OkHttp logging to prevent OOM on video uploads

## No Mock/Fallback Code Remaining

✅ All data comes from API endpoints
✅ No hardcoded match IDs (except preview defaults)
✅ No placeholder URLs
✅ No test resource references
✅ Client-side calculations only when backend aggregates unavailable (legitimate fallback)
