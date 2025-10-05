# Model Integration Guide

## Overview

This guide explains how to integrate the YOLOv11-based ping pong ball detection model (trained on the OpenTTGames dataset) into the AllanAI backend for automated match analysis. The backend now persists multi-timestamp timelines, frame series, and detection metadata, so the model output you generate should include these richer structures wherever possible.

## Model Background

### Research Foundation
- **Dataset**: [OpenTTGames Dataset](https://lab.osai.ai/) - 120fps HD table tennis videos with annotations
- **Architecture**: YOLOv11 (trained via [ping-pong-deep-learning](https://github.com/ccs-cs1l-f24/ping-pong-deep-learning))
- **Capabilities**: 
  - Ball position detection (x, y coordinates per frame)
  - Event detection (bounces, net hits, serves)
    - Frame-level annotations with bounding boxes
    - Optional multi-timestamp/frame series aligned to the video FPS
- **Citation**: TTNet research paper (Voeikov et al., CVPR 2020)

### Training Details
The model was trained on:
- **Training data**: 5 games (10-25 min each, 4271 annotated events)
- **Test data**: 7 short validation videos
- **Annotations**: Ball coordinates, event markers, semantic segmentation
- **Format**: YOLO format with normalized bounding boxes (class_id, x_center, y_center, width, height)

## Integration Steps

### 1. Obtain the Trained Model

#### Option A: Train Your Own Model
```bash
# Clone the training repository
git clone https://github.com/ccs-cs1l-f24/ping-pong-deep-learning.git
cd ping-pong-deep-learning

# Install dependencies
pip install -r requirements.txt
# For GPU support:
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu124

# Download OpenTTGames dataset
cd prepare_dataset
python download_dataset.py
python unzip.py

# Extract images and labels
python extract_ball_images_and_labels.py

# Train the model (generates best.pt)
cd ../src
python train.py

# Model will be saved as: train/weights/best.pt
```

#### Option B: Request Pre-trained Weights
Contact the repository maintainers or check for releases containing `best.pt` weights file.

### 2. Deploy the Model

#### 2a. Python Inference Service (Recommended)

Create a separate Python microservice that your Java backend can call:

```python
# model_service.py
from ultralytics import YOLO
import json
import sys
import cv2

def analyze_video(video_path, output_json_path):
    """
    Analyze a ping pong video and output ball detections + events.
    
    Args:
        video_path: Path to input video file
        output_json_path: Path to write JSON results
    """
    model = YOLO("best.pt")  # Load trained model
    cap = cv2.VideoCapture(video_path)
    
    if not cap.isOpened():
        raise ValueError(f"Cannot open video: {video_path}")
    
    fps = cap.get(cv2.CAP_PROP_FPS)
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    
    results = {
        "fps": fps,
        "events": [],
        "shots": [],
        "statistics": {
            "totalFrames": frame_count,
            "ballDetections": 0
        }
    }
    
    frame_idx = 0
    prev_ball_pos = None
    
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        
        # Run inference
        predictions = model.predict(source=frame, conf=0.25, verbose=False)
        
        for prediction in predictions:
            for box in prediction.boxes:
                # Extract ball position
                x_min, y_min, x_max, y_max = map(float, box.xyxy[0])
                x_center = (x_min + x_max) / 2.0
                y_center = (y_min + y_max) / 2.0
                confidence = float(box.conf[0])
                
                # Calculate timestamp
                timestamp_ms = (frame_idx / fps) * 1000.0
                
                results["statistics"]["ballDetections"] += 1
                
                # Detect events based on ball position changes
                if prev_ball_pos:
                    # Detect bounce (significant Y-axis change)
                    y_diff = abs(y_center - prev_ball_pos["y"])
                    if y_diff > 50:  # Threshold for bounce detection
                        results["events"].append({
                            "frame": frame_idx,
                            "timestampMs": timestamp_ms,
                            "type": "bounce",
                            "label": "Ball Bounce",
                            "confidence": confidence,
                            "ballTrajectory": [
                                [prev_ball_pos["x"], prev_ball_pos["y"]],
                                [x_center, y_center]
                            ]
                        })
                
                # Record shot
                results["shots"].append({
                    "frame": frame_idx,
                    "timestampMs": timestamp_ms,
                    "ballX": x_center,
                    "ballY": y_center,
                    "confidence": confidence
                })
                
                prev_ball_pos = {"x": x_center, "y": y_center}
        
        frame_idx += 1
    
    cap.release()
    
    # Write results
    with open(output_json_path, 'w') as f:
        json.dump(results, f, indent=2)
    
    return results

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python model_service.py <video_path> <output_json>")
        sys.exit(1)
    
    video_path = sys.argv[1]
    output_json = sys.argv[2]
    
    analyze_video(video_path, output_json)
    print(f"Analysis complete. Results written to {output_json}")
```

#### 2b. Configure Backend to Call Python Service

Update `application.properties`:

```properties
# Model Configuration
allanai.model.command=python
allanai.model.script-path=C:/path/to/model_service.py
allanai.model.output-dir=storage/model-output
allanai.model.pre-event-frames=4
allanai.model.post-event-frames=12
allanai.model.confidence-threshold=0.25
allanai.model.fallback-fps=120.0
```

### 3. Update ModelEventDetectionService

Your existing `ModelEventDetectionService` is already configured to execute external commands. Ensure it properly handles the Python script:

```java
// In ModelProperties.java - already configured
// Command will be: python C:/path/to/model_service.py {videoPath} {outputPath}
```

The service will:
1. Execute: `python model_service.py <video_path> <output_json>`
2. Read the JSON output containing detections
3. Parse into `ModelInferenceResult` with events, shots, statistics, timeline series, and detection metadata
4. Return to `MatchProcessingService` for integration

### 3a. Backend Persistence Contract

The backend normalizes model output and stores it in MongoDB using the `MatchDocument` structure described in `AIGuidelines/BackendArchitecture.md`. Keep the following expectations in mind when emitting JSON:

- **Timeline fidelity** – Provide `timestampMs` *and* the optional `timestampSeries`/`frameSeries` arrays for events and shots. The service deduplicates and sorts these, preserving additional context frames around highlights even when fallback windows are used.
- **Detection metadata** – Include a `detections` array with bounding boxes (frame number, x/y/width/height, confidence) whenever the model tracks the ball in that interval. Missing detections trigger synthetic placeholders.
- **Processing summary** – Accurate confidence values and explicit timestamps allow the backend to set `processingSummary.primarySource` to `MODEL` with supporting notes. Missing or incomplete outputs now cause the processing run to fail rather than falling back to heuristics, so model payloads must be complete.
- **Highlight references** – Highlights reference events via `HighlightRef` objects (event ID + timestamps) that the backend derives from the processed event list. Accurate timestamps and ordering ensure highlight playlists and cross-links stay consistent.

### 4. Expected JSON Output Format

The Python service should output JSON matching your `ModelInferenceResult` structure. The backend will transform this into the final MongoDB schema by:
- Generating `_id`, `title`, `description` for events
- Building `eventWindow` from `preEventFrames`/`postEventFrames`
- Creating `highlights` and `processingSummary` structures
- Adding top-level match metadata (`createdAt`, `status`, etc.)

**Python model output format:**

```json
{
  "fps": 120.0,
  "events": [
    {
      "frame": 186,
      "timestampMs": 1550.0,
      "timestampSeries": [1480.0, 1550.0, 1620.0],
      "frameSeries": [178, 186, 194],
      "type": "bounce",
      "label": "Ball Bounce",
      "confidence": 0.92,
      "player": 1,
      "importance": 7,
      "rallyLength": 5,
      "shotSpeed": 45.2,
      "shotType": "forehand",
      "ballTrajectory": [[450.2, 320.5], [455.1, 280.3]],
      "preEventFrames": 4,
      "postEventFrames": 12,
      "frameNumber": 186,
      "detections": [
        {
          "frameNumber": 186,
          "x": 452.1,
          "y": 278.4,
          "width": 32.0,
          "height": 32.0,
          "confidence": 0.92
        }
      ]
    }
  ],
  "shots": [
    {
      "frame": 185,
      "timestampMs": 1541.67,
      "timestampSeries": [1472.0, 1541.67],
      "frameSeries": [177, 185],
      "player": 1,
      "speed": 45.2,
      "accuracy": 85.0,
      "shotType": "forehand",
      "result": "in",
      "confidence": 0.89,
      "detections": [
        {
          "frameNumber": 185,
          "x": 448.0,
          "y": 300.5,
          "width": 30.0,
          "height": 30.0,
          "confidence": 0.88
        }
      ]
    }
  ],
  "statistics": {
    "player1Score": 11,
    "player2Score": 8,
    "totalRallies": 42,
    "avgRallyLength": 5.8,
    "avgBallSpeed": 38.5,
    "maxBallSpeed": 65.3
  }
}
```

**Key differences from MongoDB schema:**
- **Events**: Python outputs flat structure with `rallyLength`/`shotSpeed` at top level; backend moves these into `metadata` during transformation
- **Shots**: Python must include `timestampMs` alongside `frame` (calculate as `frame / fps * 1000`)
- **Backend-generated fields**: `_id`, `title`, `description`, `eventWindow`, `highlights`, `processingSummary` are created by `MatchProcessingService`
- **Top-level match fields**: `createdAt`, `status`, `videoPath`, etc. are managed by the backend service layer

### 5. Testing the Integration

#### Test the Python Service Standalone
```bash
# Test with a sample video
python model_service.py path/to/test_video.mp4 output.json
cat output.json
```

#### Test Backend Integration
```bash
# Upload a match through the API
curl -X POST -F "video=@test_match.mp4" http://localhost:8080/api/matches/upload

# Check processing status
curl http://localhost:8080/api/matches/{id}/status

# Retrieve analyzed events
curl http://localhost:8080/api/matches/{id}/events
```

### 6. Failure Handling

If the model command fails, yields no events, or produces an invalid result file, the backend marks the match as `FAILED` and surfaces the error message. There is no longer a heuristic fallback pipeline; ensure the configured command always generates a valid inference JSON or supply the file before invoking processing.

## Model Capabilities & Limitations

### What the Model CAN Do
✅ Detect ball position at each frame (x, y coordinates)  
✅ Identify bounce events when ball hits table  
✅ Track ball trajectory across frames  
✅ Provide confidence scores for detections  
✅ Process 120fps HD video efficiently  

### What the Model CANNOT Do (Backend Must Infer)
❌ Identify which player hit the ball (use trajectory heuristics)  
❌ Classify shot types (forehand, backhand, smash) - use velocity/angle  
❌ Detect serves vs rallies automatically - use event sequencing  
❌ Calculate scores - derive from bounce patterns and court position  
❌ Measure ball speed directly - calculate from frame-to-frame displacement  

### Recommended Enhancements

To improve the model integration:

1. **Shot Classification**: Analyze ball trajectory vectors to infer shot types
2. **Player Attribution**: Use ball position relative to court halves
3. **Speed Calculation**: Measure pixel displacement between frames + real-world calibration
4. **Rally Segmentation**: Group bounces into rallies based on temporal gaps
5. **Score Inference**: Track alternating successful returns until fault detected

## Performance Considerations

- **Processing Speed**: YOLOv11 can process ~120fps video at 30-60 FPS on modern GPUs
- **Memory**: Model requires ~50MB for weights, 2-4GB GPU memory during inference
- **Accuracy**: ~90%+ ball detection rate on OpenTTGames test set
- **Latency**: Expect 2-5 seconds per minute of 120fps video on GPU

## Troubleshooting

### Model Not Found
```
Error: No such file: best.pt
```
**Solution**: Ensure `best.pt` is in the working directory or update `YOLO("best.pt")` path

### Low Detection Rate
```
Warning: Only 12% of frames have ball detections
```
**Solution**: Lower confidence threshold from 0.25 to 0.15 or retrain with more data

### Python Command Fails
```
Error: Cannot run program "python"
```
**Solution**: 
- Update `allanai.model.command=python3` or use full path: `C:\Python311\python.exe`
- Verify Python installation: `python --version`

### GPU Not Used
```
INFO: Running on CPU (slow)
```
**Solution**: Install CUDA toolkit and PyTorch with GPU support

## Additional Resources

- [OpenTTGames Dataset](https://lab.osai.ai/)
- [Training Repository](https://github.com/ccs-cs1l-f24/ping-pong-deep-learning)
- [TTNet Research Paper](https://arxiv.org/pdf/2004.09927)
- [YOLOv11 Documentation](https://docs.ultralytics.com/)

## Support

For issues with:
- **Model training**: Open issue on [ping-pong-deep-learning](https://github.com/ccs-cs1l-f24/ping-pong-deep-learning)
- **Backend integration**: Check `ModelEventDetectionService` logs and verify JSON output format
- **Performance**: Consider GPU acceleration, reduce video resolution, or batch processing
