package com.melodyshop.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    /** Full public URL to use in <img src="..."> */
    private String url;

    /** Cloudinary public ID — needed for deletion */
    private String publicId;

    /** Format: jpg, png, webp */
    private String format;

    /** File size in bytes */
    private long bytes;

    /** Image width in pixels */
    private int width;

    /** Image height in pixels */
    private int height;

    /** The folder used: product / avatar / review */
    private String folder;
}
