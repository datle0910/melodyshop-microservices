package com.melodyshop.media.service;

import com.melodyshop.media.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    /**
     * Upload an image to Cloudinary. The folder is determined by the type.
     *
     * @param file the multipart image file
     * @param type one of: "product", "avatar", "review"
     * @return UploadResponse with the public URL and metadata
     */
    UploadResponse upload(MultipartFile file, String type);

    /**
     * Delete an image from Cloudinary by its public ID.
     *
     * @param publicId the Cloudinary public ID returned from upload
     */
    void delete(String publicId);
}
