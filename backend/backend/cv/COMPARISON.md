# Code Comparison: model_service.py vs demo_video.py

## Core Inference Loop

### demo_video.py (Reference Implementation)
```python
# Load model
model = YOLO("best.pt")

# Process video
cap = cv2.VideoCapture(video_path)
while True:
    ret, frame = cap.read()
    if not ret:
        break
    
    # YOLO prediction
    results = model.predict(source=frame, conf=0.25, verbose=False)
    for result in results:
        for box in result.boxes:
            x_min, y_min, x_max, y_max = map(int, box.xyxy[0])
            confidence = box.conf[0]
            # Use detections...
```

### model_service.py (Updated Implementation)
```python
# Load model with GPU support
device = 'cuda' if torch.cuda.is_available() else 'cpu'
model, _ = _load_yolo_model()

# Process video
cap = cv2.VideoCapture(video_path)
while cap.isOpened():
    ret, frame = cap.read()
    if not ret:
        break
    
    # YOLO prediction with GPU acceleration
    results = model.predict(source=frame, conf=confidence_threshold, verbose=False, device=device)
    primary_ball = _select_primary_ball(results, frame_idx)
    
    if primary_ball:
        # Event detection and JSON output generation
        events = detector.add_position(primary_ball)
        # ... rest of processing
```

## Key Differences

### What's the Same
1. **Model loading**: Both use `YOLO("best.pt")`
2. **Frame reading**: Both use `cv2.VideoCapture` and read frame-by-frame
3. **Inference call**: Both use `model.predict(source=frame, conf=0.25, verbose=False)`
4. **Box extraction**: Both use `box.xyxy[0]` and `box.conf[0]` directly

### What's Different (Enhancements)
1. **GPU Acceleration**: model_service.py adds `device` parameter for GPU usage
2. **Event Detection**: model_service.py adds EventDetector for bounce/shot detection
3. **JSON Output**: model_service.py generates structured JSON with statistics
4. **Highest Confidence**: model_service.py picks the best detection per frame
5. **Progress Tracking**: model_service.py adds progress indicators

### What Was Removed (Simplifications)
1. ❌ Complex label filtering (not needed for single-class model)
2. ❌ Multiple fallback extraction methods (YOLO output is consistent)
3. ❌ Class name/ID validation (model only detects ping pong balls)
4. ❌ Environment variable class configuration (unnecessary complexity)

## Performance Impact

### Before Refactoring
- ⚠️ Complex extraction logic could cause misses or false negatives
- ⚠️ Multiple validation steps added overhead
- ⚠️ Misaligned with training methodology

### After Refactoring
- ✅ Direct extraction matches training inference
- ✅ Simpler code = fewer bugs
- ✅ GPU acceleration for faster processing
- ✅ Proven approach from demo_video.py

## Validation Checklist

- [x] Code compiles without errors
- [x] Matches demo_video.py inference logic
- [x] Retains GPU acceleration
- [x] Maintains JSON output format
- [x] Event detection logic intact
- [ ] Test on sample videos (TODO)
- [ ] Compare accuracy with demo_video.py (TODO)
- [ ] Verify backend integration (TODO)
