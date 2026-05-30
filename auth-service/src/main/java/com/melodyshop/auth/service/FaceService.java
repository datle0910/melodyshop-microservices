package com.melodyshop.auth.service;

import com.melodyshop.auth.dto.*;

public interface FaceService {

    /**
     * Register a face for an authenticated user.
     * Extracts embedding from the image and stores it in the database.
     */
    FaceRegisterResponse registerFace(String userId, FaceRegisterRequest request);

    /**
     * Login a user using face recognition.
     * Verifies the captured face against the stored embedding.
     */
    FaceLoginResponse loginWithFace(FaceLoginRequest request);

    /**
     * Check if a user has registered their face.
     */
    FaceStatusResponse getFaceStatus(String userId);

    /**
     * Delete a user's face registration.
     */
    void deleteFaceRegistration(String userId);
}
