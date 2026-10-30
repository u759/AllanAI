# Model integration and API reference

## Ball-detector weights (`best.pt`)

`backend/backend/cv/best.pt` is a single-class YOLO (Ultralytics YOLO11) ball detector from the
[ping-pong-deep-learning](https://github.com/ccs-cs1l-f24/ping-pong-deep-learning) project, trained on the
[OpenTTGames](https://lab.osai.ai/) dataset. The event-spotting approach follows TTNet
([Voeikov et al., CVPR 2020 Workshops, arXiv:2004.09927](https://arxiv.org/abs/2004.09927)).

To regenerate the weights, use the upstream repo:

```bash
git clone https://github.com/ccs-cs1l-f24/ping-pong-deep-learning.git
cd ping-pong-deep-learning
pip install -r requirements.txt
cd prepare_dataset && python download_dataset.py && python unzip.py && python extract_ball_images_and_labels.py
cd ../src && python train.py   # writes train/weights/best.pt
```

Copy the result to `backend/backend/cv/best.pt`, or point `YOLO_MODEL_WEIGHTS` at it. If neither exists,
`model_service.py` falls back to `YOLO_FALLBACK_MODEL` (default `yolo11n.pt`), which is not trained on table-tennis balls.

## How the backend calls the model

`ModelEventDetectionService` runs the command list `allanai.model.command[*]` from
`application.properties` in `allanai.model.working-directory` (default `backend/backend`, relative to the
repo root). Placeholders such as `{video}`, `{outputDir}` and `{matchId}` are substituted before launch:

```
python cv/model_service.py <video> <outputDir>/<matchId>-inference.json [confidence]
```

The process has a timeout (`allanai.model.command-timeout`, default 5 minutes). A non-zero exit code, a
timeout or a missing or invalid output file marks the match `FAILED`. There is no heuristic fallback.

## Inference JSON contract

The output file is read into `ModelInferenceResult`. Top-level fields:

| Field | Meaning |
|---|---|
| `fps` | Frame rate of the analysed video |
| `events[]` | Bounces, net hits, scores and similar. Each has `frame`, `timestampMs`, `type`, `confidence`, and optionally `timestampSeries`, `frameSeries`, `player`, `importance`, `rallyLength`, `shotSpeed`, `shotType`, `ballTrajectory`, `preEventFrames`, `postEventFrames` and `detections[]` |
| `shots[]` | Per-hit records with `frame`, `timestampMs`, `player`, `speed`, `shotType`, `result`, `confidence` and `detections[]` |
| `statistics` | Match summary such as `player1Score`, `player2Score`, `totalRallies`, `avgRallyLength`, `avgBallSpeed`, `maxBallSpeed` |

A detection is `{ frameNumber, x, y, width, height, confidence }` in video pixel coordinates.
The backend adds IDs, titles, event windows, highlights and the processing summary itself.

## REST API

All endpoints except sign-up and sign-in need an `Authorization: Bearer <token>` header.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/users` | Sign up, returns a JWT |
| `POST` | `/api/users/signin` | Sign in, returns a JWT |
| `GET` | `/api/users/{userId}` | Get a profile |
| `PUT` | `/api/users/{userId}` | Update a profile |
| `PUT` | `/api/users/{userId}/password` | Change password |
| `POST` | `/api/matches/upload` | Upload a video (multipart field `video`), returns `202` with a match ID |
| `GET` | `/api/matches` | List matches |
| `GET` | `/api/matches/{id}` | Match details |
| `GET` | `/api/matches/{id}/status` | Processing status (`UPLOADED`, `PROCESSING`, `COMPLETE`, `FAILED`) |
| `GET` | `/api/matches/{id}/statistics` | Match statistics |
| `GET` | `/api/matches/{id}/events` | Detected events |
| `GET` | `/api/matches/{id}/highlights` | Highlights |
| `GET` | `/api/matches/{id}/video` | Stream the match video |
| `DELETE` | `/api/matches/{id}` | Delete a match and its video |
