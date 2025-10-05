# Model Service Refactoring Notes

## Date: October 4, 2025

## Summary
Refactored `model_service.py` to match the proven inference approach from the ping-pong-deep-learning project's `demo_video.py`.

## Key Changes

### 1. Simplified Ball Detection Logic
**Before:** Complex multi-layer extraction logic with:
- `_resolve_names_map()`
- `_extract_xyxy()` with extensive fallback handling
- `_extract_confidence()` with multiple checks
- `_extract_class()` for class ID extraction
- `_is_ball_detection()` for filtering
- `_build_allowed_labels()` for label management
- `_parse_allowed_ids()` for ID parsing

**After:** Direct extraction matching demo_video.py:
```python
def _select_primary_ball(results, frame_idx: int) -> Optional[BallPosition]:
    for result in results:
        for box in result.boxes:
            x_min, y_min, x_max, y_max = map(float, box.xyxy[0])
            confidence = float(box.conf[0])
            # Create BallPosition with highest confidence
```

### 2. Removed Unnecessary Complexity
- Removed `DEFAULT_BALL_LABELS` set (not needed for single-class model)
- Removed label filtering logic (model only detects ping pong balls)
- Removed class ID parsing from environment variables
- Simplified to match the working demo_video.py approach

### 3. Retained GPU Acceleration
- Still uses `torch.cuda.is_available()` to detect GPU
- Passes `device` parameter to `model.predict()`
- Falls back to CPU if CUDA not available

### 4. Direct Frame Processing
Following demo_video.py pattern:
```python
# Run inference directly on frame
results = model.predict(source=frame, conf=confidence_threshold, verbose=False, device=device)

# Extract ball position simply
primary_ball = _select_primary_ball(results, frame_idx)
```

## Why This Matters

### Previous Issues
1. **Overcomplicated extraction**: The previous code tried to handle every possible YOLO output format, which introduced bugs
2. **Unnecessary filtering**: Filtering by class names/IDs when the model only detects one class (ping pong ball)
3. **Misaligned with training**: The model was trained using simple extraction logic, but inference used complex extraction

### Current Approach
1. **Matches training methodology**: Uses the same simple extraction as the demo scripts
2. **Proven working code**: Based directly on demo_video.py which is tested and working
3. **Single-class model**: The best.pt model is trained specifically for ping pong balls, so no class filtering needed
4. **GPU acceleration**: Retained the performance benefits while simplifying the logic

## Testing Recommendations

1. Run on test videos from OpenTTGames dataset
2. Compare detection accuracy with demo_video.py output
3. Verify GPU acceleration is working (check CUDA memory usage)
4. Validate JSON output format matches backend expectations

## References

- Original project: https://github.com/ccs-cs1l-f24/ping-pong-deep-learning
- Dataset: OpenTTGames (https://lab.osai.ai/)
- Key reference files:
  - `ping-pong-deep-learning/src/demo_video.py` - Simple video inference
  - `ping-pong-deep-learning/src/demo.py` - Image inference with ground truth comparison
  - `ping-pong-deep-learning/README.md` - Setup instructions including GPU support
