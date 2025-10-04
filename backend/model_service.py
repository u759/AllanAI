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
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass, asdict
from collections import deque


@dataclass
class BallPosition:
    """Ball position at a specific frame."""
    x: float
    y: float
    confidence: float
    frame: int


class EventDetector:
    """Detects table tennis events from ball trajectory."""
    
    def __init__(self, fps: float, frame_height: int, frame_width: int):
        self.fps = fps
        self.frame_height = frame_height
        self.frame_width = frame_width
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
        
        if len(y_velocities) >= 4:
            # Look for pattern: negative (going up) to positive (going down) to negative (bouncing back up)
            for i in range(len(y_velocities) - 2):
                if y_velocities[i] > 5 and y_velocities[i+1] > 5 and y_velocities[i+2] < -5:
                    # Detected bounce pattern
                    self.last_bounce_frame = current.frame
                    self.rally_shot_count += 1
                    
                    # Calculate shot speed from trajectory
                    dx = abs(positions[-1].x - positions[-5].x)
                    dy = abs(positions[-1].y - positions[-5].y)
                    distance_pixels = (dx**2 + dy**2) ** 0.5
                    time_seconds = 4 / self.fps
                    speed_pixels_per_second = distance_pixels / time_seconds
                    # Estimate real speed (rough calibration: 1000 pixels ≈ 1 meter at typical distance)
                    speed_mps = speed_pixels_per_second / 1000.0 * 3.6  # Convert to km/h
                    
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
        if distance > self.frame_width / 3:
            speed_mps = (distance / 5) * self.fps / 1000.0 * 3.6
            
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
    # Check if model exists
    model_path = os.path.join(os.path.dirname(__file__), "best.pt")
    if not os.path.exists(model_path):
        raise FileNotFoundError(f"Model file not found: {model_path}. Please train the model or download pre-trained weights.")
    
    print(f"Loading model from {model_path}...")
    model = YOLO(model_path)
    
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
    
    detector = EventDetector(fps, frame_height, frame_width)
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
        
        ball_detected = False
        for prediction in predictions:
            for box in prediction.boxes:
                # Extract ball position
                x_min, y_min, x_max, y_max = map(float, box.xyxy[0])
                x_center = (x_min + x_max) / 2.0
                y_center = (y_min + y_max) / 2.0
                confidence = float(box.conf[0])
                
                ball_pos = BallPosition(x_center, y_center, confidence, frame_idx)
                
                # Detect events
                events = detector.add_position(ball_pos)
                for event in events:
                    results["events"].append(event)
                    if event.get("shotSpeed"):
                        speeds.append(event["shotSpeed"])
                
                results["statistics"]["ballDetections"] += 1
                ball_detected = True
                
                # Record shot (simplified - one per detection)
                results["shots"].append({
                    "frame": frame_idx,
                    "player": detector._infer_player(x_center),
                    "speed": speeds[-1] if speeds else 30.0,
                    "accuracy": 85.0,  # Default, could be refined
                    "shotType": "forehand",
                    "result": "in",
                    "confidence": confidence
                })
        
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
