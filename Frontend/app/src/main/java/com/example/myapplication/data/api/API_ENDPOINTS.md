# AllanAI API Endpoints Reference

This document lists all available backend endpoints and how they're used in the Android app.

## Base URL

**Emulator**: `http://10.0.2.2:8080/`  
**Physical Device**: Update in `NetworkModule.kt` with your computer's IP

---

## Endpoints

### 1. Upload Match Video

**Upload a video file for processing**

```http
POST /api/matches/upload
Content-Type: multipart/form-data
```

**Request:**
```kotlin
// In ApiMatchRepository
val requestBody = videoFile.asRequestBody("video/mp4".toMediaType())
val part = MultipartBody.Part.createFormData("video", filename, requestBody)
apiService.uploadMatch(part)
```

**Response:**
```json
{
  "matchId": "0ee996ad-3f02-4ba9-bafd-465a6a6dcf10",
  "status": "PROCESSING"
}
```

**Status Codes:**
- `202 Accepted` - Upload successful, processing started
- `400 Bad Request` - Invalid video file
- `500 Internal Server Error` - Server error

---

### 2. List All Matches

**Get list of all matches**

```http
GET /api/matches
```

**Response:**
```json
[
  {
    "id": "abc123",
    "createdAt": "2025-10-05T10:30:00Z",
    "processedAt": "2025-10-05T10:35:00Z",
    "status": "COMPLETE",
    "durationSeconds": 1800,
    "originalFilename": "match.mp4"
  },
  ...
]
```

**Usage:**
```kotlin
val matches = apiService.getMatches()
```

---

### 3. Get Match Details

**Get complete match information including statistics, events, and highlights**

```http
GET /api/matches/{id}
```

**Response:**
```json
{
  "id": "abc123",
  "createdAt": "2025-10-05T10:30:00Z",
  "processedAt": "2025-10-05T10:35:00Z",
  "status": "COMPLETE",
  "durationSeconds": 1800,
  "originalFilename": "match.mp4",
  "statistics": {
    "player1Score": 11,
    "player2Score": 8,
    "totalRallies": 45,
    "avgRallyLength": 6.5,
    "maxBallSpeed": 52.3,
    "avgBallSpeed": 32.8
  },
  "shots": [...],
  "events": [...],
  "highlights": {...}
}
```

**Usage:**
```kotlin
val matchDetails = apiService.getMatchDetails(matchId)
val match = matchDetails.toMatch()
```

---

### 4. Get Match Status

**Check processing status of a match**

```http
GET /api/matches/{id}/status
```

**Response:**
```json
{
  "id": "abc123",
  "status": "COMPLETE",
  "processedAt": "2025-10-05T10:35:00Z"
}
```

**Status Values:**
- `UPLOADED` - Just uploaded, not yet processing
- `PROCESSING` - Currently being analyzed
- `COMPLETE` - Processing finished successfully
- `FAILED` - Processing encountered an error

**Usage (polling):**
```kotlin
// Poll every 2 seconds until complete
while (true) {
    val status = apiService.getMatchStatus(matchId)
    if (status.status == "COMPLETE") break
    delay(2000)
}
```

---

### 5. Get Match Statistics

**Get statistics only (subset of match details)**

```http
GET /api/matches/{id}/statistics
```

**Response:**
```json
{
  "player1Score": 11,
  "player2Score": 8,
  "totalRallies": 45,
  "avgRallyLength": 6.5,
  "maxBallSpeed": 52.3,
  "avgBallSpeed": 32.8
}
```

---

### 6. Get Match Events

**Get all timestamped events for video navigation**

```http
GET /api/matches/{id}/events
```

**Response:**
```json
[
  {
    "id": "event1",
    "timestampMs": 83500,
    "timestampSeries": [83367, 83500, 83633],
    "frameSeries": [2505, 2510, 2515],
    "type": "PLAY_OF_THE_GAME",
    "title": "Play of the Game",
    "description": "Epic 15-shot rally",
    "player": null,
    "importance": 10,
    "metadata": {
      "rallyLength": 15,
      "shotSpeed": 45.2,
      "shotType": "FOREHAND",
      "frameNumber": 2510,
      "eventWindow": {
        "preMs": 133,
        "postMs": 400
      },
      "scoreAfter": {
        "player1": 8,
        "player2": 7
      },
      "confidence": 0.92,
      "source": "MODEL"
    }
  },
  ...
]
```

**Event Types:**
- `PLAY_OF_THE_GAME` - Best rally
- `SCORE` - Point scored
- `MISS` - Error/miss
- `RALLY_HIGHLIGHT` - Notable rally
- `SERVE_ACE` - Ace serve
- `FASTEST_SHOT` - Fastest shot

---

### 7. Get Match Highlights

**Get curated highlights (references to events)**

```http
GET /api/matches/{id}/highlights
```

**Response:**
```json
{
  "playOfTheGame": "event1",
  "topRallies": ["event1", "event3", "event5"],
  "fastestShots": ["event7", "event9"],
  "bestServes": ["event11"]
}
```

**Usage:**
```kotlin
val highlights = apiService.getMatchHighlights(matchId)
// highlights.playOfTheGame is an event ID
// Use it to find the event in the events list
val playOfGameEvent = events.find { it.id == highlights.playOfTheGame }
```

---

### 8. Stream Video

**Get video file for playback**

```http
GET /api/matches/{id}/video
```

**Response:** Binary video data (video/mp4)

**Headers:**
- `Content-Type: video/mp4`
- `Content-Disposition: inline; filename="match.mp4"`

**Usage in Android:**
```kotlin
// Don't call API directly, use ExoPlayer with URL
val videoUrl = "http://10.0.2.2:8080/api/matches/$matchId/video"
val mediaItem = MediaItem.fromUri(videoUrl)
player.setMediaItem(mediaItem)
player.prepare()
player.play()
```

---

### 9. Delete Match

**Delete a match and its video file**

```http
DELETE /api/matches/{id}
```

**Response:** 204 No Content

**Usage:**
```kotlin
apiService.deleteMatch(matchId)
// Match and video are permanently deleted
```

---

## Error Responses

All endpoints return standard error format:

```json
{
  "message": "Match not found: xyz789"
}
```

**Common HTTP Status Codes:**
- `200 OK` - Success
- `202 Accepted` - Upload accepted
- `204 No Content` - Delete success
- `400 Bad Request` - Invalid input
- `404 Not Found` - Match doesn't exist
- `500 Internal Server Error` - Server error

---

## Testing with cURL

### Upload Video
```bash
curl -X POST http://localhost:8080/api/matches/upload \
  -F "video=@/path/to/video.mp4"
```

### List Matches
```bash
curl http://localhost:8080/api/matches
```

### Get Match Details
```bash
curl http://localhost:8080/api/matches/abc123
```

### Get Events
```bash
curl http://localhost:8080/api/matches/abc123/events
```

### Check Status
```bash
curl http://localhost:8080/api/matches/abc123/status
```

### Stream Video
```bash
curl http://localhost:8080/api/matches/abc123/video \
  --output match.mp4
```

### Delete Match
```bash
curl -X DELETE http://localhost:8080/api/matches/abc123
```

---

## Implementation Status

| Endpoint | Defined in API Service | Used in Repository | Tested |
|----------|----------------------|-------------------|--------|
| POST /upload | ✅ | ✅ | ⏳ |
| GET /matches | ✅ | ✅ | ⏳ |
| GET /matches/{id} | ✅ | ✅ | ⏳ |
| GET /matches/{id}/status | ✅ | ✅ | ⏳ |
| GET /matches/{id}/statistics | ✅ | ✅ | ⏳ |
| GET /matches/{id}/events | ✅ | ✅ | ⏳ |
| GET /matches/{id}/highlights | ✅ | ✅ | ⏳ |
| GET /matches/{id}/video | ✅ | ✅ | ⏳ |
| DELETE /matches/{id} | ✅ | ✅ | ⏳ |

---

## Next Steps

1. **Test Upload**: Implement file picker and test video upload
2. **Display Matches**: Show list of matches in MatchListScreen
3. **Show Details**: Display statistics and events in MatchDetailScreen
4. **Video Player**: Integrate ExoPlayer with event timeline
5. **Error Handling**: Add user-friendly error messages

---

**Last Updated:** October 2025  
**Backend Version:** Spring Boot 3.x  
**API Version:** v1
