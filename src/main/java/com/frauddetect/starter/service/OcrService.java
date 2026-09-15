package com.frauddetect.starter.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class OcrService {

    // Linux Docker path where Debian installs Tesseract language data.
    private static final String TESSDATA_PATH =
            "/usr/share/tesseract-ocr/5/tessdata";

    public String extractText(MultipartFile imageFile)
            throws IOException, TesseractException {

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException(
                    "Image file is empty."
            );
        }

        /*
         * Keep the actual image extension.
         *
         * Previously every file was saved as .png.
         * That caused JPG/JPEG images to fail because
         * the file extension did not match the real image format.
         */
        String originalFilename =
                imageFile.getOriginalFilename();

        String extension = getImageExtension(
                originalFilename,
                imageFile.getContentType()
        );

        File tempFile =
                File.createTempFile(
                        "fraudguard-",
                        extension
                );

        try {

            imageFile.transferTo(tempFile);

            Tesseract tesseract =
                    new Tesseract();

            tesseract.setDatapath(
                    TESSDATA_PATH
            );

            tesseract.setLanguage("eng");

            /*
             * Tess4J/Tesseract will now receive the
             * image with its correct extension.
             *
             * PNG  -> .png
             * JPG  -> .jpg
             * JPEG -> .jpeg
             * GIF  -> .gif
             * BMP  -> .bmp
             */
            return tesseract.doOCR(tempFile);

        } finally {

            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Determines the correct file extension from
     * the uploaded image.
     */
    private String getImageExtension(
            String originalFilename,
            String contentType) {

        /*
         * First try the original filename.
         */
        if (originalFilename != null
                && originalFilename.contains(".")) {

            String extension =
                    originalFilename.substring(
                            originalFilename.lastIndexOf(".")
                    ).toLowerCase();

            if (isSupportedExtension(extension)) {
                return extension;
            }
        }

        /*
         * If filename does not contain an extension,
         * use the MIME/content type.
         */
        if (contentType != null) {

            switch (contentType.toLowerCase()) {

                case "image/jpeg":
                    return ".jpg";

                case "image/png":
                    return ".png";

                case "image/gif":
                    return ".gif";

                case "image/bmp":
                    return ".bmp";

                case "image/tiff":
                    return ".tiff";

                case "image/webp":
                    return ".webp";

                default:
                    break;
            }
        }

        /*
         * PNG is used only as a fallback when the
         * uploaded file provides no usable format information.
         */
        return ".png";
    }

    /**
     * Checks common image formats.
     */
    private boolean isSupportedExtension(
            String extension) {

        return extension.equals(".jpg")
                || extension.equals(".jpeg")
                || extension.equals(".png")
                || extension.equals(".gif")
                || extension.equals(".bmp")
                || extension.equals(".tif")
                || extension.equals(".tiff")
                || extension.equals(".webp");
    }
}