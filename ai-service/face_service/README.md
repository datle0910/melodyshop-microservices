# MelodyShop AI Service - Face Recognition Module

## Overview

FastAPI-based Python service for face recognition using `face_recognition` (dlib).

## Features

- **Face Detection** using HOG (Histogram of Oriented Gradients)
- **Face Encoding** extracting 128-D vectors using ResNet or HOG
- **Face Verification** using cosine similarity
- **Image Quality Validation** (blurry detection, multi-face rejection)

## Quick Start

### Local Development

```bash
cd ai-service/face_service
pip install -r requirements.txt
python main.py
```

Service runs at `http://localhost:8093`

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| POST | `/extract-embedding` | Extract 128-D face embedding from image |
| POST | `/verify-face` | Compare captured face against stored embedding |

### Extract Embedding

```bash
curl -X POST http://localhost:8093/extract-embedding \
  -H "Content-Type: application/json" \
  -d '{"image": "data:image/jpeg;base64,<base64_string>"}'
```

Response:
```json
{
  "success": true,
  "embedding": [0.123, -0.456, ...],
  "face_count": 1,
  "image_width": 640,
  "image_height": 480,
  "message": "Embedding extracted successfully.",
  "request_id": "a1b2c3d4"
}
```

### Verify Face

```bash
curl -X POST http://localhost:8093/verify-face \
  -H "Content-Type: application/json" \
  -d '{
    "image": "data:image/jpeg;base64,<base64_string>",
    "stored_embedding": [0.123, -0.456, ...],
    "similarity_threshold": 0.6
  }'
```

Response:
```json
{
  "success": true,
  "matched": true,
  "similarity": 0.8234,
  "face_count": 1,
  "message": "Face verified successfully (similarity=0.8234, threshold=0.60).",
  "request_id": "e5f6g7h8"
}
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8093` | Service port |
| `HOST` | `0.0.0.0` | Service host |

## Docker

```bash
cd ai-service
docker build -t melodyshop-ai-face -f Dockerfile.face .
docker run -p 8093:8093 --name melodyshop-ai-face melodyshop-ai-face
```

## Similarity Threshold Guide

| Threshold | Security Level | Use Case |
|-----------|---------------|----------|
| 0.5 | Low | Lenient matching |
| 0.6 | Medium | Balanced (default) |
| 0.7 | High | Recommended for production |
| 0.8 | Very High | Strict, may reject legitimate users |

## Notes

- `face_recognition` uses `dlib` which requires a C++ compiler and CMake for installation on some platforms
- For CPU-only deployment, `face_recognition` works without GPU
- Images are processed in memory; raw images are NOT stored permanently
- Only 128-D embedding vectors are stored in the database
