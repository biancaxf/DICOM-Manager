package com.example.DICOM.Manager.service;

import com.example.DICOM.Manager.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService{
    private final DicomService dicomService;
    private final TemplateEngine templateEngine;

    @Override
    public byte[] generateReportPdf(Long dicomId) {

        UploadResponse r = dicomService.findById(dicomId);
        byte[] png = dicomService.getPreviewPng(dicomId);


        String dataUri = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(png);


        Map<String, Object> model = new HashMap<>();
        model.put("title", "DICOM Report");
        model.put("generatedAt", java.time.OffsetDateTime.now());
        model.put("previewDataUri", dataUri);
        model.put("meta", r); // folosim direct UploadResponse în template


        org.thymeleaf.context.Context ctx = new org.thymeleaf.context.Context(java.util.Locale.getDefault());
        ctx.setVariables(model);
        String html = templateEngine.process("report", ctx); // templates/report.html


        try (var out = new java.io.ByteArrayOutputStream()) {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder b = new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            b.withHtmlContent(html, null);
            b.toStream(out);
            b.useFastMode();
            b.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Eroare generare PDF", e);
        }
    }
}
