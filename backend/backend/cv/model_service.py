"""
Ping Pong Match Analysis Service
=================================

This script provides inference using a YOLOv11 model trained on the OpenTTGames dataset
to detect ping pong ball positions and infer game events from video footage.

Based on the ping-pong-deep-learning project: https://github.com/ccs-cs1l-f24/ping-pong-deep-learning
Uses the same inference approach as demo_video.py for accurate ball detection.

Usage:
    python model_service.py <video_path> <output_json_path>

Requirements:
    - ultralytics (pip install ultralytics)
    - opencv-python (pip install opencv-python)
    - torch with CUDA support (optional, for GPU acceleration)
      Install with: pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu124

Model:
    - Place trained 'best.pt' model weights in the same directory as this script
    - Model trained on OpenTTGames dataset (https://lab.osai.ai/)
    - Based on YOLOv11 architecture
    - Uses GPU acceleration if CUDA is available, otherwise falls back to CPU
    - Processes video frame-by-frame like demo_video.py

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
import torch
from typing import Dict, List, Tuple, Optional
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





def _load_yolo_model() -> Tuple[YOLO, str]:
    """
    Load YOLO weights, matching the logic from demo_video.py.
    Tries to load best.pt (the trained ping pong model) from the script directory.
    """
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


def _select_primary_ball(results, frame_idx: int) -> Optional[BallPosition]:
    """
    Extract ball position from YOLO results, matching the logic from demo_video.py.
    Takes the highest confidence detection.
    """
    primary: Optional[BallPosition] = None
    
    for result in results:
        boxes = result.boxes
        if boxes is None or len(boxes) == 0:
            continue
            
        for box in boxes:
            # Extract bounding box coordinates directly like in demo_video.py
            x_min, y_min, x_max, y_max = map(float, box.xyxy[0])
            
            # Extract confidence
            confidence = float(box.conf[0])
            
            # Create ball position
            candidate = BallPosition(
                x=(x_min + x_max) / 2.0,
                y=(y_min + y_max) / 2.0,
                confidence=confidence,
                frame=frame_idx,
                bbox=(x_min, y_min, x_max, y_max)
            )
            
            # Keep highest confidence detection
            if primary is None or candidate.confidence > primary.confidence:
                primary = candidate
                
    return primary


class EventDetector:
    """Detects table tennis events & derives kinematic features.

    This is a lightweight heuristic module – not a trained classifier – so all
    derived attributes (speed, shotType, bounce) are best-effort estimates.
    """

    def __init__(self, fps: float, frame_height: int, frame_width: int, pixels_per_meter: float = 1000.0):
        self.fps = fps
        self.frame_height = frame_height
        self.frame_width = frame_width
        self.pixels_per_meter = pixels_per_meter
        self.history: deque[BallPosition] = deque(maxlen=40)
        self.last_bounce_frame = -100
        self.rally_start_frame = 0
        self.rally_shot_count = 0
        # Exponential moving averages for smoothing
        self._ema_speed_kmh: Optional[float] = None
        self._ema_alpha = 0.35

    # --------------------------- Public API --------------------------- #
    def add_position(self, pos: BallPosition) -> List[Dict]:
        self.history.append(pos)
        events: List[Dict] = []
        if len(self.history) < 4:
            return events

        bounce_event = self._detect_bounce()
        if bounce_event:
            events.append(bounce_event)

        fast_event = self._detect_fast_shot()
        if fast_event:
            events.append(fast_event)

        return events

    def current_speed_kmh(self) -> Optional[float]:
        """Return smoothed speed estimate based on last 3-6 frames."""
        if len(self.history) < 3 or self.fps <= 0:
            return None
        # Use last N frames (adaptive up to 6)
        sample = list(self.history)[-6:]
        first = sample[0]
        last = sample[-1]
        dt = (last.frame - first.frame) / self.fps
        if dt <= 0:
            return None
        dx = last.x - first.x
        dy = last.y - first.y
        dist_pixels = (dx * dx + dy * dy) ** 0.5
        m_per_pixel = 1.0 / self.pixels_per_meter
        speed_mps = (dist_pixels * m_per_pixel) / dt
        speed_kmh = speed_mps * 3.6
        # Smooth
        if self._ema_speed_kmh is None:
            self._ema_speed_kmh = speed_kmh
        else:
            self._ema_speed_kmh = self._ema_alpha * speed_kmh + (1 - self._ema_alpha) * self._ema_speed_kmh
        return round(self._ema_speed_kmh, 1)

    def classify_shot_type(self) -> Optional[str]:
        if len(self.history) < 4 or self.fps <= 0:
            return None
        p3, p2, p1, p0 = list(self.history)[-4:]
        # Instant velocities (pixels / frame)
        vx = (p0.x - p1.x)
        vy = (p0.y - p1.y)
        speed = (vx * vx + vy * vy) ** 0.5
        vertical_ratio = abs(vy) / (speed + 1e-6)
        # Heuristics
        if speed > self.frame_width * 0.25 and vy > 0:
            return "smash"
        if vy < -5 and vertical_ratio > 0.35:
            return "topspin"
        if vy > 6 and vertical_ratio > 0.4 and speed < self.frame_width * 0.18:
            return "lob"
        if speed > self.frame_width * 0.22:
            return "drive"
        return "rally"

    def finish_rally(self) -> Optional[Dict]:
        if self.rally_shot_count > 0 and self.history:
            last_frame = self.history[-1].frame
            event = {
                "type": "rally_end",
                "shotCount": self.rally_shot_count,
                "duration": (last_frame - self.rally_start_frame) / self.fps,
                "frame": last_frame,
            }
            self.rally_shot_count = 0
            self.rally_start_frame = last_frame
            return event
        return None

    # ------------------------ Internal Helpers ----------------------- #
    def _detect_bounce(self) -> Optional[Dict]:
        if len(self.history) < 6:
            return None
        current = self.history[-1]
        if current.frame - self.last_bounce_frame < int(self.fps * 0.08):  # ~80 ms debounce
            return None
        # Estimate vertical velocities (positive = downward if coordinate origin top-left)
        ys = [p.y for p in self.history]
        v_y = [ys[i+1] - ys[i] for i in range(len(ys)-1)]
        # A bounce pattern: strong downward velocity then sign change upward with magnitude drop
        # Use last 5 velocity samples
        window = v_y[-5:]
        if len(window) < 5:
            return None
        # Pattern detection
        down1, down2, up1 = window[-5], window[-4], window[-3]
        if down1 > 4 and down2 > 2 and up1 < -2:  # heuristic thresholds
            self.last_bounce_frame = current.frame
            self.rally_shot_count += 1
            speed_kmh = self.current_speed_kmh()
            shot_type = self.classify_shot_type()
            return {
                "frame": current.frame,
                "timestampMs": (current.frame / self.fps) * 1000.0,
                "type": "bounce",
                "label": "Ball Bounce",
                "player": self._infer_player(current.x),
                "rallyLength": self.rally_shot_count,
                "shotSpeed": speed_kmh,
                "shotType": shot_type,
                "frameNumber": current.frame
            }
        return None

    def _detect_fast_shot(self) -> Optional[Dict]:
        speed_kmh = self.current_speed_kmh()
        if speed_kmh is None:
            return None
        # Threshold chosen heuristically; adjust with calibration data
        if speed_kmh > 55:
            last = self.history[-1]
            return {
                "frame": last.frame,
                "timestampMs": (last.frame / self.fps) * 1000.0,
                "type": "fast_shot",
                "label": "Fast Shot",
                "player": self._infer_player(last.x),
                "shotSpeed": speed_kmh,
                "shotType": self.classify_shot_type(),
                "frameNumber": last.frame
            }
        return None

    def _infer_player(self, x: float) -> int:
        return 1 if x < self.frame_width / 2 else 2



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
    device = 'cuda' if torch.cuda.is_available() else 'cpu'
    print(f"Using device: {device}")
    
    model, _ = _load_yolo_model()
    
    print(f"Opening video: {video_path}")
    cap = cv2.VideoCapture(video_path)
    
    if not cap.isOpened():
        raise ValueError(f"Cannot open video: {video_path}")
    
    fps = cap.get(cv2.CAP_PROP_FPS)
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    frame_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    frame_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    
    print(f"Video properties: {frame_width}x{frame_height} @ {fps} fps, {frame_count} frames")
    
    output_results = {
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
        
        # Run inference - matching demo_video.py approach
        inference_results = model.predict(source=frame, conf=confidence_threshold, verbose=False, device=device)

        primary_ball = _select_primary_ball(inference_results, frame_idx)

        ball_detected = primary_ball is not None

        if primary_ball:
            events = detector.add_position(primary_ball)
            for event in events:
                output_results["events"].append(event)
                if event.get("shotSpeed"):
                    speeds.append(event["shotSpeed"])

            output_results["statistics"]["ballDetections"] += 1

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
            est_speed = detector.current_speed_kmh() or 0.0
            shot_type = detector.classify_shot_type() or "unknown"
            # Naive accuracy proxy: higher confidence & moderate speed -> higher accuracy
            raw_acc = (primary_ball.confidence * 0.6 + max(0.0, 1.0 - abs(est_speed - 45) / 60.0) * 0.4) * 100.0
            accuracy = round(max(0.0, min(100.0, raw_acc)), 1)
            # Result heuristic placeholder (improve later with table / out detection)
            result = "in"
            shot_payload = {
                "frame": frame_idx,
                "player": detector._infer_player(primary_ball.x),
                "speedKmh": est_speed,
                "accuracyPct": accuracy,
                "shotType": shot_type,
                "result": result,
                "confidence": float(primary_ball.confidence),
                "detections": detection_payload,
                "frameSeries": [frame_idx]
            }
            if timestamp_ms is not None:
                shot_payload["timestampSeries"] = [timestamp_ms]
            output_results["shots"].append(shot_payload)
        
        if not ball_detected:
            no_ball_frames += 1
            # If ball not detected for 30 frames (likely end of rally)
            if no_ball_frames > 30:
                rally_event = detector.finish_rally()
                if rally_event:
                    output_results["statistics"]["totalRallies"] += 1
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
    if output_results["events"]:
        bounce_events = [e for e in output_results["events"] if e["type"] == "bounce"]
        if bounce_events:
            output_results["statistics"]["totalRallies"] = max(1, len(bounce_events) // 4)
            rally_lengths = [e["rallyLength"] for e in bounce_events if "rallyLength" in e]
            if rally_lengths:
                output_results["statistics"]["avgRallyLength"] = round(sum(rally_lengths) / len(rally_lengths), 1)
    
    if speeds:
        output_results["statistics"]["avgBallSpeed"] = round(sum(speeds) / len(speeds), 1)
        output_results["statistics"]["maxBallSpeed"] = round(max(speeds), 1)
    
    # Estimate scores (simplified - count bounces per side)
    player1_bounces = len([e for e in output_results["events"] if e.get("player") == 1 and e["type"] == "bounce"])
    player2_bounces = len([e for e in output_results["events"] if e.get("player") == 2 and e["type"] == "bounce"])
    output_results["statistics"]["player1Score"] = player1_bounces // 2
    output_results["statistics"]["player2Score"] = player2_bounces // 2
    
    print(f"\nAnalysis complete:")
    print(f"  - Ball detected in {output_results['statistics']['ballDetections']} frames")
    print(f"  - {len(output_results['events'])} events detected")
    print(f"  - {len(output_results['shots'])} shots recorded")
    
    # Write results
    with open(output_json_path, 'w') as f:
        json.dump(output_results, f, indent=2)
    
    print(f"Results written to {output_json_path}")
    return output_results


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
