"""
table_calibration.py - Interactive tool to manually calibrate table boundaries
"""

import cv2
import json
import sys

class TableCalibrationTool:
    def __init__(self, video_path: str):
        self.video_path = video_path
        self.points = []
        self.point_labels = ["Top-Left", "Top-Right", "Bottom-Right", "Bottom-Left"]
        
    def mouse_callback(self, event, x, y, flags, param):
        if event == cv2.EVENT_LBUTTONDOWN and len(self.points) < 4:
            self.points.append((x, y))
            print(f"Marked {self.point_labels[len(self.points)-1]}: ({x}, {y})")
    
    def calibrate(self) -> dict:
        cap = cv2.VideoCapture(self.video_path)
        ret, frame = cap.read()
        cap.release()
        
        if not ret:
            raise ValueError(f"Cannot read video: {self.video_path}")
        
        clone = frame.copy()
        cv2.namedWindow("Calibration")
        cv2.setMouseCallback("Calibration", self.mouse_callback)
        
        print("\nClick the 4 corners of the table in this order:")
        print("1. Top-Left")
        print("2. Top-Right")
        print("3. Bottom-Right")
        print("4. Bottom-Left")
        print("Press 'r' to reset, 'q' to finish\n")
        
        while True:
            display = clone.copy()
            
            # Draw marked points
            for i, (px, py) in enumerate(self.points):
                cv2.circle(display, (px, py), 5, (0, 255, 0), -1)
                cv2.putText(display, self.point_labels[i], (px+10, py-10),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
            
            # Draw lines between points
            if len(self.points) > 1:
                for i in range(len(self.points)-1):
                    cv2.line(display, self.points[i], self.points[i+1], (0, 255, 0), 2)
            
            if len(self.points) == 4:
                cv2.line(display, self.points[3], self.points[0], (0, 255, 0), 2)
                cv2.putText(display, "Press 'q' to finish", (10, 30),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
            
            cv2.imshow("Calibration", display)
            key = cv2.waitKey(1) & 0xFF
            
            if key == ord('r'):
                self.points = []
                print("Reset points")
            elif key == ord('q') and len(self.points) == 4:
                break
        
        cv2.destroyAllWindows()
        
        # Calculate bounds
        xs = [p[0] for p in self.points]
        ys = [p[1] for p in self.points]
        
        bounds = {
            "x_min": float(min(xs)),
            "y_min": float(min(ys)),
            "x_max": float(max(xs)),
            "y_max": float(max(ys)),
            "net_x": float((min(xs) + max(xs)) / 2)
        }
        
        print(f"\nCalibrated table bounds: {bounds}")
        return bounds

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python table_calibration.py <video_path> [output_json]")
        sys.exit(1)
    
    video_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else "table_bounds.json"
    
    tool = TableCalibrationTool(video_path)
    bounds = tool.calibrate()
    
    with open(output_path, 'w') as f:
        json.dump(bounds, f, indent=2)
    
    print(f"\nTable bounds saved to {output_path}")
    print(f"\nTo use with model_service.py:")
    print(f'  export TABLE_BOUNDS=\'{json.dumps(bounds)}\'')
    print(f"  python model_service.py {video_path} output.json")