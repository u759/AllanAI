"""
Enhanced Ping Pong Match Analysis Service
==========================================

Enhancements based on TTNet paper (CVPR 2020) methodology:
- Table boundary detection for legal bounce validation
- Net position tracking for net hit detection
- Game state machine for rally/serve tracking
- Proper event classification (SCORE, MISS, RALLY_HIGHLIGHT, etc.)

References:
- TTNet paper: https://arxiv.org/abs/2004.09927
- OpenTTGames dataset: https://lab.osai.ai/
- ITTF rules for ping pong scoring
"""

from ultralytics import YOLO
import json
import sys
import cv2
import os
import torch
import numpy as np
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass
from collections import deque
from enum import Enum


class GamePhase(Enum):
    """Game state phases"""
    WAITING_FOR_SERVE = "WAITING_FOR_SERVE"
    SERVE_IN_PROGRESS = "SERVE_IN_PROGRESS"
    RALLY_IN_PROGRESS = "RALLY_IN_PROGRESS"
    POINT_SCORED = "POINT_SCORED"


class Side(Enum):
    """Table sides"""
    LEFT = 1
    RIGHT = 2
    UNKNOWN = 0


@dataclass
class BallPosition:
    """Ball position at a specific frame."""
    x: float
    y: float
    confidence: float
    frame: int
    bbox: Optional[Tuple[float, float, float, float]] = None


@dataclass
class TableBounds:
    """Table boundary coordinates"""
    x_min: float
    y_min: float
    x_max: float
    y_max: float
    net_x: float  # X-coordinate of the net (center line)
    
    def contains(self, x: float, y: float, margin: float = 20.0) -> bool:
        """Check if point is within table bounds with margin"""
        return (self.x_min - margin <= x <= self.x_max + margin and 
                self.y_min - margin <= y <= self.y_max + margin)
    
    def get_side(self, x: float) -> Side:
        """Determine which side of table the x-coordinate is on"""
        if x < self.net_x:
            return Side.LEFT
        elif x > self.net_x:
            return Side.RIGHT
        return Side.UNKNOWN
    
    def near_net(self, x: float, threshold: float = 30.0) -> bool:
        """Check if x-coordinate is near the net"""
        return abs(x - self.net_x) < threshold


class TableDetector:
    """
    Detects table boundaries using color segmentation.
    Based on TTNet's semantic segmentation approach but using traditional CV.
    """
    
    def __init__(self):
        self.cached_bounds: Optional[TableBounds] = None
        self.table_color_range = {
            'blue': ((90, 50, 50), (130, 255, 255)),  # Blue tables
            'green': ((35, 40, 40), (85, 255, 255))   # Green tables
        }
    
    def detect(self, frame: np.ndarray, force_recalculate: bool = False) -> Optional[TableBounds]:
        """
        Detect table boundaries in frame.
        Caches result since table position shouldn't change during match.
        """
        if self.cached_bounds is not None and not force_recalculate:
            return self.cached_bounds
        
        hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
        
        # Try both color ranges
        best_bounds = None
        best_area = 0
        
        for color_name, (lower, upper) in self.table_color_range.items():
            mask = cv2.inRange(hsv, np.array(lower), np.array(upper))
            
            # Morphological operations to clean up mask
            kernel = np.ones((5, 5), np.uint8)
            mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)
            mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)
            
            # Find contours
            contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            if not contours:
                continue
            
            # Get largest contour (should be the table)
            largest_contour = max(contours, key=cv2.contourArea)
            area = cv2.contourArea(largest_contour)
            
            if area > best_area:
                best_area = area
                x, y, w, h = cv2.boundingRect(largest_contour)
                
                # Net is assumed to be at the center of the table
                net_x = x + w / 2.0
                
                best_bounds = TableBounds(
                    x_min=float(x),
                    y_min=float(y),
                    x_max=float(x + w),
                    y_max=float(y + h),
                    net_x=net_x
                )
        
        # Require minimum table area (avoid false detections)
        frame_area = frame.shape[0] * frame.shape[1]
        if best_bounds and best_area > frame_area * 0.1:  # Table should be at least 10% of frame
            self.cached_bounds = best_bounds
            return best_bounds
        
        return None
    
    def calibrate_from_points(self, top_left: Tuple[float, float], 
                             top_right: Tuple[float, float],
                             bottom_left: Tuple[float, float],
                             bottom_right: Tuple[float, float]) -> TableBounds:
        """
        Manual calibration by specifying table corners.
        Use this if automatic detection fails.
        """
        x_coords = [top_left[0], top_right[0], bottom_left[0], bottom_right[0]]
        y_coords = [top_left[1], top_right[1], bottom_left[1], bottom_right[1]]
        
        x_min = min(x_coords)
        x_max = max(x_coords)
        y_min = min(y_coords)
        y_max = max(y_coords)
        net_x = (x_min + x_max) / 2.0
        
        bounds = TableBounds(
            x_min=x_min,
            y_min=y_min,
            x_max=x_max,
            y_max=y_max,
            net_x=net_x
        )
        
        self.cached_bounds = bounds
        return bounds


class GameStateTracker:
    """
    Tracks game state following ITTF ping pong rules.
    
    Based on TTNet paper's event spotting approach but with explicit game logic:
    - Serve detection
    - Rally tracking
    - Score keeping
    - Legal bounce validation
    """
    
    def __init__(self, fps: float):
        self.fps = fps
        self.phase = GamePhase.WAITING_FOR_SERVE
        self.server = Side.LEFT  # Start with left side serving
        self.score = {Side.LEFT: 0, Side.RIGHT: 0}
        self.rally_shot_count = 0
        self.rally_start_frame = 0
        self.last_bounce_side = Side.UNKNOWN
        self.last_bounce_frame = -100
        self.serve_count = 0  # Serves alternate every 2 points
        self.last_ball_side = Side.UNKNOWN
        
    def should_switch_server(self) -> bool:
        """In ping pong, server switches every 2 points"""
        total_points = self.score[Side.LEFT] + self.score[Side.RIGHT]
        return total_points % 2 == 0
    
    def process_bounce(self, ball_pos: BallPosition, table_bounds: TableBounds, 
                      ball_history: deque) -> Optional[Dict]:
        """
        Process a detected bounce and determine game event.
        
        Returns event dict if this bounce is significant (point scored, etc.)
        """
        # Debounce - ignore bounces too close together (less than 80ms)
        if ball_pos.frame - self.last_bounce_frame < int(self.fps * 0.08):
            return None
        
        # Determine if bounce is on table
        on_table = table_bounds.contains(ball_pos.x, ball_pos.y)
        bounce_side = table_bounds.get_side(ball_pos.x)
        
        event = None
        
        if self.phase == GamePhase.WAITING_FOR_SERVE:
            if on_table and bounce_side == self.server:
                # Valid serve (must bounce on server's side first)
                self.phase = GamePhase.SERVE_IN_PROGRESS
                self.rally_start_frame = ball_pos.frame
                self.rally_shot_count = 1
                self.last_bounce_side = bounce_side
                event = {
                    "type": "serve",
                    "label": "Serve",
                    "is_scoring": False
                }
            else:
                # Service fault
                opponent = Side.RIGHT if self.server == Side.LEFT else Side.LEFT
                self._award_point(opponent)
                event = {
                    "type": "MISS",
                    "label": "Service Fault",
                    "is_scoring": True,
                    "scoring_side": self._side_value(opponent)
                }
        
        elif self.phase == GamePhase.SERVE_IN_PROGRESS:
            # After serve bounces on server side, must bounce on opponent side
            opponent_side = Side.RIGHT if self.server == Side.LEFT else Side.LEFT
            
            if on_table and bounce_side == opponent_side:
                # Valid serve completed, rally begins
                self.phase = GamePhase.RALLY_IN_PROGRESS
                self.rally_shot_count += 1
                self.last_bounce_side = bounce_side
                event = {
                    "type": "bounce",
                    "label": "Ball Bounce",
                    "is_scoring": False
                }
            elif not on_table:
                # Serve went out
                self._award_point(opponent_side)
                event = {
                    "type": "MISS",
                    "label": "Serve Out",
                    "is_scoring": True,
                    "scoring_side": self._side_value(opponent_side)
                }
            else:
                # Serve bounced on wrong side
                self._award_point(opponent_side)
                event = {
                    "type": "MISS",
                    "label": "Service Error",
                    "is_scoring": True,
                    "scoring_side": self._side_value(opponent_side)
                }
        
        elif self.phase == GamePhase.RALLY_IN_PROGRESS:
            # Rally rules: must bounce on opponent's side from last bounce
            expected_side = Side.RIGHT if self.last_bounce_side == Side.LEFT else Side.LEFT
            
            if on_table and bounce_side == expected_side:
                # Legal bounce, rally continues
                self.rally_shot_count += 1
                self.last_bounce_side = bounce_side
                event = {
                    "type": "bounce",
                    "label": "Ball Bounce",
                    "is_scoring": False,
                    "rally_length": self.rally_shot_count
                }
            elif not on_table:
                # Ball went out - last hitter loses point
                scoring_side = self.last_bounce_side  # Opponent of who hit it out
                self._award_point(scoring_side)
                event = {
                    "type": "MISS",
                    "label": "Ball Out",
                    "is_scoring": True,
                    "scoring_side": self._side_value(scoring_side)
                }
            else:
                # Bounced on wrong side (double bounce on same side)
                scoring_side = bounce_side  # Opponent scores
                self._award_point(scoring_side)
                event = {
                    "type": "MISS",
                    "label": "Double Bounce",
                    "is_scoring": True,
                    "scoring_side": self._side_value(scoring_side)
                }
        
        self.last_bounce_frame = ball_pos.frame
        return event
    
    def process_net_hit(self, ball_pos: BallPosition, table_bounds: TableBounds) -> Optional[Dict]:
        """
        Process a net hit.
        
        During serve: fault (point to receiver)
        During rally: play continues if ball goes over
        """
        event = None
        
        if self.phase == GamePhase.SERVE_IN_PROGRESS:
            # Net on serve is a fault
            opponent = Side.RIGHT if self.server == Side.LEFT else Side.LEFT
            self._award_point(opponent)
            event = {
                "type": "MISS",
                "label": "Net on Serve",
                "is_scoring": True,
                "scoring_side": self._side_value(opponent)
            }
        elif self.phase == GamePhase.RALLY_IN_PROGRESS:
            # Net during rally - point goes to opponent if ball doesn't clear
            # This requires checking next frames to see if ball clears net
            event = {
                "type": "net_hit",
                "label": "Net Hit",
                "is_scoring": False  # Will be updated if ball doesn't clear
            }
        
        return event
    
    def _award_point(self, side: Side):
        """Award point and reset for next serve"""
        if side in [Side.LEFT, Side.RIGHT]:
            self.score[side] += 1
        
        # Check if server should switch
        if self.should_switch_server():
            self.server = Side.RIGHT if self.server == Side.LEFT else Side.LEFT
        
        # Reset for next point
        self.phase = GamePhase.WAITING_FOR_SERVE
        self.rally_shot_count = 0
        self.last_bounce_side = Side.UNKNOWN

    @staticmethod
    def _side_value(side: Optional[Side]) -> int:
        """Convert Side enum to primitive for serialization."""
        if isinstance(side, Side):
            return int(side.value)
        if isinstance(side, (int, float)):
            return int(side)
        return 0
    
    def finish_rally(self, final_frame: int) -> Optional[Dict]:
        """Called when ball is not detected for extended period"""
        if self.rally_shot_count > 0:
            duration = (final_frame - self.rally_start_frame) / self.fps
            
            # If rally was in progress, award point to last side that successfully hit
            if self.phase == GamePhase.RALLY_IN_PROGRESS:
                scoring_side = self.last_bounce_side
                self._award_point(scoring_side)
                
                return {
                    "type": "SCORE",
                    "label": "Rally End",
                    "is_scoring": True,
                    "scoring_side": self._side_value(scoring_side),
                    "rally_length": self.rally_shot_count,
                    "duration": duration
                }
            
            # Reset state
            self.rally_shot_count = 0
        
        return None


class EnhancedEventDetector:
    """
    Enhanced event detector with table awareness and game state tracking.
    
    Improvements over basic heuristic approach:
    - Uses table boundaries for legal bounce validation
    - Tracks game state (serve, rally, scoring)
    - Detects net hits
    - Follows official ITTF ping pong rules
    """
    
    def __init__(self, fps: float, frame_height: int, frame_width: int, 
                 table_bounds: Optional[TableBounds] = None):
        self.fps = fps
        self.frame_height = frame_height
        self.frame_width = frame_width
        self.table_bounds = table_bounds
        self.history: deque[BallPosition] = deque(maxlen=40)
        self.game_state = GameStateTracker(fps)
        
        # Speed calculation (calibrated to table size)
        if table_bounds:
            # Standard ping pong table is 2.74m x 1.525m
            table_width_pixels = table_bounds.x_max - table_bounds.x_min
            self.pixels_per_meter = table_width_pixels / 1.525
        else:
            self.pixels_per_meter = 1000.0  # Fallback
        
        # Smoothing
        self._ema_speed_kmh: Optional[float] = None
        self._ema_alpha = 0.35
    
    def set_table_bounds(self, bounds: TableBounds):
        """Update table boundaries and recalibrate"""
        self.table_bounds = bounds
        table_width_pixels = bounds.x_max - bounds.x_min
        self.pixels_per_meter = table_width_pixels / 1.525
    
    def add_position(self, pos: BallPosition) -> List[Dict]:
        """Process new ball position and detect events"""
        self.history.append(pos)
        events: List[Dict] = []
        
        if len(self.history) < 4:
            return events
        
        # Detect bounces
        bounce_event = self._detect_bounce()
        if bounce_event and self.table_bounds:
            # Process bounce through game state machine
            game_event = self.game_state.process_bounce(pos, self.table_bounds, self.history)
            if game_event:
                # Merge bounce detection with game state info
                bounce_event.update(game_event)
                events.append(bounce_event)
        
        # Detect net hits
        if self.table_bounds and self.table_bounds.near_net(pos.x):
            net_event = self._detect_net_hit()
            if net_event:
                game_event = self.game_state.process_net_hit(pos, self.table_bounds)
                if game_event:
                    net_event.update(game_event)
                    events.append(net_event)
        
        # Detect special shots (smashes, aces, etc.)
        special_event = self._detect_special_shot()
        if special_event:
            events.append(special_event)
        
        return events
    
    def _detect_bounce(self) -> Optional[Dict]:
        """
        Detect ball bounce using vertical velocity pattern analysis.
        
        Based on TTNet paper methodology: bounces show characteristic
        velocity reversal pattern (downward then upward motion).
        """
        if len(self.history) < 6:
            return None
        
        current = self.history[-1]
        
        # Calculate vertical velocities
        ys = [p.y for p in list(self.history)[-6:]]
        v_y = [ys[i+1] - ys[i] for i in range(len(ys)-1)]
        
        if len(v_y) < 5:
            return None
        
        # Bounce pattern: downward motion followed by upward (velocity sign change)
        # Note: Y increases downward in image coordinates
        down1, down2, mid, up1, up2 = v_y[-5:]
        
        # Thresholds adjusted for typical frame rates (30-120 fps)
        velocity_threshold = 3.0 if self.fps < 60 else 5.0
        
        if (down1 > velocity_threshold and 
            down2 > velocity_threshold * 0.5 and 
            mid > -velocity_threshold * 0.3 and
            up1 < -velocity_threshold * 0.5):
            
            speed_kmh = self.current_speed_kmh()
            shot_type = self.classify_shot_type()
            
            return {
                "frame": current.frame,
                "timestampMs": (current.frame / self.fps) * 1000.0,
                "type": "bounce",  # Will be updated by game state
                "label": "Ball Bounce",
                "player": self._infer_player(current.x),
                "shotSpeed": speed_kmh,
                "shotType": shot_type,
                "frameNumber": current.frame,
                "confidence": current.confidence
            }
        
        return None
    
    def _detect_net_hit(self) -> Optional[Dict]:
        """
        Detect net hits by analyzing ball trajectory near net position.
        
        Net hit characteristics:
        - Ball is near net X-coordinate
        - Sudden direction change or velocity drop
        - Usually lower Y position (near table height)
        """
        if len(self.history) < 4 or not self.table_bounds:
            return None
        
        positions = list(self.history)[-4:]
        current = positions[-1]
        
        # Check if ball is near net
        if not self.table_bounds.near_net(current.x, threshold=30.0):
            return None
        
        # Check if ball is at table height
        table_center_y = (self.table_bounds.y_min + self.table_bounds.y_max) / 2.0
        if abs(current.y - table_center_y) > 100:  # More than 100px from table center
            return None
        
        # Analyze trajectory - net hits show sudden horizontal velocity drop
        if len(positions) >= 3:
            prev = positions[-2]
            prev_prev = positions[-3]
            
            vx_before = prev_prev.x - prev.x
            vx_after = prev.x - current.x
            
            # Net hit: velocity reversal or significant drop
            if (abs(vx_before) > 5 and abs(vx_after) < abs(vx_before) * 0.3) or \
               (vx_before * vx_after < 0):  # Direction reversal
                
                return {
                    "frame": current.frame,
                    "timestampMs": (current.frame / self.fps) * 1000.0,
                    "type": "net_hit",
                    "label": "Net Hit",
                    "frameNumber": current.frame,
                    "confidence": current.confidence
                }
        
        return None
    
    def _detect_special_shot(self) -> Optional[Dict]:
        """Detect special shots (smashes, aces, fast shots)"""
        speed_kmh = self.current_speed_kmh()
        
        if speed_kmh is None:
            return None
        
        current = self.history[-1]
        shot_type = self.classify_shot_type()
        
        # Smash detection (high speed + downward trajectory)
        if speed_kmh > 70 and shot_type == "smash":
            return {
                "frame": current.frame,
                "timestampMs": (current.frame / self.fps) * 1000.0,
                "type": "FASTEST_SHOT",
                "label": "Powerful Smash",
                "player": self._infer_player(current.x),
                "shotSpeed": speed_kmh,
                "shotType": shot_type,
                "frameNumber": current.frame,
                "importance": 8
            }
        
        # Fast shot detection
        if speed_kmh > 55:
            return {
                "frame": current.frame,
                "timestampMs": (current.frame / self.fps) * 1000.0,
                "type": "FASTEST_SHOT",
                "label": "Fast Shot",
                "player": self._infer_player(current.x),
                "shotSpeed": speed_kmh,
                "shotType": shot_type,
                "frameNumber": current.frame,
                "importance": 6
            }
        
        # Long rally detection
        if self.game_state.rally_shot_count >= 10:
            return {
                "frame": current.frame,
                "timestampMs": (current.frame / self.fps) * 1000.0,
                "type": "RALLY_HIGHLIGHT",
                "label": "Extended Rally",
                "rallyLength": self.game_state.rally_shot_count,
                "frameNumber": current.frame,
                "importance": 7
            }
        
        return None
    
    def current_speed_kmh(self) -> Optional[float]:
        """Calculate current ball speed with EMA smoothing"""
        if len(self.history) < 3 or self.fps <= 0:
            return None
        
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
        
        # Apply exponential smoothing
        if self._ema_speed_kmh is None:
            self._ema_speed_kmh = speed_kmh
        else:
            self._ema_speed_kmh = self._ema_alpha * speed_kmh + (1 - self._ema_alpha) * self._ema_speed_kmh
        
        return round(self._ema_speed_kmh, 1)
    
    def classify_shot_type(self) -> Optional[str]:
        """Classify shot type based on trajectory"""
        if len(self.history) < 4 or self.fps <= 0:
            return None
        
        positions = list(self.history)[-4:]
        p3, p2, p1, p0 = positions
        
        vx = (p0.x - p1.x)
        vy = (p0.y - p1.y)
        speed = (vx * vx + vy * vy) ** 0.5
        vertical_ratio = abs(vy) / (speed + 1e-6)
        
        # Shot classification heuristics
        if speed > self.frame_width * 0.25 and vy > 0:
            return "smash"
        if vy < -5 and vertical_ratio > 0.35:
            return "topspin"
        if vy > 6 and vertical_ratio > 0.4 and speed < self.frame_width * 0.18:
            return "lob"
        if speed > self.frame_width * 0.22:
            return "drive"
        
        return "rally"
    
    def _infer_player(self, x: float) -> int:
        """Infer player number from x-coordinate"""
        if self.table_bounds:
            side = self.table_bounds.get_side(x)
            return 1 if side == Side.LEFT else 2
        return 1 if x < self.frame_width / 2 else 2
    
    def finish_rally(self, final_frame: int) -> Optional[Dict]:
        """Finish current rally and return summary event"""
        return self.game_state.finish_rally(final_frame)
    
    def get_score(self) -> Tuple[int, int]:
        """Get current score (player1, player2)"""
        return (self.game_state.score[Side.LEFT], self.game_state.score[Side.RIGHT])


def _load_yolo_model() -> Tuple[YOLO, str]:
    """Load YOLO weights"""
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

    fallback = os.environ.get("YOLO_FALLBACK_MODEL", "yolo11n.pt")
    print(f"Primary model weights not found. Falling back to {fallback}...")
    return YOLO(fallback), fallback


def _select_primary_ball(results, frame_idx: int) -> Optional[BallPosition]:
    """Extract ball position from YOLO results"""
    primary: Optional[BallPosition] = None
    
    for result in results:
        boxes = result.boxes
        if boxes is None or len(boxes) == 0:
            continue
            
        for box in boxes:
            x_min, y_min, x_max, y_max = map(float, box.xyxy[0])
            confidence = float(box.conf[0])
            
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


def analyze_video(video_path: str, output_json_path: str, 
                 confidence_threshold: float = 0.25,
                 manual_table_bounds: Optional[Dict] = None) -> Dict:
    """
    Analyze a ping pong video with enhanced event detection.
    
    Args:
        video_path: Path to input video file
        output_json_path: Path to write JSON results
        confidence_threshold: Minimum confidence for ball detection (0.0-1.0)
        manual_table_bounds: Optional dict with keys: x_min, y_min, x_max, y_max, net_x
    
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
    
    # Initialize table detector
    table_detector = TableDetector()
    table_bounds = None
    
    if manual_table_bounds:
        print("Using manual table bounds")
        table_bounds = TableBounds(**manual_table_bounds)
        table_detector.cached_bounds = table_bounds
    
    # Read first frame for table detection
    ret, first_frame = cap.read()
    if ret and table_bounds is None:
        print("Detecting table boundaries...")
        table_bounds = table_detector.detect(first_frame)
        if table_bounds:
            print(f"Table detected: x=[{table_bounds.x_min:.0f}, {table_bounds.x_max:.0f}], "
                  f"y=[{table_bounds.y_min:.0f}, {table_bounds.y_max:.0f}], net_x={table_bounds.net_x:.0f}")
        else:
            print("WARNING: Table detection failed. Results may be inaccurate.")
            print("Consider providing manual table bounds.")
    
    # Reset video to start
    cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
    
    # Initialize enhanced detector
    detector = EnhancedEventDetector(fps, frame_height, frame_width, table_bounds)
    
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
        },
        "tableDetected": table_bounds is not None
    }
    
    if table_bounds:
        output_results["tableBounds"] = {
            "xMin": table_bounds.x_min,
            "yMin": table_bounds.y_min,
            "xMax": table_bounds.x_max,
            "yMax": table_bounds.y_max,
            "netX": table_bounds.net_x
        }
    
    frame_idx = 0
    no_ball_frames = 0
    speeds = []
    
    print("Processing frames...")
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        
        # Run YOLO inference
        inference_results = model.predict(
            source=frame, 
            conf=confidence_threshold, 
            verbose=False, 
            device=device
        )
        
        primary_ball = _select_primary_ball(inference_results, frame_idx)
        
        if primary_ball:
            # Process through enhanced detector
            events = detector.add_position(primary_ball)
            
            for event in events:
                # Map event types to Java enums
                event_type = event.get("type", "bounce").upper()
                if event.get("is_scoring"):
                    event_type = "SCORE"
                
                event["type"] = event_type
                output_results["events"].append(event)
                
                if event.get("shotSpeed"):
                    speeds.append(event["shotSpeed"])
            
            output_results["statistics"]["ballDetections"] += 1
            
            # Build shot record
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
            
            timestamp_ms = (frame_idx / fps) * 1000.0
            est_speed = detector.current_speed_kmh() or 0.0
            shot_type = detector.classify_shot_type() or "rally"
            
            # Better accuracy calculation
            on_table = table_bounds and table_bounds.contains(primary_ball.x, primary_ball.y) if table_bounds else True
            raw_acc = (primary_ball.confidence * 0.7 + (1.0 if on_table else 0.5) * 0.3) * 100.0
            accuracy = round(max(0.0, min(100.0, raw_acc)), 1)
            
            # Determine result
            result = "in" if on_table else "out"
            
            shot_payload = {
                "frame": frame_idx,
                "player": detector._infer_player(primary_ball.x),
                "speedKmh": est_speed,
                "accuracyPct": accuracy,
                "shotType": shot_type,
                "result": result,
                "confidence": float(primary_ball.confidence),
                "detections": detection_payload,
                "frameSeries": [frame_idx],
                "timestampSeries": [timestamp_ms]
            }
            
            output_results["shots"].append(shot_payload)
            no_ball_frames = 0
        else:
            no_ball_frames += 1
            
            # End rally if ball not detected for 30 frames (~0.5-1 second)
            if no_ball_frames > 30:
                rally_event = detector.finish_rally(frame_idx)
                if rally_event:
                    rally_event["type"] = "SCORE"
                    output_results["events"].append(rally_event)
                    output_results["statistics"]["totalRallies"] += 1
                no_ball_frames = 0
        
        frame_idx += 1
        
        if frame_idx % 100 == 0:
            progress = (frame_idx / frame_count) * 100
            print(f"Progress: {progress:.1f}% ({frame_idx}/{frame_count} frames)")
    
    cap.release()
    
    # Get final score from game state
    p1_score, p2_score = detector.get_score()
    output_results["statistics"]["player1Score"] = p1_score
    output_results["statistics"]["player2Score"] = p2_score
    
    # Calculate statistics
    if output_results["events"]:
        bounce_events = [e for e in output_results["events"] if e.get("rallyLength")]
        if bounce_events:
            rally_lengths = [e["rallyLength"] for e in bounce_events]
            output_results["statistics"]["avgRallyLength"] = round(
                sum(rally_lengths) / len(rally_lengths), 1
            )
    
    if speeds:
        output_results["statistics"]["avgBallSpeed"] = round(sum(speeds) / len(speeds), 1)
        output_results["statistics"]["maxBallSpeed"] = round(max(speeds), 1)
    
    if output_results["statistics"]["totalRallies"] == 0:
        output_results["statistics"]["totalRallies"] = max(1, len(output_results["events"]) // 3)
    
    print(f"\nAnalysis complete:")
    print(f"  - Ball detected in {output_results['statistics']['ballDetections']} frames")
    print(f"  - {len(output_results['events'])} events detected")
    print(f"  - {len(output_results['shots'])} shots recorded")
    print(f"  - Score: {p1_score} - {p2_score}")
    
    # Write results
    with open(output_json_path, 'w') as f:
        json.dump(output_results, f, indent=2)
    
    print(f"Results written to {output_json_path}")
    return output_results


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python model_service.py <video_path> <output_json> [confidence_threshold]")
        print("Example: python model_service.py match.mp4 output.json 0.25")
        print("\nOptional: Set TABLE_BOUNDS environment variable for manual calibration")
        print('  TABLE_BOUNDS=\'{"x_min":100,"y_min":200,"x_max":800,"y_max":600,"net_x":450}\'')
        sys.exit(1)
    
    video_path = sys.argv[1]
    output_json = sys.argv[2]
    confidence = float(sys.argv[3]) if len(sys.argv) > 3 else 0.25
    
    # Check for manual table bounds
    manual_bounds = None
    if 'TABLE_BOUNDS' in os.environ:
        try:
            manual_bounds = json.loads(os.environ['TABLE_BOUNDS'])
            print(f"Using manual table bounds: {manual_bounds}")
        except json.JSONDecodeError:
            print("WARNING: TABLE_BOUNDS environment variable is not valid JSON")
    
    try:
        analyze_video(video_path, output_json, confidence, manual_bounds)
        sys.exit(0)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)