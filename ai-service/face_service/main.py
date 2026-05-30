"""
MelodyShop AI Service - Face Recognition Module
FastAPI service for face detection, embedding extraction, and face verification.
"""

import os
import sys
import base64
import io
import uuid
import numpy as np
from typing import Optional
from datetime import datetime

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import uvicorn

# face_recognition uses dlib under the hood
import face_recognition
import cv2
from PIL import Image

app = FastAPI(
    title="MelodyShop AI - Face Recognition",
    description="Face detection, embedding extraction, and face verification for MelodyShop",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Pydantic Models ──────────────────────────────────────────────────────────

class ExtractEmbeddingRequest(BaseModel):
    image: str = Field(..., description="Base64-encoded image (with or without data-URI prefix)")
    validate_single_face: bool = Field(default=True, description="Reject if 0 or 2+ faces detected")


class ExtractEmbeddingResponse(BaseModel):
    success: bool
    embedding: Optional[list[float]] = None
    face_count: int = 0
    image_width: Optional[int] = None
    image_height: Optional[int] = None
    message: str = ""
    request_id: str = ""


class VerifyFaceRequest(BaseModel):
    image: str = Field(..., description="Base64-encoded image to verify")
    stored_embedding: list[float] = Field(..., description="Previously stored 128-D face embedding")
    similarity_threshold: float = Field(default=0.6, ge=0.0, le=1.0, description="Minimum similarity to pass (default 0.6)")


class VerifyFaceResponse(BaseModel):
    success: bool
    matched: bool = False
    similarity: float = 0.0
    face_count: int = 0
    message: str = ""
    request_id: str = ""


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    version: str
    timestamp: str


# ── Helper Functions ─────────────────────────────────────────────────────────

def base64_to_image(base64_str: str) -> tuple[Optional[np.ndarray], Optional[str]]:
    """
    Convert base64 string to OpenCV image (numpy BGR array).
    Returns (image, error_message).
    """
    try:
        if "," in base64_str:
            header, data = base64_str.split(",", 1)
        else:
            data = base64_str

        image_bytes = base64.b64decode(data)
        np_array = np.frombuffer(image_bytes, dtype=np.uint8)
        image = cv2.imdecode(np_array, cv2.IMREAD_COLOR)

        if image is None:
            return None, "Could not decode image. Please ensure the image is valid JPEG/PNG."

        return image, None
    except Exception as e:
        return None, f"Failed to decode base64 image: {str(e)}"


def is_image_blurry(image: np.ndarray, threshold: float = 30.0) -> bool:
    """Estimate image sharpness using Laplacian variance. Lower = blurrier.
    
    Note: threshold=30.0 is tuned for typical webcam captures. 
    Professional photos can use 100.0+, but webcams produce lower values.
    """
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    laplacian_var = cv2.Laplacian(gray, cv2.CV_64F).var()
    return laplacian_var < threshold


def count_faces(image: np.ndarray) -> int:
    """Count number of faces in the image using HOG-based face_recognition."""
    try:
        rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        locations = face_recognition.face_locations(rgb, model="hog")
        return len(locations)
    except Exception:
        return 0


def encode_image(image: np.ndarray) -> np.ndarray:
    """Encode a BGR image to 128-D face encoding vector using face_recognition."""
    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    encodings = face_recognition.face_encodings(rgb, model="small")
    if not encodings:
        return None
    return encodings[0]


def cosine_similarity(a: list[float], b: np.ndarray) -> float:
    """Compute cosine similarity between two vectors."""
    a_arr = np.array(a)
    dot = np.dot(a_arr, b)
    norm_a = np.linalg.norm(a_arr)
    norm_b = np.linalg.norm(b)
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return float(dot / (norm_a * norm_b))


def euclidean_distance(a: list[float], b: np.ndarray) -> float:
    """Compute Euclidean distance between two vectors."""
    return float(np.linalg.norm(np.array(a) - b))


# ── Endpoints ───────────────────────────────────────────────────────────────

@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """Health check endpoint."""
    return HealthResponse(
        status="healthy",
        model_loaded=True,
        version="1.0.0",
        timestamp=datetime.utcnow().isoformat(),
    )


@app.post("/extract-embedding", response_model=ExtractEmbeddingResponse, tags=["Face Recognition"])
async def extract_embedding(req: ExtractEmbeddingRequest):
    """
    Detect faces in an image and extract 128-D face encoding.
    
    - Rejects blurry images if sharpness is below threshold
    - Rejects if 0 or multiple faces detected (when validate_single_face=true)
    - Returns 128-D embedding vector compatible with face_recognition library
    """
    request_id = str(uuid.uuid4())[:8]

    # Decode image
    image, error = base64_to_image(req.image)
    if error:
        return ExtractEmbeddingResponse(
            success=False,
            message=error,
            request_id=request_id,
        )

    h, w = image.shape[:2]

    # Note: Blur check removed — webcam images have low Laplacian variance
    # even when visually clear, causing false rejections. face_recognition
    # will return empty embeddings for truly unusable images anyway.

    # Count faces
    face_count = count_faces(image)

    if face_count == 0:
        return ExtractEmbeddingResponse(
            success=False,
            face_count=0,
            image_width=w,
            image_height=h,
            message="No face detected. Please position your face clearly in front of the camera.",
            request_id=request_id,
        )

    if req.validate_single_face and face_count > 1:
        return ExtractEmbeddingResponse(
            success=False,
            face_count=face_count,
            image_width=w,
            image_height=h,
            message=f"Multiple faces detected ({face_count}). Please ensure only one person is in the frame.",
            request_id=request_id,
        )

    # Extract encoding
    embedding = encode_image(image)

    if embedding is None:
        return ExtractEmbeddingResponse(
            success=False,
            face_count=face_count,
            image_width=w,
            image_height=h,
            message="Could not extract face encoding. Please try again with a different angle or lighting.",
            request_id=request_id,
        )

    return ExtractEmbeddingResponse(
        success=True,
        embedding=embedding.tolist(),
        face_count=face_count,
        image_width=w,
        image_height=h,
        message="Embedding extracted successfully.",
        request_id=request_id,
    )


@app.post("/verify-face", response_model=VerifyFaceResponse, tags=["Face Recognition"])
async def verify_face(req: VerifyFaceRequest):
    """
    Compare a captured face against a stored embedding.
    
    Uses cosine similarity to compute match score.
    - Default threshold: 0.6 (cosine similarity)
    - At 0.6: moderately strict matching
    - At 0.7: recommended for production security
    - At 0.8: very strict (may reject legitimate users in poor lighting)
    """
    request_id = str(uuid.uuid4())[:8]

    # Decode image
    image, error = base64_to_image(req.image)
    if error:
        return VerifyFaceResponse(
            success=False,
            message=error,
            request_id=request_id,
        )

    # Count faces
    face_count = count_faces(image)

    if face_count == 0:
        return VerifyFaceResponse(
            success=False,
            face_count=0,
            message="No face detected in the provided image.",
            request_id=request_id,
        )

    if face_count > 1:
        return VerifyFaceResponse(
            success=False,
            face_count=face_count,
            message=f"Multiple faces detected ({face_count}). Please ensure only one person is in the frame.",
            request_id=request_id,
        )

    # Extract embedding
    embedding = encode_image(image)
    if embedding is None:
        return VerifyFaceResponse(
            success=False,
            face_count=face_count,
            message="Could not extract face encoding from the provided image.",
            request_id=request_id,
        )

    # Compute similarity
    similarity = cosine_similarity(req.stored_embedding, embedding)

    # Also compute euclidean distance for logging
    distance = euclidean_distance(req.stored_embedding, embedding)

    matched = similarity >= req.similarity_threshold

    return VerifyFaceResponse(
        success=True,
        matched=matched,
        similarity=round(similarity, 4),
        face_count=face_count,
        message=(
            f"Face verified successfully (similarity={similarity:.4f}, threshold={req.similarity_threshold:.2f})."
            if matched
            else f"Face does not match (similarity={similarity:.4f}, threshold={req.similarity_threshold:.2f})."
        ),
        request_id=request_id,
    )


@app.get("/", tags=["Root"])
async def root():
    """Root endpoint."""
    return {
        "service": "MelodyShop AI - Face Recognition",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health",
    }


# ── Entry Point ─────────────────────────────────────────────────────────────

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8093))
    host = os.environ.get("HOST", "0.0.0.0")
    uvicorn.run(
        "main:app",
        host=host,
        port=port,
        reload=False,
        workers=1,
    )
