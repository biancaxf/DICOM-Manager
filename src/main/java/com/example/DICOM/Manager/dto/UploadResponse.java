package com.example.DICOM.Manager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class UploadResponse implements java.io.Serializable {

    private long id;
    private String filename;
    private long size;

    private String descriere;
    private String patientName;
    private String patientId;
    private String modality;
    private String anatomicRegion;
    private BigDecimal kvp;
    private String studyDescription;
    private String seriesDescription;

    private String sopClassUID;
    private String sopInstanceUID;
    private String studyUID;
    private String seriesUID;

    private Long contentLength;
    private LocalDateTime createdAt;

    private LocalDate patientBirthdate;
    private String referringPhysician;
}
