package com.example.DICOM.Manager.service;

public interface PdfService {
    byte[] generateReportPdf(Long dicomId);
}
