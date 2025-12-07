package com.example.DICOM.Manager.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "DICOM_OBJS")
@SequenceGenerator(
        name = "dicom_objs_seq",
        sequenceName = "SEQ_DICOM_OBJS",
        allocationSize = 1
)
public class DicomObj {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dicom_objs_seq")
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "DESCRIERE", length = 200)
    private String descriere;

    // NOTĂ: câmpurile ORDSYS.* NU sunt mapate în JPA

    @Column(name = "SOP_INSTANCE_UID", length = 64)
    private String sopInstanceUid;

    @Column(name = "SOP_CLASS_UID", length = 64)
    private String sopClassUid;

    @Column(name = "STUDY_UID", length = 64)
    private String studyUid;

    @Column(name = "SERIES_UID", length = 64)
    private String seriesUid;

    @Column(name = "CONTENT_LENGTH")
    private Long contentLength;

    // Oracle DATE -> LocalDateTime
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "MODALITY", length = 20)
    private String modality;

    @Column(name = "ANATOMIC_REGION", length = 50)
    private String anatomicRegion;

    @Column(name = "KVP", length = 10)
    private BigDecimal kvp;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "patient_birthdate")
    private LocalDate patientBirthdate;

    @Column(name = "referring_physician")
    private String referringPhysician;
}
