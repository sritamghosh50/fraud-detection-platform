package com.frauddetect.starter.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class OcrService {

    private static final int MAX_IMAGE_SIZE = 1600;

    private static final String TESSDATA_PATH =
            "/usr/share/tesseract-ocr/5/tessdata";

    public String extractText(MultipartFile imageFile) {

        if (imageFile == null || imageFile.isEmpty()) {
            return "";
        }

        File tempInput = null;
        File processedImage = null;

        try {

            BufferedImage original =
                    ImageIO.read(imageFile.getInputStream());

            if (original == null) {
                return "";
            }

            BufferedImage resized =
                    resizeImage(original, MAX_IMAGE_SIZE);

            BufferedImage prepared =
                    prepareImage(resized);

            tempInput = File.createTempFile(
                    "fraudguard-upload-",
                    ".png"
            );

            processedImage = File.createTempFile(
                    "fraudguard-ocr-",
                    ".png"
            );

            ImageIO.write(
                    resized,
                    "png",
                    tempInput
            );

            ImageIO.write(
                    prepared,
                    "png",
                    processedImage
            );

            ITesseract tesseract = new Tesseract();

            tesseract.setDatapath(TESSDATA_PATH);
            tesseract.setLanguage("eng");

            /*
             * PSM 6:
             * Assume one uniform block of text.
             * This works well for email/message screenshots.
             */
            tesseract.setPageSegMode(6);

            /*
             * Use the faster default OCR engine.
             */
            tesseract.setOcrEngineMode(1);

            /*
             * Tell Tesseract that the image has normal screen text.
             */
            tesseract.setVariable(
                    "user_defined_dpi",
                    "200"
            );

            String text =
                    tesseract.doOCR(processedImage);

            if (text == null) {
                return "";
            }

            return cleanText(text);

        } catch (IOException e) {

            System.out.println(
                    "Image processing failed: "
                            + e.getMessage()
            );

            return "";

        } catch (TesseractException e) {

            System.out.println(
                    "OCR failed: "
                            + e.getMessage()
            );

            return "";

        } finally {

            deleteFile(tempInput);
            deleteFile(processedImage);
        }
    }

    private BufferedImage resizeImage(
            BufferedImage original,
            int maxSize
    ) {

        int width = original.getWidth();
        int height = original.getHeight();

        int largest =
                Math.max(width, height);

        if (largest <= maxSize) {
            return original;
        }

        double scale =
                (double) maxSize / largest;

        int newWidth =
                Math.max(1, (int) Math.round(width * scale));

        int newHeight =
                Math.max(1, (int) Math.round(height * scale));

        BufferedImage resized =
                new BufferedImage(
                        newWidth,
                        newHeight,
                        BufferedImage.TYPE_INT_RGB
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

        return resized;
    }

    private BufferedImage prepareImage(
            BufferedImage source
    ) {

        int width = source.getWidth();
        int height = source.getHeight();

        BufferedImage gray =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_BYTE_GRAY
                );

        Graphics2D graphics =
                gray.createGraphics();

        graphics.drawImage(
                source,
                0,
                0,
                null
        );

        graphics.dispose();

        return gray;
    }

    private String cleanText(String text) {

        return text
                .replace("\u0000", " ")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    private void deleteFile(File file) {

        if (file != null && file.exists()) {
            try {
                file.delete();
            } catch (Exception ignored) {
            }
        }
    }
}