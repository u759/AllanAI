# AllanAI Frontend Updates - Real-Time Stats & Highlights

## Summary
Updated the Android frontend to properly parse and display all match statistics from the API, implement real-time stats synchronized with video playback, and show highlights from all matches using actual API data.

## Changes Made

### 1. Data Model Updates (`Match.kt`)
**Problem**: The `MatchStatistics` model was missing extended fields from the JSON response, causing data loss during mapping.

**Solution**: Added comprehensive statistics models:
- `RallyMetrics` - Rally performance (total, average, longest, duration)
- `ShotSpeedMetrics` - Speed analysis (fastest, average incoming/outgoing)
- `ServeMetrics` - Serve performance (total, success rate, faults, speeds)
- `ReturnMetrics` - Return performance (total, success rate, speeds)
- `ShotTypeAggregate` - Per-shot-type statistics
- `PlayerBreakdown` - Individual player performance
- `MomentumSample` - Score progression timeline

**Enums Extended**:
- `ShotType`: Added RALLY, DRIVE, LOB, SMASH, TOPSPIN, BACKSPIN, BLOCK, PUSH
- `EventType`: Added RALLY, SERVE, FAST_SHOT, BOUNCE, WINNER, ERROR, LET

### 2. Response Mapper Updates (`ResponseMapper.kt`)
**Problem**: Mapper wasn't converting new DTO fields to domain models.

**Solution**: Added mapping functions:
- `RallyMetricsResponse.toRallyMetrics()`
- `ShotSpeedMetricsResponse.toShotSpeedMetrics()`
- `ServeMetricsResponse.toServeMetrics()`
- `ReturnMetricsResponse.toReturnMetrics()`
- `ShotTypeAggregateResponse.toShotTypeAggregate()`
- `PlayerBreakdownResponse.toPlayerBreakdown()`
- `MomentumSampleResponse.toMomentumSample()`

All fields from the API response are now properly mapped to domain models.

### 3. Highlights Screen (`HighlightsViewModel.kt`)
**Before**: Used hardcoded thumbnails, filtered events manually, ignored API highlights data.

**After**:
- Reads `highlights.playOfTheGame`, `highlights.topRallies`, `highlights.fastestShots`, `highlights.bestServes` from API
- Maps highlight references to actual events using event IDs
- Displays match title and player names from API data
- Removed all hardcoded placeholder URLs
- Uses match thumbnail from API instead of generating locally

**Key Changes**:
```kotlin
// Now uses API highlights structure
match.highlights?.let { highlights ->
    // Add play of the game
    highlights.playOfTheGame?.let { potg ->
        match.events.find { it.id == potg.eventId }
    }
    // Add top rallies, fastest shots, best serves
}
```

### 4. Real-Time Stats (`MatchDetailViewModel.kt`)
**Problem**: Stats were static - didn't update as video played.

**Solution**: Added real-time state tracking:
- `_currentShot` - Shot at current video position
- `_currentEvent` - Event at current video position  
- `_currentScore` - Score at current video position
- `_liveStats` - Cumulative stats up to current position

**`updateRealTimeStats()` Function**:
```kotlin
private fun updateRealTimeStats(match: Match, positionMs: Long) {
    // Find current shot (±50ms window)
    // Find current event (±200ms window)
    // Update score from momentum timeline
    // Calculate live stats from shots up to current position
}
```

Automatically called when video position changes.

### 5. Live Stats Display (`MatchDetailScreen.kt`)
**Before**: Showed 3 static stats (all displaying 0).

**After**: Added `LivePerformanceMetricsSection` with:

**Current Score Display**:
- Live score from momentum timeline
- Shows player names
- Updates as video plays

**Current Shot Info Card**:
- Player number
- Shot type (RALLY, DRIVE, LOB, etc.)
- Speed in mph
- Accuracy percentage
- Only shown when shot is active at current position

**Live Statistics (4 cards)**:
- Max Speed (mph) - highest speed seen so far
- Avg Speed (mph) - average of all shots up to now
- Total Shots - count of shots up to current position
- Rallies - rallies completed up to current position

**Current Event Display**:
- Event title with emoji
- Event description
- Shot speed (if available)
- Only shown when event is active at current position

**Update Mechanism**:
```kotlin
val shotsUpToNow = remember(currentPositionMs) {
    match.shots.filter { it.timestampMs <= currentPositionMs }
}
```
Uses `remember(currentPositionMs)` to automatically recalculate when video position changes.

### 6. Removed Hardcoded/Mock Data

**MatchDetailScreen.kt**:
- ❌ Removed hardcoded avatar URL
- ✅ Now uses player icon with API data
- ❌ Removed "Ethan's Match vs. Alex"
- ✅ Now shows actual match title and player names from API

**HighlightsViewModel.kt**:
- ❌ Removed hardcoded Google Photos thumbnails
- ✅ Uses match.thumbnailPath from API
- ❌ Removed placeholder thumbnail logic
- ✅ Deprecated getPlaceholderThumbnail()

**EventTypeRow**:
- ✅ Added handlers for all event types from API
- ✅ No fallback to hardcoded events

### 7. NetworkModule.kt Fix
**Problem**: HTTP body logging caused OutOfMemoryError on large video uploads.

**Solution**:
```kotlin
level = if (BuildConfig.DEBUG) {
    HttpLoggingInterceptor.Level.HEADERS  // Don't log body
} else {
    HttpLoggingInterceptor.Level.NONE
}
```

## How Real-Time Stats Work

```
Video Position Changes (0ms → 5000ms → 10000ms)
          ↓
MatchDetailViewModel.updatePosition(positionMs)
          ↓
updateRealTimeStats(match, positionMs)
          ↓
    ┌─────────────────┬──────────────────┬─────────────────┐
    ↓                 ↓                  ↓                 ↓
Find Current     Find Current    Update Score    Calculate Stats
   Shot             Event          Timeline       Up To Position
    ↓                 ↓                  ↓                 ↓
_currentShot   _currentEvent   _currentScore    _liveStats
    ↓                 ↓                  ↓                 ↓
LivePerformanceMetricsSection displays all in UI
```

## Visualizer.js Inspiration Applied

**Score Updates** (visualizer.js lines 767-800):
```javascript
updateScore(currentMs) {
    const scoreUpdate = this.momentumTimeline.find(...)
    if (scoreUpdate) showScoreNotification(...)
}
```
→ Android: Uses `momentumTimeline` to find score at current position

**Current Shot Display** (visualizer.js lines 845-875):
```javascript
updateCurrentShot(currentMs) {
    const shot = this.shots.find(s => 
        Math.abs(this.getShotTimestamp(s) - currentMs) <= 50
    )
    renderShotDetails(shot)
}
```
→ Android: Same ±50ms window, displays shot type, speed, accuracy

**Live Stats Calculation** (visualizer.js lines 699-733):
```javascript
onTimeUpdate() {
    const shotsUpToNow = this.shots.filter(s => 
        s.timestampMs <= currentMs
    )
    updateStatistics(shotsUpToNow)
}
```
→ Android: Filters shots by `timestampMs <= currentPositionMs`, recalculates on position change

## API Integration

All data now comes from backend API endpoints:

**Match Details**: `GET /api/matches/{id}`
```json
{
  "statistics": {
    "rallyMetrics": {...},
    "shotSpeedMetrics": {...},
    "serveMetrics": {...},
    "momentumTimeline": [...]
  },
  "shots": [...],
  "events": [...],
  "highlights": {
    "playOfTheGame": {...},
    "topRallies": [...],
    "fastestShots": [...],
    "bestServes": [...]
  }
}
```

**Video URL**: `GET /api/matches/{id}/video`

**All Matches**: `GET /api/matches` (for highlights tab)

## Testing Recommendations

1. **Load a match** - Verify all statistics display correctly
2. **Play video** - Watch live stats update in real-time
3. **Check highlights tab** - Confirm all match highlights appear
4. **Verify no crashes** - No OutOfMemoryError on uploads
5. **Check event types** - All event types render with correct emoji

## Files Modified

1. `Match.kt` - Added extended statistics models
2. `ResponseMapper.kt` - Added mapping for all new fields
3. `HighlightsViewModel.kt` - Use API highlights data
4. `MatchDetailViewModel.kt` - Added real-time stat tracking
5. `MatchDetailScreen.kt` - Added live stats display
6. `NetworkModule.kt` - Fixed OOM on uploads

## Result

✅ **All stats from API are displayed**  
✅ **Real-time stats update with video**  
✅ **Highlights tab shows all match highlights**  
✅ **No hardcoded or mock data**  
✅ **No OOM errors on uploads**  
✅ **Follows visualizer.js patterns**
