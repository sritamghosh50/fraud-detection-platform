package com.frauddetect.starter.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class OcrService {

    private static final String TESSDATA_PATH =
            "/usr/share/tesseract-ocr/5/tessdata";

    private static final int MAX_IMAGE_SIZE = 2000;

    public String extractText(
            MultipartFile imageFile)
            throws IOException, TesseractException {

        if (imageFile == null
                || imageFile.isEmpty()) {

            throw new IllegalArgumentException(
                    "Image file is empty."
            );
        }

        File originalFile =
                File.createTempFile(
                        "fraudguard-original-",
                        ".img"
                );

        File processedFile =
                File.createTempFile(
                        "fraudguard-ocr-",
                        ".png"
                );

        try {

            imageFile.transferTo(
                    originalFile
            );

            BufferedImage original =
                    ImageIO.read(
                            originalFile
                    );

            if (original == null) {

                throw new IOException(
                        "Unable to read the uploaded image."
                );
            }

            BufferedImage processed =
                    prepareImage(
                            original
                    );

            ImageIO.write(
                    processed,
                    "png",
                    processedFile
            );

            Tesseract tesseract =
                    new Tesseract();

            tesseract.setDatapath(
                    TESSDATA_PATH
            );

            tesseract.setLanguage(
                    "eng"
            );

            /*
             * Single uniform block of text.
             * Faster for screenshots/messages.
             */
            tesseract.setPageSegMode(
                    6
            );

            /*
             * Avoid Tesseract trying to guess
             * a strange DPI from screenshots.
             */
            tesseract.setVariable(
                    "user_defined_dpi",
                    "200"
            );

            return tesseract.doOCR(
                    processedFile
            );

        } finally {

            if (originalFile.exists()) {

                originalFile.delete();
            }

            if (processedFile.exists()) {

                processedFile.delete();
            }
        }
    }

    private BufferedImage prepareImage(
            BufferedImage original) {

        int width =
                original.getWidth();

        int height =
                original.getHeight();

        double scale =
                Math.min(
                        1.0,
                        Math.min(
                                (double) MAX_IMAGE_SIZE / width,
                                (double) MAX_IMAGE_SIZE / height
                        )
                );

        int newWidth =
                Math.max(
                        1,
                        (int) Math.round(
                                width * scale
                        )
                );

        int newHeight =
                Math.max(
                        1,
                        (int) Math.round(
                                height * scale
                        )
                );

        BufferedImage resized =
                new BufferedImage(
                        newWidth,
                        newHeight,
                        BufferedImage.TYPE_BYTE_GRAY
                );

        Graphics2D graphics =
                resized.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF
        );

        graphics.drawImage(
                original,
                0,
                0,
                newWidth,
                newHeight,
                null
        );

        graphics.dispose();

        /*
         * Slight contrast improvement.
         */
        BufferedImage contrast =
                new BufferedImage(
                        newWidth,
                        newHeight,
                        BufferedImage.TYPE_BYTE_GRAY
                );

        for (int y = 0;
             y < newHeight;
             y++) {

            for (int x = 0;
                 x < newWidth;
                 x++) {

                int rgb =
                        resized.getRGB(
                                x,
                                y
                        );

                int gray =
                        new Color(
                                rgb
                        ).getRed();

                /*
                 * Simple contrast stretch.
                 */
                int adjusted =
                        (gray - 128) * 2 + 128;

                adjusted =
                        Math.max(
                                0,
                                Math.min(
                                        255,
                                        adjusted
                                )
                        );

                int value =
                        (adjusted << 16)
                                | (adjusted << 8)
                                | adjusted;

                contrast.setRGB(
                        x,
                        y,
                        value
                );
            }
        }

        return contrast;
    }
}