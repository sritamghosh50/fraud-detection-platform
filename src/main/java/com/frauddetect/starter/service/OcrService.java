package com.frauddetect.starter.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Extracts text from an uploaded image (screenshot of SMS, WhatsApp,
 * email, etc.) using Tesseract OCR. The extracted text is then handed
 * off to the SAME MessageAnalysisService used for pasted text - no
 * duplicate fraud-detection logic needed.
 */
@Service
public class OcrService {

    // IMPORTANT: update this path if your Tesseract installed somewhere else
    private static final String TESSDATA_PATH = "C:\\Program Files\\Tesseract-OCR\\tessdata";

    public String extractText(MultipartFile imageFile) throws IOException, TesseractException {
        // Save the uploaded image to a temporary file, since Tesseract needs a real file path
        File tempFile = File.createTempFile("upload_", "_" + imageFile.getOriginalFilename());
        try {
            Files.write(tempFile.toPath(), imageFile.getBytes());

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(TESSDATA_PATH);
            tesseract.setLanguage("eng");

            return tesseract.doOCR(tempFile);
        } finally {
            tempFile.delete(); // clean up - don't leave temp files lying around
        }
    }
}