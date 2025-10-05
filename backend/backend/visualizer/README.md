# 🏓 Ping Pong Match Visualizer

A real-time video analysis visualizer for ping pong matches with synchronized statistics, event tracking, and ball position overlay.

## Features

### 📹 Video Playback
- Full video player with standard controls (play, pause, seek)
- Variable playback speed (0.25x - 2x)
- Real-time synchronized overlays

### 🎯 Ball Tracking Overlay
- Visual ball position markers with confidence indicators
- Ball trajectory visualization
- Bounding box display for detected balls

### 📊 Live Statistics
- **Match Score**: Real-time player scores
- **Rally Statistics**: Total rallies, average rally length
- **Speed Metrics**: Maximum and average ball speed
- **Current Shot Info**: Speed, type, accuracy, and result

### 🎬 Event Timeline
- Visual timeline showing all match events
- Color-coded event types:
  - 🟢 Green: Score
  - 🔴 Red: Miss
  - 🟠 Orange: Rally Highlight
  - 🔴 Pink: Fastest Shot
  - 🟣 Purple: Serve Ace
  - 🟡 Gold: Play of the Game
- Click any event to jump to that moment

### 🌟 Highlights
- Play of the Game
- Top Rallies
- Fastest Shots
- Best Serves
- One-click navigation to highlights

### 🔔 Event Notifications
- Real-time event popups during playback
- Non-intrusive notifications for key moments

## Usage

### Quick Start

1. **Open the Visualizer**
   ```
   Open index.html in a modern web browser
   ```

2. **Load Your Files**
   - **Video**: Select your match video (.mp4)
   - **Log**: Select `log.json` (contains shots and statistics)
   - **Events**: Select `events.json` (contains match events)
   - **Highlights**: Select `highlights.json` (contains highlight references)

3. **Click "Load Visualizer"**
   - The upload dialog will close
   - Video will load with all synchronized data

### Controls

#### Video Controls
- **Play/Pause**: Click the play button or spacebar
- **Seek**: Drag the progress bar or click on the timeline
- **Speed**: Select playback speed from dropdown (0.25x - 2x)

#### Navigation
- **Events Timeline**: Click any marker to jump to that event
- **Events List**: Click any event card to jump to that moment
- **Highlights**: Click any highlight to jump to that moment

### File Formats

#### log.json
```json
{
  "statistics": {
    "player1Score": 5,
    "player2Score": 5,
    "totalRallies": 2,
    "avgRallyLength": 0.0,
    "maxBallSpeed": 11.4,
    "avgBallSpeed": 8.0
  },
  "shots": [
    {
      "frame": 64,
      "timestampMs": 533,
      "player": 1,
      "speed": 8.5,
      "accuracy": 85.2,
      "shotType": "RALLY",
      "result": "IN",
      "detections": [
        {
          "frameNumber": 64,
          "x": 100.0,
          "y": 200.0,
          "width": 20.0,
          "height": 20.0,
          "confidence": 0.95
        }
      ]
    }
  ]
}
```

#### events.json
```json
[
  {
    "id": "uuid",
    "timestampMs": 533,
    "type": "SCORE",
    "title": "Point Scored",
    "description": "Ball Bounce",
    "player": 1,
    "importance": 5,
    "metadata": {
      "ballTrajectory": [[x1, y1], [x2, y2], ...]
    }
  }
]
```

#### highlights.json
```json
{
  "playOfTheGame": {
    "eventId": "uuid",
    "timestampMs": 533
  },
  "topRallies": [...],
  "fastestShots": [...],
  "bestServes": [...]
}
```

## Browser Compatibility

- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Edge 90+

## Keyboard Shortcuts

- **Space**: Play/Pause
- **Arrow Left**: Rewind 5 seconds
- **Arrow Right**: Forward 5 seconds
- **Arrow Up**: Increase speed
- **Arrow Down**: Decrease speed

## Tips

1. **Performance**: For best performance with large files, use Chrome or Edge
2. **Accuracy**: The ball overlay shows detection confidence - higher opacity = higher confidence
3. **Events**: Hover over timeline markers to see event details
4. **Mobile**: Best viewed on desktop/laptop for full feature access

## Troubleshooting

### Video won't play
- Ensure your video is in MP4 format with H.264 codec
- Check browser console for codec support messages

### Events not syncing
- Verify timestamps in JSON files are in milliseconds
- Check that FPS matches between video and log data

### Overlay not showing
- Ensure detection coordinates are within video dimensions
- Check that canvas is properly sized (inspect element)

## Development

### File Structure
```
visualizer/
├── index.html          # Main HTML structure
├── styles.css          # Styling and layout
├── visualizer.js       # Core visualization logic
├── README.md          # This file
├── events.json        # Sample events data
├── highlights.json    # Sample highlights data
└── log.json          # Sample log data
```

### Extending the Visualizer

To add custom event types, update:
1. `styles.css`: Add color classes for new event types
2. `visualizer.js`: Update `buildTimeline()` to handle new types
3. Update legend/documentation as needed

## Credits

Built for Allan AI Ping Pong Analysis System
