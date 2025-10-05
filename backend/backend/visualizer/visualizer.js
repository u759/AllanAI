// ==========================================
// Ping Pong Match Visualizer
// ==========================================

class MatchVisualizer {
    constructor() {
        this.video = null;
        this.canvas = null;
        this.ctx = null;
        this.logData = null;
        this.eventsData = null;
        this.highlightsData = null;
        this.currentEventIndex = -1;
        this.currentShotIndex = -1;
        this.animationFrameId = null;
        
        // Player speed chart data
        this.player1SpeedCanvas = null;
        this.player1SpeedCtx = null;
        this.player2SpeedCanvas = null;
        this.player2SpeedCtx = null;
        this.player1SpeedData = [];
        this.player2SpeedData = [];
        this.maxChartDataPoints = 50;
        this.player1MaxSpeed = 0;
        this.player1TotalSpeed = 0;
        this.player1ShotCount = 0;
        this.player2MaxSpeed = 0;
        this.player2TotalSpeed = 0;
        this.player2ShotCount = 0;
        
        this.initializeElements();
        this.setupEventListeners();
    }

    initializeElements() {
        // Video elements
        this.video = document.getElementById('video-player');
        this.canvas = document.getElementById('overlay-canvas');
        this.ctx = this.canvas?.getContext('2d');
        
        // Upload elements
        this.videoUpload = document.getElementById('video-upload');
        this.logUpload = document.getElementById('log-upload');
        this.eventsUpload = document.getElementById('events-upload');
        this.highlightsUpload = document.getElementById('highlights-upload');
        this.loadBtn = document.getElementById('load-btn');
        this.uploadSection = document.getElementById('upload-section');
        
        // Control elements
        this.playPauseBtn = document.getElementById('play-pause-btn');
        this.seekBar = document.getElementById('seek-bar');
        this.currentTimeDisplay = document.getElementById('current-time');
        this.durationDisplay = document.getElementById('duration');
        this.playbackSpeedSelect = document.getElementById('playback-speed');
        
        // Display elements
        this.player1ScoreEl = document.getElementById('player1-score');
        this.player2ScoreEl = document.getElementById('player2-score');
        this.eventNotification = document.getElementById('event-notification');
        this.timelineEl = document.getElementById('timeline');
        this.eventsListEl = document.getElementById('events-list');
        this.highlightsListEl = document.getElementById('highlights-list');
        
        // Stats elements
        this.totalRalliesEl = document.getElementById('total-rallies');
        this.avgRallyEl = document.getElementById('avg-rally');
        this.maxSpeedEl = document.getElementById('max-speed');
        this.avgSpeedEl = document.getElementById('avg-speed');
        this.currentShotSpeedEl = document.getElementById('current-shot-speed');
        this.currentShotTypeEl = document.getElementById('current-shot-type');
        this.currentShotAccuracyEl = document.getElementById('current-shot-accuracy');
        this.currentShotResultEl = document.getElementById('current-shot-result');
        
        // Speed chart elements
        this.player1SpeedCanvas = document.getElementById('player1-speed-chart');
        this.player1SpeedCtx = this.player1SpeedCanvas?.getContext('2d');
        this.player2SpeedCanvas = document.getElementById('player2-speed-chart');
        this.player2SpeedCtx = this.player2SpeedCanvas?.getContext('2d');
        this.player1CurrentSpeedEl = document.getElementById('player1-current-speed');
        this.player2CurrentSpeedEl = document.getElementById('player2-current-speed');
        this.player1MaxSpeedEl = document.getElementById('player1-max-speed');
        this.player1AvgSpeedEl = document.getElementById('player1-avg-speed');
        this.player2MaxSpeedEl = document.getElementById('player2-max-speed');
        this.player2AvgSpeedEl = document.getElementById('player2-avg-speed');
    }

    setupEventListeners() {
        // File uploads
        this.videoUpload?.addEventListener('change', () => this.checkAllFilesLoaded());
        this.logUpload?.addEventListener('change', () => this.checkAllFilesLoaded());
        this.eventsUpload?.addEventListener('change', () => this.checkAllFilesLoaded());
        this.highlightsUpload?.addEventListener('change', () => this.checkAllFilesLoaded());
        this.loadBtn?.addEventListener('click', () => this.loadVisualizer());
        
        // Video controls
        this.playPauseBtn?.addEventListener('click', () => this.togglePlayPause());
        this.seekBar?.addEventListener('input', (e) => this.seek(e));
        this.playbackSpeedSelect?.addEventListener('change', (e) => this.changeSpeed(e));
        
        // Video events
        if (this.video) {
            this.video.addEventListener('loadedmetadata', () => this.onVideoLoaded());
            this.video.addEventListener('timeupdate', () => this.onTimeUpdate());
            this.video.addEventListener('play', () => this.onPlay());
            this.video.addEventListener('pause', () => this.onPause());
            this.video.addEventListener('ended', () => this.onEnded());
        }
    }

    checkAllFilesLoaded() {
        const allLoaded = this.videoUpload?.files.length > 0 &&
                         this.logUpload?.files.length > 0 &&
                         this.eventsUpload?.files.length > 0 &&
                         this.highlightsUpload?.files.length > 0;
        
        if (this.loadBtn) {
            this.loadBtn.disabled = !allLoaded;
        }
    }

    async loadVisualizer() {
        try {
            // Load video
            const videoFile = this.videoUpload?.files[0];
            if (videoFile && this.video) {
                const videoURL = URL.createObjectURL(videoFile);
                const videoSource = document.getElementById('video-source');
                if (videoSource) {
                    videoSource.src = videoURL;
                }
                this.video.load();
            }
            
            // Load JSON files
            this.logData = await this.readJSONFile(this.logUpload?.files[0]);
            this.eventsData = await this.readJSONFile(this.eventsUpload?.files[0]);
            this.highlightsData = await this.readJSONFile(this.highlightsUpload?.files[0]);
            
            // Initialize display
            this.initializeDisplay();
            
            // Hide upload section
            if (this.uploadSection) {
                this.uploadSection.classList.add('hidden');
            }
            
            console.log('Visualizer loaded successfully');
        } catch (error) {
            console.error('Error loading visualizer:', error);
            alert('Error loading files. Please check the console for details.');
        }
    }

    async readJSONFile(file) {
        if (!file) throw new Error('File not provided');
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (e) => {
                try {
                    const json = JSON.parse(e.target.result);
                    resolve(json);
                } catch (error) {
                    reject(error);
                }
            };
            reader.onerror = reject;
            reader.readAsText(file);
        });
    }

    initializeDisplay() {
        // Update statistics
        if (this.logData?.statistics) {
            const stats = this.logData.statistics;
            if (this.totalRalliesEl) this.totalRalliesEl.textContent = stats.totalRallies || 0;
            if (this.avgRallyEl) this.avgRallyEl.textContent = (stats.avgRallyLength || 0).toFixed(1);
            if (this.maxSpeedEl) this.maxSpeedEl.textContent = `${(stats.maxBallSpeed || 0).toFixed(1)} km/h`;
            if (this.avgSpeedEl) this.avgSpeedEl.textContent = `${(stats.avgBallSpeed || 0).toFixed(1)} km/h`;
            
            if (this.player1ScoreEl) this.player1ScoreEl.textContent = stats.player1Score || 0;
            if (this.player2ScoreEl) this.player2ScoreEl.textContent = stats.player2Score || 0;
        }
        
        // Process shot data for speed charts
        this.processPlayerSpeedData();
        
        // Initialize speed charts
        this.initializeSpeedCharts();
        
        // Build timeline
        this.buildTimeline();
        
        // Build events list
        this.buildEventsList();
        
        // Build highlights
        this.buildHighlightsList();
    }

    processPlayerSpeedData() {
        if (!this.logData?.shots) return;
        
        // Pre-calculate player statistics
        this.logData.shots.forEach(shot => {
            const speed = shot.speed || 0;
            const player = shot.player || 1;
            
            if (player === 1) {
                this.player1MaxSpeed = Math.max(this.player1MaxSpeed, speed);
                this.player1TotalSpeed += speed;
                this.player1ShotCount++;
            } else {
                this.player2MaxSpeed = Math.max(this.player2MaxSpeed, speed);
                this.player2TotalSpeed += speed;
                this.player2ShotCount++;
            }
        });
        
        // Update display
        if (this.player1MaxSpeedEl) {
            this.player1MaxSpeedEl.textContent = `${this.player1MaxSpeed.toFixed(1)} km/h`;
        }
        if (this.player1AvgSpeedEl) {
            const avg = this.player1ShotCount > 0 ? this.player1TotalSpeed / this.player1ShotCount : 0;
            this.player1AvgSpeedEl.textContent = `${avg.toFixed(1)} km/h`;
        }
        if (this.player2MaxSpeedEl) {
            this.player2MaxSpeedEl.textContent = `${this.player2MaxSpeed.toFixed(1)} km/h`;
        }
        if (this.player2AvgSpeedEl) {
            const avg = this.player2ShotCount > 0 ? this.player2TotalSpeed / this.player2ShotCount : 0;
            this.player2AvgSpeedEl.textContent = `${avg.toFixed(1)} km/h`;
        }
    }

    initializeSpeedCharts() {
        if (this.player1SpeedCanvas) {
            this.player1SpeedCanvas.width = this.player1SpeedCanvas.offsetWidth * 2;
            this.player1SpeedCanvas.height = this.player1SpeedCanvas.offsetHeight * 2;
        }
        if (this.player2SpeedCanvas) {
            this.player2SpeedCanvas.width = this.player2SpeedCanvas.offsetWidth * 2;
            this.player2SpeedCanvas.height = this.player2SpeedCanvas.offsetHeight * 2;
        }
        this.drawSpeedChart(1);
        this.drawSpeedChart(2);
    }

    drawSpeedChart(player) {
        const canvas = player === 1 ? this.player1SpeedCanvas : this.player2SpeedCanvas;
        const ctx = player === 1 ? this.player1SpeedCtx : this.player2SpeedCtx;
        const data = player === 1 ? this.player1SpeedData : this.player2SpeedData;
        const maxSpeed = Math.max(player === 1 ? this.player1MaxSpeed : this.player2MaxSpeed, 20);
        
        if (!canvas || !ctx) return;
        
        const width = canvas.width;
        const height = canvas.height;
        const padding = 40;
        const chartWidth = width - padding * 2;
        const chartHeight = height - padding * 2;
        
        // Clear canvas
        ctx.clearRect(0, 0, width, height);
        
        // Draw background
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, width, height);
        
        // Draw grid lines
        ctx.strokeStyle = '#e0e0e0';
        ctx.lineWidth = 1;
        const gridLines = 5;
        for (let i = 0; i <= gridLines; i++) {
            const y = padding + (chartHeight / gridLines) * i;
            ctx.beginPath();
            ctx.moveTo(padding, y);
            ctx.lineTo(width - padding, y);
            ctx.stroke();
            
            // Draw y-axis labels
            const speedValue = maxSpeed - (maxSpeed / gridLines) * i;
            ctx.fillStyle = '#666';
            ctx.font = '20px sans-serif';
            ctx.textAlign = 'right';
            ctx.fillText(speedValue.toFixed(0), padding - 10, y + 7);
        }
        
        // Draw x-axis label
        ctx.fillStyle = '#666';
        ctx.font = '20px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('Time →', width / 2, height - 10);
        
        // Draw data line
        if (data.length > 1) {
            ctx.strokeStyle = player === 1 ? '#667eea' : '#e91e63';
            ctx.lineWidth = 3;
            ctx.beginPath();
            
            data.forEach((point, index) => {
                const x = padding + (chartWidth / (this.maxChartDataPoints - 1)) * index;
                const y = padding + chartHeight - (point.speed / maxSpeed) * chartHeight;
                
                if (index === 0) {
                    ctx.moveTo(x, y);
                } else {
                    ctx.lineTo(x, y);
                }
            });
            
            ctx.stroke();
            
            // Draw data points
            ctx.fillStyle = player === 1 ? '#667eea' : '#e91e63';
            data.forEach((point, index) => {
                const x = padding + (chartWidth / (this.maxChartDataPoints - 1)) * index;
                const y = padding + chartHeight - (point.speed / maxSpeed) * chartHeight;
                
                ctx.beginPath();
                ctx.arc(x, y, 4, 0, Math.PI * 2);
                ctx.fill();
            });
        }
        
        // Draw axes
        ctx.strokeStyle = '#333';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(padding, padding);
        ctx.lineTo(padding, height - padding);
        ctx.lineTo(width - padding, height - padding);
        ctx.stroke();
    }

    buildTimeline() {
        if (!this.timelineEl || !this.eventsData || !this.logData) return;
        
        const duration = this.logData.durationSeconds * 1000; // ms
        
        // Clear existing markers
        this.timelineEl.innerHTML = '<div class="timeline-current" id="timeline-current"></div>';
        
        // Add event markers
        this.eventsData.forEach((event, index) => {
            const marker = document.createElement('div');
            marker.className = `timeline-marker ${event.type}`;
            marker.style.left = `${(event.timestampMs / duration) * 100}%`;
            marker.title = `${event.title} - ${this.formatTime(event.timestampMs / 1000)}`;
            marker.addEventListener('click', () => this.seekToTime(event.timestampMs / 1000));
            this.timelineEl.appendChild(marker);
        });
    }

    buildEventsList() {
        if (!this.eventsListEl || !this.eventsData) return;
        
        this.eventsListEl.innerHTML = '';
        
        if (this.eventsData.length === 0) {
            this.eventsListEl.innerHTML = '<p class="no-data">No events</p>';
            return;
        }
        
        this.eventsData.forEach((event, index) => {
            const eventItem = document.createElement('div');
            eventItem.className = 'event-item';
            eventItem.dataset.index = index;
            eventItem.innerHTML = `
                <div class="event-title">${event.title}</div>
                <div class="event-description">${event.description || ''}</div>
                <div class="event-time">${this.formatTime(event.timestampMs / 1000)}</div>
            `;
            eventItem.addEventListener('click', () => this.seekToTime(event.timestampMs / 1000));
            this.eventsListEl.appendChild(eventItem);
        });
    }

    buildHighlightsList() {
        if (!this.highlightsListEl || !this.highlightsData) return;
        
        this.highlightsListEl.innerHTML = '';
        
        const highlights = [];
        
        // Add play of the game
        if (this.highlightsData.playOfTheGame) {
            highlights.push({
                title: '⭐ Play of the Game',
                timestamp: this.highlightsData.playOfTheGame.timestampMs
            });
        }
        
        // Add top rallies
        if (this.highlightsData.topRallies?.length > 0) {
            this.highlightsData.topRallies.forEach((rally, i) => {
                highlights.push({
                    title: `🏆 Top Rally #${i + 1}`,
                    timestamp: rally.timestampMs
                });
            });
        }
        
        // Add fastest shots
        if (this.highlightsData.fastestShots?.length > 0) {
            this.highlightsData.fastestShots.forEach((shot, i) => {
                highlights.push({
                    title: `⚡ Fastest Shot #${i + 1}`,
                    timestamp: shot.timestampMs
                });
            });
        }
        
        // Add best serves
        if (this.highlightsData.bestServes?.length > 0) {
            this.highlightsData.bestServes.forEach((serve, i) => {
                highlights.push({
                    title: `🎯 Best Serve #${i + 1}`,
                    timestamp: serve.timestampMs
                });
            });
        }
        
        if (highlights.length === 0) {
            this.highlightsListEl.innerHTML = '<p class="no-data">No highlights yet</p>';
            return;
        }
        
        highlights.forEach(highlight => {
            const item = document.createElement('div');
            item.className = 'highlight-item';
            item.innerHTML = `
                <div class="highlight-title">${highlight.title}</div>
                <div class="highlight-time">${this.formatTime(highlight.timestamp / 1000)}</div>
            `;
            item.addEventListener('click', () => this.seekToTime(highlight.timestamp / 1000));
            this.highlightsListEl.appendChild(item);
        });
    }

    onVideoLoaded() {
        if (!this.video) return;
        
        const duration = this.video.duration;
        if (this.seekBar) {
            this.seekBar.max = duration;
        }
        if (this.durationDisplay) {
            this.durationDisplay.textContent = this.formatTime(duration);
        }
        
        // Setup canvas
        if (this.canvas && this.video) {
            this.canvas.width = this.video.videoWidth;
            this.canvas.height = this.video.videoHeight;
        }
    }

    onTimeUpdate() {
        if (!this.video) return;
        
        const currentTime = this.video.currentTime;
        const currentMs = currentTime * 1000;
        
        // Update seek bar
        if (this.seekBar && !this.seekBar.matches(':active')) {
            this.seekBar.value = currentTime;
        }
        
        // Update time display
        if (this.currentTimeDisplay) {
            this.currentTimeDisplay.textContent = this.formatTime(currentTime);
        }
        
        // Update timeline cursor
        const timelineCurrent = document.getElementById('timeline-current');
        if (timelineCurrent && this.logData) {
            const duration = this.logData.durationSeconds * 1000;
            timelineCurrent.style.left = `${(currentMs / duration) * 100}%`;
        }
        
        // Update current event
        this.updateCurrentEvent(currentMs);
        
        // Update current shot
        this.updateCurrentShot(currentMs);
        
        // Draw overlay
        this.drawOverlay(currentMs);
    }

    updateCurrentEvent(currentMs) {
        if (!this.eventsData) return;
        
        // Find current event (within 500ms window)
        let newEventIndex = -1;
        for (let i = 0; i < this.eventsData.length; i++) {
            const event = this.eventsData[i];
            if (Math.abs(event.timestampMs - currentMs) < 500) {
                newEventIndex = i;
                break;
            }
        }
        
        // Show event notification if new event
        if (newEventIndex !== this.currentEventIndex && newEventIndex !== -1) {
            this.showEventNotification(this.eventsData[newEventIndex]);
        }
        
        this.currentEventIndex = newEventIndex;
        
        // Highlight current event in list
        const eventItems = this.eventsListEl?.querySelectorAll('.event-item');
        eventItems?.forEach((item, index) => {
            if (index === newEventIndex) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });
    }

    updateCurrentShot(currentMs) {
        if (!this.logData?.shots) return;
        
        // Find current shot
        let currentShot = null;
        for (const shot of this.logData.shots) {
            const shotTime = shot.timestampMs || (shot.frame / 120 * 1000); // Assume 120fps if timestampMs missing
            if (Math.abs(shotTime - currentMs) < 100) { // 100ms window
                currentShot = shot;
                break;
            }
        }
        
        // Update shot info display
        if (currentShot) {
            if (this.currentShotSpeedEl) {
                this.currentShotSpeedEl.textContent = `${(currentShot.speed || 0).toFixed(1)} km/h`;
            }
            if (this.currentShotTypeEl) {
                this.currentShotTypeEl.textContent = (currentShot.shotType || 'UNKNOWN').toUpperCase();
            }
            if (this.currentShotAccuracyEl) {
                this.currentShotAccuracyEl.textContent = `${(currentShot.accuracy || 0).toFixed(1)}%`;
            }
            if (this.currentShotResultEl) {
                this.currentShotResultEl.textContent = (currentShot.result || 'UNKNOWN').toUpperCase();
                this.currentShotResultEl.style.color = currentShot.result === 'IN' ? '#4caf50' : '#f44336';
            }
            
            // Update player speed charts
            this.updatePlayerSpeedChart(currentShot);
        } else {
            // Clear display
            if (this.currentShotSpeedEl) this.currentShotSpeedEl.textContent = '-- km/h';
            if (this.currentShotTypeEl) this.currentShotTypeEl.textContent = '--';
            if (this.currentShotAccuracyEl) this.currentShotAccuracyEl.textContent = '--%';
            if (this.currentShotResultEl) {
                this.currentShotResultEl.textContent = '--';
                this.currentShotResultEl.style.color = '#333';
            }
        }
    }

    updatePlayerSpeedChart(shot) {
        const player = shot.player || 1;
        const speed = shot.speed || 0;
        const timestamp = shot.timestampMs || (shot.frame / 120 * 1000);
        
        if (player === 1) {
            // Update current speed display
            if (this.player1CurrentSpeedEl) {
                this.player1CurrentSpeedEl.textContent = speed.toFixed(1);
            }
            
            // Add to data array
            this.player1SpeedData.push({ timestamp, speed });
            
            // Keep only recent data points
            if (this.player1SpeedData.length > this.maxChartDataPoints) {
                this.player1SpeedData.shift();
            }
            
            // Redraw chart
            this.drawSpeedChart(1);
        } else {
            // Update current speed display
            if (this.player2CurrentSpeedEl) {
                this.player2CurrentSpeedEl.textContent = speed.toFixed(1);
            }
            
            // Add to data array
            this.player2SpeedData.push({ timestamp, speed });
            
            // Keep only recent data points
            if (this.player2SpeedData.length > this.maxChartDataPoints) {
                this.player2SpeedData.shift();
            }
            
            // Redraw chart
            this.drawSpeedChart(2);
        }
    }

    drawOverlay(currentMs) {
        if (!this.ctx || !this.canvas || !this.logData?.shots) return;
        
        // Clear canvas
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        
        // Find shots near current time
        const nearbyShots = this.logData.shots.filter(shot => {
            const shotTime = shot.timestampMs || (shot.frame / 120 * 1000);
            return Math.abs(shotTime - currentMs) < 500; // 500ms window
        });
        
        // Draw ball positions
        nearbyShots.forEach(shot => {
            if (shot.detections && shot.detections.length > 0) {
                shot.detections.forEach(detection => {
                    this.drawBallPosition(detection);
                });
            }
        });
        
        // Draw trajectory if available
        const currentEvent = this.currentEventIndex >= 0 ? this.eventsData[this.currentEventIndex] : null;
        if (currentEvent?.metadata?.ballTrajectory) {
            this.drawTrajectory(currentEvent.metadata.ballTrajectory);
        }
    }

    drawBallPosition(detection) {
        if (!this.ctx) return;
        
        const x = detection.x + detection.width / 2;
        const y = detection.y + detection.height / 2;
        const radius = Math.max(detection.width, detection.height) / 2;
        
        // Draw ball circle
        this.ctx.beginPath();
        this.ctx.arc(x, y, radius, 0, Math.PI * 2);
        this.ctx.strokeStyle = '#FFD700';
        this.ctx.lineWidth = 3;
        this.ctx.stroke();
        
        // Draw confidence indicator
        this.ctx.fillStyle = `rgba(255, 215, 0, ${detection.confidence * 0.3})`;
        this.ctx.fill();
        
        // Draw bounding box
        this.ctx.strokeStyle = '#FF4500';
        this.ctx.lineWidth = 2;
        this.ctx.strokeRect(detection.x, detection.y, detection.width, detection.height);
    }

    drawTrajectory(trajectory) {
        if (!this.ctx || !trajectory || trajectory.length < 2) return;
        
        this.ctx.beginPath();
        this.ctx.strokeStyle = 'rgba(102, 126, 234, 0.6)';
        this.ctx.lineWidth = 2;
        
        trajectory.forEach((point, index) => {
            const [x, y] = point;
            if (index === 0) {
                this.ctx.moveTo(x, y);
            } else {
                this.ctx.lineTo(x, y);
            }
        });
        
        this.ctx.stroke();
        
        // Draw points
        trajectory.forEach(point => {
            const [x, y] = point;
            this.ctx.beginPath();
            this.ctx.arc(x, y, 3, 0, Math.PI * 2);
            this.ctx.fillStyle = '#667eea';
            this.ctx.fill();
        });
    }

    showEventNotification(event) {
        if (!this.eventNotification) return;
        
        this.eventNotification.textContent = `${event.title}: ${event.description || ''}`;
        this.eventNotification.classList.add('show');
        
        setTimeout(() => {
            this.eventNotification.classList.remove('show');
        }, 2000);
    }

    togglePlayPause() {
        if (!this.video) return;
        
        if (this.video.paused) {
            this.video.play();
        } else {
            this.video.pause();
        }
    }

    onPlay() {
        const playIcon = document.getElementById('play-icon');
        if (playIcon) {
            playIcon.textContent = '⏸';
        }
    }

    onPause() {
        const playIcon = document.getElementById('play-icon');
        if (playIcon) {
            playIcon.textContent = '▶';
        }
    }

    onEnded() {
        const playIcon = document.getElementById('play-icon');
        if (playIcon) {
            playIcon.textContent = '▶';
        }
    }

    seek(event) {
        if (!this.video) return;
        this.video.currentTime = parseFloat(event.target.value);
    }

    seekToTime(seconds) {
        if (!this.video) return;
        this.video.currentTime = seconds;
        if (this.video.paused) {
            this.video.play();
        }
    }

    changeSpeed(event) {
        if (!this.video) return;
        this.video.playbackRate = parseFloat(event.target.value);
    }

    formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    }
}

// Initialize visualizer when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    const visualizer = new MatchVisualizer();
    console.log('Match Visualizer initialized');
});
