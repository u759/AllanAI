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

        // Data sources
        this.statisticsData = null;
        this.momentumTimeline = [];

        // Runtime state
        this.currentScoreIndex = -1;
        this.currentScoreState = { player1: 0, player2: 0 };
        this.activeShot = null;
        this.activeShotKey = null;
        this.scoreToastTimeouts = [];
        this.estimatedFps = null;
        
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
    this.statisticsUpload = document.getElementById('statistics-upload');
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
    this.statisticsUpload?.addEventListener('change', () => this.checkAllFilesLoaded());
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
    const videoLoaded = this.videoUpload?.files.length > 0;
    const logLoaded = this.logUpload?.files.length > 0;
    const eventsLoaded = this.eventsUpload?.files.length > 0;
    const highlightsLoaded = this.highlightsUpload?.files.length > 0;
    const statsLoaded = this.statisticsUpload ? this.statisticsUpload.files.length > 0 : true;
    const allLoaded = videoLoaded && logLoaded && eventsLoaded && highlightsLoaded && statsLoaded;
        
        if (this.loadBtn) {
            this.loadBtn.disabled = !allLoaded;
        }
    }

    async loadVisualizer() {
        try {
            this.resetVisualizerState();

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
            if (Array.isArray(this.logData?.shots)) {
                this.logData.shots = this.logData.shots
                    .filter(shot => typeof shot === 'object' && shot !== null)
                    .sort((a, b) => {
                        const aTime = typeof a?.timestampMs === 'number' ? a.timestampMs : (Array.isArray(a?.timestampSeries) ? a.timestampSeries[0] : 0);
                        const bTime = typeof b?.timestampMs === 'number' ? b.timestampMs : (Array.isArray(b?.timestampSeries) ? b.timestampSeries[0] : 0);
                        return aTime - bTime;
                    });
            }
            const eventsRaw = await this.readJSONFile(this.eventsUpload?.files[0]);
            this.eventsData = Array.isArray(eventsRaw)
                ? eventsRaw.filter(event => typeof event?.timestampMs === 'number')
                : [];
            this.highlightsData = await this.readJSONFile(this.highlightsUpload?.files[0]);
            const hasSeparateStats = this.statisticsUpload?.files?.length > 0;
            if (hasSeparateStats) {
                this.statisticsData = await this.readJSONFile(this.statisticsUpload.files[0]);
            } else {
                this.statisticsData = this.logData?.statistics || null;
            }

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

    resetVisualizerState() {
        this.player1SpeedData = [];
        this.player2SpeedData = [];
        this.player1MaxSpeed = 0;
        this.player1TotalSpeed = 0;
        this.player1ShotCount = 0;
        this.player2MaxSpeed = 0;
        this.player2TotalSpeed = 0;
        this.player2ShotCount = 0;
        this.statisticsData = null;
        this.momentumTimeline = [];
        this.currentScoreIndex = -1;
        this.currentScoreState = { player1: 0, player2: 0 };
        this.activeShot = null;
        this.activeShotKey = null;
        this.estimatedFps = null;
        this.scoreToastTimeouts.forEach(clearTimeout);
        this.scoreToastTimeouts = [];
    document.querySelectorAll('.score-toast').forEach(node => node.remove());

        if (this.player1ScoreEl) {
            this.player1ScoreEl.textContent = '0';
            this.player1ScoreEl.style.backgroundColor = '';
        }
        if (this.player2ScoreEl) {
            this.player2ScoreEl.textContent = '0';
            this.player2ScoreEl.style.backgroundColor = '';
        }
        if (this.player1CurrentSpeedEl) this.player1CurrentSpeedEl.textContent = '--';
        if (this.player2CurrentSpeedEl) this.player2CurrentSpeedEl.textContent = '--';
    }

    initializeDisplay() {
        const stats = this.statisticsData || this.logData?.statistics || {};

        // Pre-calc fps from metadata if available
        const totalFrames = stats.totalFrames || this.logData?.statistics?.totalFrames;
        if (totalFrames && this.logData?.durationSeconds) {
            this.estimatedFps = totalFrames / this.logData.durationSeconds;
        }

        // Update headline statistics
        if (this.totalRalliesEl) this.totalRalliesEl.textContent = stats.totalRallies ?? stats?.rallyMetrics?.totalRallies ?? 0;
        if (this.avgRallyEl) this.avgRallyEl.textContent = ((stats.avgRallyLength ?? stats?.rallyMetrics?.averageRallyLength) || 0).toFixed(1);
        if (this.maxSpeedEl) this.maxSpeedEl.textContent = `${((stats.maxBallSpeed ?? stats?.shotSpeedMetrics?.fastestShotMph) || 0).toFixed(1)} mph`;
        if (this.avgSpeedEl) this.avgSpeedEl.textContent = `${((stats.avgBallSpeed ?? stats?.shotSpeedMetrics?.averageShotMph) || 0).toFixed(1)} mph`;

        // Configure momentum timeline and starting score
        this.momentumTimeline = Array.isArray(stats.momentumTimeline) ? [...stats.momentumTimeline] : [];
        if (this.momentumTimeline.length === 0 && Array.isArray(this.logData?.statistics?.momentumTimeline)) {
            this.momentumTimeline = [...this.logData.statistics.momentumTimeline];
        }
        this.momentumTimeline.sort((a, b) => (a?.timestampMs ?? 0) - (b?.timestampMs ?? 0));

        const initialScore = this.momentumTimeline.length > 0 && this.momentumTimeline[0]?.scoreAfter
            ? this.momentumTimeline[0].scoreAfter
            : { player1: stats.player1Score ?? 0, player2: stats.player2Score ?? 0 };
        this.applyScore(initialScore);

        // Populate extended statistic panels when available
        this.populateExtendedStats(stats);

        document.querySelectorAll('.speed-unit')
            .forEach(unitEl => { unitEl.textContent = 'mph'; });

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

    applyScore(scoreState) {
        const safePlayer1 = Number(scoreState?.player1 ?? 0);
        const safePlayer2 = Number(scoreState?.player2 ?? 0);
        this.currentScoreState = { player1: safePlayer1, player2: safePlayer2 };

        if (this.player1ScoreEl) {
            this.player1ScoreEl.textContent = safePlayer1;
            this.player1ScoreEl.dataset.score = safePlayer1;
        }
        if (this.player2ScoreEl) {
            this.player2ScoreEl.textContent = safePlayer2;
            this.player2ScoreEl.dataset.score = safePlayer2;
        }
    }

    populateExtendedStats(stats) {
        if (!stats) return;

        const serveMetrics = stats.serveMetrics || {};
        const returnMetrics = stats.returnMetrics || {};
        const shotSpeedMetrics = stats.shotSpeedMetrics || {};

        this.setElementText('serve-total', serveMetrics.totalServes);
        this.setElementText('serve-successful', serveMetrics.successfulServes);
        this.setElementText('serve-faults', serveMetrics.faults);
        this.setElementText('serve-success-rate', serveMetrics.successRate, '%');
        this.setElementText('serve-average-speed', serveMetrics.averageServeSpeed, ' mph');
        this.setElementText('serve-fastest-speed', serveMetrics.fastestServeSpeed, ' mph');

        this.setElementText('return-total', returnMetrics.totalReturns);
        this.setElementText('return-successful', returnMetrics.successfulReturns);
        this.setElementText('return-success-rate', returnMetrics.successRate, '%');
        this.setElementText('return-average-speed', returnMetrics.averageReturnSpeed, ' mph');

        this.setElementText('shot-fastest', shotSpeedMetrics.fastestShotMph, ' mph');
        this.setElementText('shot-average', shotSpeedMetrics.averageShotMph, ' mph');
        this.setElementText('shot-incoming', shotSpeedMetrics.averageIncomingShotMph, ' mph');
        this.setElementText('shot-outgoing', shotSpeedMetrics.averageOutgoingShotMph, ' mph');

        const shotBreakdownEl = document.getElementById('shot-type-breakdown');
        if (shotBreakdownEl) {
            shotBreakdownEl.innerHTML = '';
            if (Array.isArray(stats.shotTypeBreakdown) && stats.shotTypeBreakdown.length > 0) {
                stats.shotTypeBreakdown.forEach(item => {
                    const row = document.createElement('div');
                    row.className = 'breakdown-row';
                    row.innerHTML = `
                        <span class="breakdown-label">${item.shotType}</span>
                        <span class="breakdown-value">${item.count}</span>
                        <span class="breakdown-value">${(item.averageSpeed ?? 0).toFixed(1)} mph</span>
                        <span class="breakdown-value">${(item.averageAccuracy ?? 0).toFixed(1)}%</span>
                    `;
                    shotBreakdownEl.appendChild(row);
                });
            } else {
                shotBreakdownEl.innerHTML = '<p class="no-data">No data</p>';
            }
        }

        const playerBreakdownEl = document.getElementById('player-breakdown');
        if (playerBreakdownEl) {
            playerBreakdownEl.innerHTML = '';
            if (Array.isArray(stats.playerBreakdown) && stats.playerBreakdown.length > 0) {
                stats.playerBreakdown.forEach(playerStats => {
                    const block = document.createElement('div');
                    block.className = 'player-breakdown-card';
                    block.innerHTML = `
                        <h4>Player ${playerStats.player}</h4>
                        <p>Points Won: ${playerStats.totalPointsWon}</p>
                        <p>Shot Speed: ${(playerStats.averageShotSpeed ?? 0).toFixed(1)} mph</p>
                        <p>Accuracy: ${(playerStats.averageAccuracy ?? 0).toFixed(1)}%</p>
                        <p>Serve Success: ${(playerStats.serveSuccessRate ?? 0).toFixed(1)}%</p>
                        <p>Return Success: ${(playerStats.returnSuccessRate ?? 0).toFixed(1)}%</p>
                    `;
                    playerBreakdownEl.appendChild(block);
                });
            } else {
                playerBreakdownEl.innerHTML = '<p class="no-data">No data</p>';
            }
        }

        const momentumListEl = document.getElementById('momentum-list');
        if (momentumListEl) {
            momentumListEl.innerHTML = '';
            if (Array.isArray(this.momentumTimeline) && this.momentumTimeline.length > 0) {
                this.momentumTimeline.forEach(sample => {
                    const item = document.createElement('div');
                    item.className = 'momentum-item';
                    const scoreAfter = sample.scoreAfter || { player1: 0, player2: 0 };
                    const scorer = sample.scoringPlayer ? `Player ${sample.scoringPlayer}` : 'Start';
                    item.innerHTML = `
                        <span class="momentum-time">${this.formatTime((sample.timestampMs || 0) / 1000)}</span>
                        <span class="momentum-scorer">${scorer}</span>
                        <span class="momentum-score">${scoreAfter.player1}-${scoreAfter.player2}</span>
                    `;
                    momentumListEl.appendChild(item);
                });
            } else {
                momentumListEl.innerHTML = '<p class="no-data">No momentum samples</p>';
            }
        }
    }

    setElementText(id, value, suffix = '') {
        const element = document.getElementById(id);
        if (!element) return;
        if (value === undefined || value === null || Number.isNaN(value)) {
            if (suffix && suffix.trim().length > 0) {
                const trimmed = suffix.trim();
                if (suffix.startsWith(' ')) {
                    element.textContent = `--${suffix}`;
                } else if (trimmed === '%') {
                    element.textContent = '--%';
                } else {
                    element.textContent = `-- ${suffix}`;
                }
            } else {
                element.textContent = '--';
            }
        } else if (typeof value === 'number') {
            element.textContent = `${value.toFixed(1)}${suffix}`.replace('.0', '');
        } else {
            element.textContent = `${value}${suffix}`;
        }
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
            this.player1MaxSpeedEl.textContent = `${this.player1MaxSpeed.toFixed(1)} mph`;
        }
        if (this.player1AvgSpeedEl) {
            const avg = this.player1ShotCount > 0 ? this.player1TotalSpeed / this.player1ShotCount : 0;
            this.player1AvgSpeedEl.textContent = `${avg.toFixed(1)} mph`;
        }
        if (this.player2MaxSpeedEl) {
            this.player2MaxSpeedEl.textContent = `${this.player2MaxSpeed.toFixed(1)} mph`;
        }
        if (this.player2AvgSpeedEl) {
            const avg = this.player2ShotCount > 0 ? this.player2TotalSpeed / this.player2ShotCount : 0;
            this.player2AvgSpeedEl.textContent = `${avg.toFixed(1)} mph`;
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
        
    const durationSeconds = this.logData?.durationSeconds ?? (this.video?.duration ?? 1);
    const duration = Math.max(durationSeconds, 1) * 1000; // ms
        
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

        // Add score markers
        if (Array.isArray(this.momentumTimeline)) {
            this.momentumTimeline.forEach((sample, index) => {
                if (index === 0) return; // skip initial state
                const scoreMarker = document.createElement('div');
                scoreMarker.className = 'timeline-marker score-marker';
                scoreMarker.style.left = `${((sample.timestampMs || 0) / duration) * 100}%`;
                scoreMarker.style.background = '#4caf50';
                scoreMarker.style.width = '6px';
                scoreMarker.style.height = '16px';
                scoreMarker.title = `Score ${sample.scoreAfter?.player1 ?? 0}-${sample.scoreAfter?.player2 ?? 0}`;
                scoreMarker.addEventListener('click', () => this.seekToTime((sample.timestampMs || 0) / 1000));
                this.timelineEl.appendChild(scoreMarker);
            });
        }
    }

    buildEventsList() {
        if (!this.eventsListEl || !this.eventsData) return;
        
        this.eventsListEl.innerHTML = '';
        
        const validEvents = this.eventsData.filter(event => typeof event?.timestampMs === 'number');

        if (validEvents.length === 0) {
            this.eventsListEl.innerHTML = '<p class="no-data">No events</p>';
            return;
        }
        
        validEvents.forEach((event, index) => {
            const eventItem = document.createElement('div');
            eventItem.className = 'event-item';
            eventItem.dataset.index = index;
            const typeLabel = event.type ? `<span class="event-type">${event.type}</span>` : '';
            const playerLabel = Number.isFinite(event.player) ? `<span class="event-player">P${event.player}</span>` : '';
            const speedValue = typeof event?.metadata?.shotSpeed === 'number' ? event.metadata.shotSpeed.toFixed(1) : null;
            const speedLabel = speedValue ? `<span class="event-speed">${speedValue} mph</span>` : '';
            eventItem.innerHTML = `
                <div class="event-title">${event.title}</div>
                <div class="event-meta">${typeLabel}${playerLabel}${speedLabel}</div>
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
        if (timelineCurrent) {
            const durationSeconds = this.logData?.durationSeconds ?? (this.video?.duration ?? 1);
            const duration = Math.max(durationSeconds, 1) * 1000;
            timelineCurrent.style.left = `${Math.min(100, (currentMs / duration) * 100)}%`;
        }
        
        // Update score progression
        this.updateScore(currentMs);

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

    updateScore(currentMs) {
        if (!Array.isArray(this.momentumTimeline) || this.momentumTimeline.length === 0) {
            return;
        }

        let targetIndex = this.currentScoreIndex;
        for (let i = 0; i < this.momentumTimeline.length; i++) {
            const eventTime = this.momentumTimeline[i]?.timestampMs ?? 0;
            if (currentMs >= eventTime) {
                targetIndex = i;
            } else {
                break;
            }
        }

        if (targetIndex === -1 || targetIndex === this.currentScoreIndex) {
            return;
        }

        const entry = this.momentumTimeline[targetIndex];
        const scoreAfter = entry?.scoreAfter || this.currentScoreState;
        const previousScore = { ...this.currentScoreState };

        this.applyScore(scoreAfter);
        this.currentScoreIndex = targetIndex;

        if (entry?.scoringPlayer) {
            const playerKey = `player${entry.scoringPlayer}`;
            const previous = previousScore[playerKey] ?? 0;
            const current = this.currentScoreState[playerKey] ?? 0;
            if (current > previous) {
                this.showScoreNotification(entry.scoringPlayer, scoreAfter);
            }
        }
    }

    showScoreNotification(playerNumber, scoreAfter) {
        const scoreElement = playerNumber === 1 ? this.player1ScoreEl : this.player2ScoreEl;
        if (!scoreElement) return;

        const container = scoreElement.closest('.player-score') || scoreElement.parentElement || scoreElement;
        if (!container) return;

        if (getComputedStyle(container).position === 'static') {
            container.style.position = 'relative';
        }

        const toast = document.createElement('div');
    toast.className = 'score-toast';
    toast.textContent = `+1`;
        toast.style.position = 'absolute';
        toast.style.top = '-28px';
        toast.style.left = '50%';
        toast.style.transform = 'translateX(-50%)';
        toast.style.padding = '4px 10px';
        toast.style.borderRadius = '999px';
        toast.style.background = playerNumber === 1 ? 'rgba(102, 126, 234, 0.95)' : 'rgba(233, 30, 99, 0.95)';
        toast.style.color = '#fff';
        toast.style.fontWeight = '700';
        toast.style.boxShadow = '0 2px 6px rgba(0,0,0,0.2)';
        toast.style.zIndex = '10';
    toast.dataset.score = `${scoreAfter?.player1 ?? 0}-${scoreAfter?.player2 ?? 0}`;

        container.appendChild(toast);

        scoreElement.style.transition = 'background-color 0.3s ease';
        scoreElement.style.backgroundColor = 'rgba(255, 235, 59, 0.6)';

        const clearHighlight = setTimeout(() => {
            scoreElement.style.backgroundColor = '';
        }, 600);
        const removeToast = setTimeout(() => {
            toast.remove();
        }, 1500);

        this.scoreToastTimeouts.push(clearHighlight, removeToast);
    }

    updateCurrentShot(currentMs) {
        if (!Array.isArray(this.logData?.shots) || this.logData.shots.length === 0) {
            return;
        }

        let closestShot = null;
        let closestDiff = Infinity;

        for (const shot of this.logData.shots) {
            const shotTime = this.getShotTimestamp(shot);
            if (shotTime === null) continue;
            const diff = Math.abs(shotTime - currentMs);
            if (diff < closestDiff) {
                closestDiff = diff;
                closestShot = shot;
            }
        }

        if (closestShot && closestDiff <= 400) {
            const shotKey = this.buildShotKey(closestShot);
            if (shotKey !== this.activeShotKey) {
                this.activeShotKey = shotKey;
                this.activeShot = closestShot;
                this.renderShotDetails(closestShot);
                this.updatePlayerSpeedChart(closestShot);
            }
        } else if (this.activeShot) {
            this.clearShotDetails();
            this.activeShot = null;
            this.activeShotKey = null;
        }
    }

    updatePlayerSpeedChart(shot) {
        const player = shot.player || 1;
    const speed = Number(shot.speed || 0);
    const timestamp = this.getShotTimestamp(shot) ?? Date.now();
        
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

    getShotTimestamp(shot) {
        if (typeof shot?.timestampMs === 'number') {
            return shot.timestampMs;
        }
        if (Array.isArray(shot?.timestampSeries) && shot.timestampSeries.length > 0) {
            return shot.timestampSeries[0];
        }
        if (Array.isArray(shot?.frameSeries) && shot.frameSeries.length > 0 && this.estimatedFps) {
            return (shot.frameSeries[0] / this.estimatedFps) * 1000;
        }
        if (Array.isArray(shot?.detections) && shot.detections.length > 0 && this.estimatedFps) {
            const frame = shot.detections[0].frameNumber;
            if (typeof frame === 'number') {
                return (frame / this.estimatedFps) * 1000;
            }
        }
        return null;
    }

    buildShotKey(shot) {
        const stamp = this.getShotTimestamp(shot) ?? Math.random();
        const player = shot?.player ?? 0;
        const shotType = shot?.shotType ?? 'unknown';
        return `${player}-${shotType}-${stamp}`;
    }

    renderShotDetails(shot) {
        const speed = Number(shot?.speed ?? 0);
        const shotType = shot?.shotType ? String(shot.shotType).toUpperCase() : 'UNKNOWN';
        const accuracy = Number(shot?.accuracy ?? 0);
        const result = shot?.result ? String(shot.result).toUpperCase() : 'UNKNOWN';

        if (this.currentShotSpeedEl) {
            this.currentShotSpeedEl.textContent = `${speed.toFixed(1)} mph`;
        }
        if (this.currentShotTypeEl) {
            this.currentShotTypeEl.textContent = shotType;
        }
        if (this.currentShotAccuracyEl) {
            this.currentShotAccuracyEl.textContent = `${accuracy.toFixed(1)}%`;
        }
        if (this.currentShotResultEl) {
            this.currentShotResultEl.textContent = result;
            this.currentShotResultEl.style.color = result === 'IN' ? '#4caf50' : '#f44336';
        }
    }

    clearShotDetails() {
        if (this.currentShotSpeedEl) this.currentShotSpeedEl.textContent = '-- mph';
        if (this.currentShotTypeEl) this.currentShotTypeEl.textContent = '--';
        if (this.currentShotAccuracyEl) this.currentShotAccuracyEl.textContent = '--%';
        if (this.currentShotResultEl) {
            this.currentShotResultEl.textContent = '--';
            this.currentShotResultEl.style.color = '#333';
        }
    }

    pickDetectionForShot(shot, currentMs) {
        if (!Array.isArray(shot?.detections) || shot.detections.length === 0) {
            return null;
        }

        let targetFrame = null;
        if (this.estimatedFps) {
            targetFrame = Math.round((currentMs / 1000) * this.estimatedFps);
        }

        const primaryFrame = Array.isArray(shot.frameSeries) && shot.frameSeries.length > 0
            ? shot.frameSeries[0]
            : null;

        let bestDetection = shot.detections[0];
        let bestScore = Infinity;

        shot.detections.forEach(detection => {
            const frameNumber = detection.frameNumber ?? primaryFrame ?? 0;
            const confidence = detection.confidence ?? 0;
            let frameDiff = 0;
            if (targetFrame !== null && typeof frameNumber === 'number') {
                frameDiff = Math.abs(frameNumber - targetFrame);
            } else if (typeof frameNumber === 'number' && typeof primaryFrame === 'number') {
                frameDiff = Math.abs(frameNumber - primaryFrame);
            }

            const score = frameDiff - confidence * 10;
            if (score < bestScore) {
                bestScore = score;
                bestDetection = detection;
            }
        });

        return bestDetection;
    }

    drawOverlay(currentMs) {
        if (!this.ctx || !this.canvas || !this.logData?.shots) return;
        
        // Clear canvas
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        
        const shotsToRender = [];

        if (this.activeShot) {
            shotsToRender.push(this.activeShot);
        } else {
            let fallbackShot = null;
            let fallbackDiff = Infinity;
            for (const shot of this.logData.shots) {
                const shotTime = this.getShotTimestamp(shot);
                if (shotTime === null) continue;
                const diff = Math.abs(shotTime - currentMs);
                if (diff < fallbackDiff && diff < 500) {
                    fallbackDiff = diff;
                    fallbackShot = shot;
                }
            }
            if (fallbackShot) {
                shotsToRender.push(fallbackShot);
            }
        }

        shotsToRender.forEach(shot => {
            const detection = this.pickDetectionForShot(shot, currentMs);
            if (detection) {
                this.drawBallPosition(detection, shot);
            }
        });
        
        // Draw trajectory if available
        const currentEvent = this.currentEventIndex >= 0 ? this.eventsData[this.currentEventIndex] : null;
        if (currentEvent?.metadata?.ballTrajectory) {
            this.drawTrajectory(currentEvent.metadata.ballTrajectory);
        }
    }

    drawBallPosition(detection, shot) {
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

        if (shot && typeof shot.speed === 'number' && shot.speed > 0) {
            const speedLabel = `${shot.speed.toFixed(1)} mph`;
            this.ctx.font = 'bold 20px "Segoe UI", sans-serif';
            this.ctx.textAlign = 'center';
            this.ctx.lineWidth = 4;
            this.ctx.strokeStyle = 'rgba(0, 0, 0, 0.6)';
            this.ctx.strokeText(speedLabel, x, y - radius - 12);
            this.ctx.fillStyle = '#00e5ff';
            this.ctx.fillText(speedLabel, x, y - radius - 12);
        }
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
