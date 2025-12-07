package com.example.DICOM.Manager.controller;

import com.example.DICOM.Manager.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PdfController {
    private final PdfService pdfService;

    @GetMapping(value = "/dicom/{id}/report.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long id) {
        byte[] pdf = pdfService.generateReportPdf(id);
        String filename = "dicom-" + id + ".pdf";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
