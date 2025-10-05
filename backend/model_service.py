"""
Ping Pong Match Analysis Service
=================================

This script provides inference using a YOLOv11 model trained on the OpenTTGames dataset
to detect ping pong ball positions and infer game events from video footage.

Usage:
    python model_service.py <video_path> <output_json_path>

Requirements:
    - ultralytics (pip install ultralytics)
    - opencv-python (pip install opencv-python)
    - torch with CUDA support (optional, for GPU acceleration)

Model:
    - Place trained 'best.pt' model weights in the same directory as this script
    - Model trained on OpenTTGames dataset (https://lab.osai.ai/)
    - Based on YOLOv11 architecture

Output Format:
    JSON file containing:
    - fps: Video frame rate
    - events: List of detected game events (bounces, serves, etc.)
    - shots: Frame-by-frame ball detections
    - statistics: Aggregated match statistics
"""

from ultralytics import YOLO
import json
import sys
import cv2
import os
from typing import Dict, List, Tuple, Optional, Set, Iterable
from dataclasses import dataclass
from collections import deque


@dataclass
class BallPosition:
    """Ball position at a specific frame."""
    x: float
    y: float
    confidence: float
    frame: int
    bbox: Optional[Tuple[float, float, float, float]] = None


DEFAULT_BALL_LABELS: Set[str] = {
    "sports ball",
    "ball",
    "ping pong ball",
    "ping-pong-ball",
    "ping_pong_ball",
    "table tennis ball",
    "table-tennis-ball",
    "table_tennis_ball"
}


def _load_yolo_model() -> Tuple[YOLO, str]:
    """Load YOLO weights, falling back to a general model when custom weights are missing."""
    override_path = os.environ.get("YOLO_MODEL_WEIGHTS")
    candidates: List[Optional[str]] = []
    if override_path:
        candidates.append(override_path)

    script_dir = os.path.dirname(__file__)
    candidates.append(os.path.join(script_dir, "best.pt"))

    for candidate in candidates:
        if candidate and os.path.exists(candidate):
            print(f"Loading model from {candidate}...")
            return YOLO(candidate), candidate

    fallback = os.environ.get("YOLO_FALLBACK_MODEL", "yolov8n.pt")
    print(f"Primary model weights not found. Falling back to {fallback}...")
    return YOLO(fallback), fallback


def _build_allowed_labels(model: YOLO) -> Set[str]:
    labels = {label.lower() for label in DEFAULT_BALL_LABELS}
    names = getattr(model, "names", None)
    name_iterable: Iterable[str] = ()
    if isinstance(names, dict):
        name_iterable = names.values()
    elif isinstance(names, (list, tuple)):
        name_iterable = names

    for name in name_iterable:
        if isinstance(name, str) and name:
            labels.add(name.lower())

    env_labels = os.environ.get("BALL_LABELS")
    if env_labels:
        labels.update(label.strip().lower() for label in env_labels.split(',') if label.strip())
    return labels


def _parse_allowed_ids() -> Set[int]:
    env_ids = os.environ.get("BALL_CLASS_IDS")
    if not env_ids:
        return set()
    ids: Set[int] = set()
    for raw in env_ids.split(','):
        raw = raw.strip()
        if not raw:
            continue
        try:
            ids.add(int(raw))
        except ValueError:
            continue
    return ids


def _resolve_names_map(model: YOLO, prediction) -> Dict[int, str]:
    for source in (getattr(prediction, "names", None), getattr(model, "names", None)):
        if not source:
            continue
        if isinstance(source, dict):
            return {int(k): str(v) for k, v in source.items()}
        if isinstance(source, (list, tuple)):
            return {idx: str(name) for idx, name in enumerate(source)}
    return {}


def _extract_xyxy(box) -> Optional[Tuple[float, float, float, float]]:
    coords = getattr(box, "xyxy", None)
    if coords is None:
        return None
    try:
        first = coords[0]
    except (TypeError, IndexError):
        first = coords

    if hasattr(first, "tolist"):
        values = first.tolist()
    elif isinstance(first, (list, tuple)):
        values = list(first)
    else:
        values = [float(first)] if first is not None else []

    if len(values) < 4:
        return None
    return float(values[0]), float(values[1]), float(values[2]), float(values[3])


def _extract_confidence(box) -> Optional[float]:
    if hasattr(box, "conf"):
        conf = box.conf
    elif hasattr(box, "confidence"):
        conf = getattr(box, "confidence")
    else:
        return None

    if isinstance(conf, (list, tuple)):
        conf = conf[0]
    if hasattr(conf, "item"):
        conf = conf.item()
    try:
        return float(conf)
    except (TypeError, ValueError):
        return None


def _extract_class(box, names_map: Dict[int, str]) -> Tuple[Optional[int], Optional[str]]:
    cls_id = None
    if hasattr(box, "cls"):
        cls = box.cls
        if isinstance(cls, (list, tuple)):
            cls = cls[0]
        if hasattr(cls, "item"):
            cls = cls.item()
        try:
            cls_id = int(cls)
        except (TypeError, ValueError):
            cls_id = None
    elif hasattr(box, "class_id"):
        try:
            cls_id = int(getattr(box, "class_id"))
        except (TypeError, ValueError):
            cls_id = None

    cls_name = None
    if cls_id is not None:
        cls_name = names_map.get(cls_id)
    return cls_id, cls_name


def _is_ball_detection(cls_id: Optional[int], cls_name: Optional[str],
                       allowed_labels: Set[str], allowed_ids: Set[int], total_classes: int) -> bool:
    if cls_id is not None and cls_id in allowed_ids:
        return True
    if cls_name and cls_name.lower() in allowed_labels:
        return True
    if not allowed_labels and not allowed_ids:
        return True
    if total_classes == 1 and cls_id is not None:
        return True
    return False


def _select_primary_ball(predictions, model: YOLO, frame_idx: int, confidence_threshold: float,
                         allowed_labels: Set[str], allowed_ids: Set[int]) -> Optional[BallPosition]:
    primary: Optional[BallPosition] = None
    for prediction in predictions:
        boxes = getattr(prediction, "boxes", None)
        if boxes is None:
            continue
        names_map = _resolve_names_map(model, prediction)
        total_classes = len(names_map)
        for box in boxes:
            coords = _extract_xyxy(box)
            if coords is None:
                continue
            confidence = _extract_confidence(box)
            if confidence is None or confidence < confidence_threshold:
                continue
            cls_id, cls_name = _extract_class(box, names_map)
            if not _is_ball_detection(cls_id, cls_name, allowed_labels, allowed_ids, total_classes):
                continue
            x_min, y_min, x_max, y_max = coords
            candidate = BallPosition(
                x=(x_min + x_max) / 2.0,
                y=(y_min + y_max) / 2.0,
                confidence=confidence,
                frame=frame_idx,
                bbox=(x_min, y_min, x_max, y_max)
            )
            if primary is None or candidate.confidence > primary.confidence:
                primary = candidate
    return primary


class EventDetector:
    """Detects table tennis events from ball trajectory."""
    
    def __init__(self, fps: float, frame_height: int, frame_width: int, pixels_per_meter: float = 1000.0):
        self.fps = fps
        self.frame_height = frame_height
        self.frame_width = frame_width
        self.pixels_per_meter = pixels_per_meter  # Calibration for speed calculation
        self.history = deque(maxlen=30)  # Keep last 30 positions
        self.last_bounce_frame = -100
        self.rally_start_frame = 0
        self.rally_shot_count = 0
        
    def add_position(self, pos: BallPosition) -> List[Dict]:
        """Add a ball position and detect events."""
        self.history.append(pos)
        events = []
        
        if len(self.history) < 3:
            return events
        
        # Detect bounce (rapid Y-direction change)
        bounce_event = self._detect_bounce(pos)
        if bounce_event:
            events.append(bounce_event)
            
        # Detect fast shots
        fast_shot = self._detect_fast_shot(pos)
        if fast_shot:
            events.append(fast_shot)
            
        return events
    
    def _detect_bounce(self, current: BallPosition) -> Optional[Dict]:
        """Detect ball bounce on table."""
        if current.frame - self.last_bounce_frame < 10:
            return None  # Too soon after last bounce
            
        positions = list(self.history)
        if len(positions) < 5:
            return None
            
        # Check for Y-direction reversal (ball going up after going down)
        y_velocities = [positions[i+1].y - positions[i].y for i in range(len(positions)-1)]
        # Normalize bounce threshold to frame height
        bounce_threshold = max(5, self.frame_height * 0.01)  # 1% of frame height
        if len(y_velocities) >= 4:
            # Look for pattern: negative (going up) to positive (going down) to negative (bouncing back up)
            for i in range(len(y_velocities) - 2):
                if y_velocities[i] > bounce_threshold and y_velocities[i+1] > bounce_threshold and y_velocities[i+2] < -bounce_threshold:
                    # Detected bounce pattern
                    self.last_bounce_frame = current.frame
                    self.rally_shot_count += 1
                    # Calculate shot speed from trajectory
                    dx = abs(positions[-1].x - positions[-5].x)
                    dy = abs(positions[-1].y - positions[-5].y)
                    distance_pixels = (dx**2 + dy**2) ** 0.5
                    time_seconds = 4 / self.fps
                    speed_pixels_per_second = distance_pixels / time_seconds
                    # Use calibrated pixels_per_meter for speed
                    speed_mps = speed_pixels_per_second / self.pixels_per_meter * 3.6  # Convert to km/h
                    return {
                        "frame": current.frame,
                        "timestampMs": (current.frame / self.fps) * 1000.0,
                        "type": "bounce",
                        "label": "Ball Bounce",
                        "confidence": current.confidence,
                        "player": self._infer_player(current.x),
                        "importance": min(10, 5 + int(self.rally_shot_count / 2)),
                        "rallyLength": self.rally_shot_count,
                        "shotSpeed": round(speed_mps, 1),
                        "shotType": self._infer_shot_type(y_velocities[i]),
                        "ballTrajectory": [
                            [positions[-5].x, positions[-5].y],
                            [positions[-1].x, positions[-1].y]
                        ],
                        "preEventFrames": 4,
                        "postEventFrames": 12,
                        "frameNumber": current.frame
                    }
        return None
    
    def _detect_fast_shot(self, current: BallPosition) -> Optional[Dict]:
        """Detect unusually fast shots."""
        if len(self.history) < 5:
            return None
            
        positions = list(self.history)
        # Calculate velocity over last 5 frames
        dx = abs(positions[-1].x - positions[-5].x)
        dy = abs(positions[-1].y - positions[-5].y)
        distance = (dx**2 + dy**2) ** 0.5
        
        # If ball moved more than 1/3 of frame width in 5 frames, it's fast
        fast_shot_threshold = self.frame_width / 3
        if distance > fast_shot_threshold:
            speed_pixels_per_second = (distance / 5) * self.fps
            speed_mps = speed_pixels_per_second / self.pixels_per_meter * 3.6
            # Only report if significantly fast and not recently reported
            if speed_mps > 50 and current.frame - self.last_bounce_frame > 15:
                return {
                    "frame": current.frame,
                    "timestampMs": (current.frame / self.fps) * 1000.0,
                    "type": "fast_shot",
                    "label": "Fast Shot",
                    "confidence": current.confidence,
                    "player": self._infer_player(positions[-5].x),
                    "importance": 8,
                    "shotSpeed": round(speed_mps, 1),
                    "shotType": "smash" if dy > dx else "forehand",
                    "ballTrajectory": [
                        [positions[-5].x, positions[-5].y],
                        [positions[-1].x, positions[-1].y]
                    ],
                    "preEventFrames": 4,
                    "postEventFrames": 12,
                    "frameNumber": current.frame
                }
        return None
    
    def _infer_player(self, x: float) -> int:
        """Infer which player based on ball X position."""
        # Assume player 1 on left half, player 2 on right half
        return 1 if x < self.frame_width / 2 else 2
    
    def _infer_shot_type(self, y_velocity: float) -> str:
        """Infer shot type from ball trajectory."""
        if abs(y_velocity) > 30:
            return "smash"
        elif y_velocity < 0:
            return "topspin"
        else:
            return "forehand"
    
    def finish_rally(self) -> Optional[Dict]:
        """Called when rally ends to generate summary event."""
        if self.rally_shot_count > 0:
            event = {
                "type": "rally_end",
                "shotCount": self.rally_shot_count,
                "duration": (self.history[-1].frame - self.rally_start_frame) / self.fps if self.history else 0
            }
            self.rally_shot_count = 0
            self.rally_start_frame = self.history[-1].frame if self.history else 0
            return event
        return None


def analyze_video(video_path: str, output_json_path: str, confidence_threshold: float = 0.25) -> Dict:
    """
    Analyze a ping pong video and output ball detections + events.
    
    Args:
        video_path: Path to input video file
        output_json_path: Path to write JSON results
        confidence_threshold: Minimum confidence for ball detection (0.0-1.0)
    
    Returns:
        Dictionary containing analysis results
    """
    model, _ = _load_yolo_model()
    allowed_labels = _build_allowed_labels(model)
    allowed_ids = _parse_allowed_ids()
    
    print(f"Opening video: {video_path}")
    cap = cv2.VideoCapture(video_path)
    
    if not cap.isOpened():
        raise ValueError(f"Cannot open video: {video_path}")
    
    fps = cap.get(cv2.CAP_PROP_FPS)
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    frame_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    frame_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    
    print(f"Video properties: {frame_width}x{frame_height} @ {fps} fps, {frame_count} frames")
    
    results = {
        "fps": fps,
        "events": [],
        "shots": [],
        "statistics": {
            "totalFrames": frame_count,
            "ballDetections": 0,
            "player1Score": 0,
            "player2Score": 0,
            "totalRallies": 0,
            "avgRallyLength": 0.0,
            "avgBallSpeed": 0.0,
            "maxBallSpeed": 0.0
        }
    }
    
    # You may want to calibrate pixels_per_meter for your camera setup
    pixels_per_meter = 1000.0  # Default, adjust as needed for your production videos
    detector = EventDetector(fps, frame_height, frame_width, pixels_per_meter)
    frame_idx = 0
    no_ball_frames = 0
    speeds = []
    
    print("Processing frames...")
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        
        # Run inference
        predictions = model.predict(source=frame, conf=confidence_threshold, verbose=False)

        primary_ball = _select_primary_ball(
            predictions,
            model,
            frame_idx,
            confidence_threshold,
            allowed_labels,
            allowed_ids
        )

        ball_detected = primary_ball is not None

        if primary_ball:
            events = detector.add_position(primary_ball)
            for event in events:
                results["events"].append(event)
                if event.get("shotSpeed"):
                    speeds.append(event["shotSpeed"])

            results["statistics"]["ballDetections"] += 1

            detection_payload = []
            if primary_ball.bbox:
                x_min, y_min, x_max, y_max = primary_ball.bbox
                detection_payload.append({
                    "frameNumber": frame_idx,
                    "x": float(x_min),
                    "y": float(y_min),
                    "width": float(max(x_max - x_min, 0.0)),
                    "height": float(max(y_max - y_min, 0.0)),
                    "confidence": float(primary_ball.confidence)
                })
            else:
                detection_payload.append({
                    "frameNumber": frame_idx,
                    "confidence": float(primary_ball.confidence)
                })

            timestamp_ms = (frame_idx / fps) * 1000.0 if fps else None
            shot_payload = {
                "frame": frame_idx,
                "player": detector._infer_player(primary_ball.x),
                "speed": speeds[-1] if speeds else 30.0,
                "accuracy": 85.0,
                "shotType": "forehand",
                "result": "in",
                "confidence": float(primary_ball.confidence),
                "detections": detection_payload,
                "frameSeries": [frame_idx]
            }
            if timestamp_ms is not None:
                shot_payload["timestampSeries"] = [timestamp_ms]
            results["shots"].append(shot_payload)
        
        if not ball_detected:
            no_ball_frames += 1
            # If ball not detected for 30 frames (likely end of rally)
            if no_ball_frames > 30:
                rally_event = detector.finish_rally()
                if rally_event:
                    results["statistics"]["totalRallies"] += 1
                no_ball_frames = 0
        else:
            no_ball_frames = 0
        
        frame_idx += 1
        
        # Progress indicator
        if frame_idx % 100 == 0:
            progress = (frame_idx / frame_count) * 100
            print(f"Progress: {progress:.1f}% ({frame_idx}/{frame_count} frames)")
    
    cap.release()
    
    # Calculate statistics
    if results["events"]:
        bounce_events = [e for e in results["events"] if e["type"] == "bounce"]
        if bounce_events:
            results["statistics"]["totalRallies"] = max(1, len(bounce_events) // 4)
            rally_lengths = [e["rallyLength"] for e in bounce_events if "rallyLength" in e]
            if rally_lengths:
                results["statistics"]["avgRallyLength"] = round(sum(rally_lengths) / len(rally_lengths), 1)
    
    if speeds:
        results["statistics"]["avgBallSpeed"] = round(sum(speeds) / len(speeds), 1)
        results["statistics"]["maxBallSpeed"] = round(max(speeds), 1)
    
    # Estimate scores (simplified - count bounces per side)
    player1_bounces = len([e for e in results["events"] if e.get("player") == 1 and e["type"] == "bounce"])
    player2_bounces = len([e for e in results["events"] if e.get("player") == 2 and e["type"] == "bounce"])
    results["statistics"]["player1Score"] = player1_bounces // 2
    results["statistics"]["player2Score"] = player2_bounces // 2
    
    print(f"\nAnalysis complete:")
    print(f"  - Ball detected in {results['statistics']['ballDetections']} frames")
    print(f"  - {len(results['events'])} events detected")
    print(f"  - {len(results['shots'])} shots recorded")
    
    # Write results
    with open(output_json_path, 'w') as f:
        json.dump(results, f, indent=2)
    
    print(f"Results written to {output_json_path}")
    return results


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python model_service.py <video_path> <output_json> [confidence_threshold]")
        print("Example: python model_service.py match.mp4 output.json 0.25")
        sys.exit(1)
    
    video_path = sys.argv[1]
    output_json = sys.argv[2]
    confidence = float(sys.argv[3]) if len(sys.argv) > 3 else 0.25
    
    try:
        analyze_video(video_path, output_json, confidence)
        sys.exit(0)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)
