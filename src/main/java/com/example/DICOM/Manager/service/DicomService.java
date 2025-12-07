package com.example.DICOM.Manager.service;

import com.example.DICOM.Manager.dto.UploadResponse;
import com.example.DICOM.Manager.model.DicomObj;
import com.example.DICOM.Manager.repository.DicomRepository;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DicomService {

    private final DicomRepository repo;
    private final DicomPlsqlService plsql;

    public DicomService(DicomRepository repo, DicomPlsqlService plsql) {
        this.repo = repo;
        this.plsql = plsql;
    }

    @Transactional
    public UploadResponse uploadDicom(MultipartFile file, String descriere) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fișierul este gol.");
        }
        try {

            byte[] bytes = file.getBytes();


            Long id = plsql.initObject(descriere);
            plsql.importFromBlob(id, file.getOriginalFilename(), bytes);
            plsql.makePreview(id, "PNG");

            Attributes attrs = readAttributes(bytes);
            ExtraMeta extra   = extractExtraMeta(attrs);
            LocalDateTime when = dicomDateTime(attrs);

            String patientName = attrs.getString(Tag.PatientName);                    // (0010,0010)
            String physician   = attrs.getString(Tag.ReferringPhysicianName);         // (0008,0090)
            Date birthRaw      = attrs.getDate(Tag.PatientBirthDate);                 // (0010,0030)
            LocalDate birth    = (birthRaw != null)
                    ? birthRaw.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    : null;



            DicomObj obj = repo.findById(id).orElseThrow();
            obj.setContentLength((long) bytes.length);
            obj.setModality(extra.modality());
            obj.setAnatomicRegion(extra.anatomicRegion());


            java.math.BigDecimal kvpNum = null;
            if (extra.kvp() != null && !extra.kvp().isBlank()) {
                try { kvpNum = new java.math.BigDecimal(extra.kvp().trim()); } catch (NumberFormatException ignore) {}
            }
            obj.setKvp(kvpNum);

            obj.setCreatedAt(when);

            obj.setPatientName(patientName);
            obj.setReferringPhysician(physician);
            obj.setPatientBirthdate(birth);

            repo.save(obj);

            // 4) răspuns
            return mapToUploadResponse(obj, file.getOriginalFilename());
        } catch (Exception ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Eroare la importul DICOM în Oracle", ex);
        }
    }


    // ===== Helpers =====

    private record ExtraMeta(String modality, String anatomicRegion, String kvp) {}

    private Attributes readAttributes(byte[] bytes) {
        try (var bais = new java.io.ByteArrayInputStream(bytes);
             var dis = new DicomInputStream(bais)) {
            dis.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
            return dis.readDataset(-1, -1);
        } catch (Exception e) {
            throw new RuntimeException("Eroare la parsarea fișierului DICOM: " + e.getMessage(), e);
        }
    }

    private ExtraMeta extractExtraMeta(Attributes attr) {
        String modality = trimToNull(attr.getString(Tag.Modality));     // (0008,0060)
        String kvp      = trimToNull(attr.getString(Tag.KVP));          // (0018,0060)

        // (0008,2218) Anatomic Region Sequence → preferăm Code Meaning (0008,0104)
        String anatomic = null;
        Sequence seq = attr.getSequence(Tag.AnatomicRegionSequence);
        if (seq != null && !seq.isEmpty()) {
            Attributes item = seq.get(0);
            anatomic = firstNonBlank(
                    item.getString(Tag.CodeMeaning),  // (0008,0104)
                    item.getString(Tag.CodeValue)     // (0008,0100) fallback
            );
        }
        return new ExtraMeta(modality, trimToNull(anatomic), kvp);
    }

    private static String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v.trim();
        return null;
    }

    private Attributes readAttributes(MultipartFile file) {
        try (InputStream is = file.getInputStream(); DicomInputStream dis = new DicomInputStream(is)) {
            dis.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
            return dis.readDataset(-1, -1);
        } catch (Exception e) {
            throw new RuntimeException("Eroare la parsarea fișierului DICOM: " + e.getMessage(), e);
        }
    }

    private LocalDateTime dicomDateTime(Attributes attrs) {
        Date dt = attrs.getDate(Tag.AcquisitionDateTime);
        if (dt == null) {
            Date d = attrs.getDate(Tag.StudyDate);
            Date t = attrs.getDate(Tag.StudyTime);
            if (d != null && t != null) {
                Calendar cd = Calendar.getInstance();
                cd.setTime(d);
                Calendar ct = Calendar.getInstance();
                ct.setTime(t);
                cd.set(Calendar.HOUR_OF_DAY, ct.get(Calendar.HOUR_OF_DAY));
                cd.set(Calendar.MINUTE, ct.get(Calendar.MINUTE));
                cd.set(Calendar.SECOND, ct.get(Calendar.SECOND));
                cd.set(Calendar.MILLISECOND, 0);
                dt = cd.getTime();
            } else if (d != null) {
                dt = d; // doar data (00:00)
            }
        }
        if (dt == null) return null;
        return LocalDateTime.ofInstant(dt.toInstant(), ZoneId.systemDefault());
    }

    public byte[] getPreviewPng(Long id) {
        byte[] png = plsql.fetchPreviewBytes(id);
        if (png != null && png.length > 0) return png;


        byte[] dcm = plsql.fetchDicomBytes(id);
        if (dcm == null || dcm.length == 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "DICOM lipsă");
        }

        byte[] rendered = renderPngWithDcm4che(dcm);

        try {
            plsql.savePreviewBytes(id, rendered);
        } catch (Exception ignore) {

        }
        return rendered;
    }


    private byte[] renderPngWithDcm4che(byte[] dicomBytes) {
        javax.imageio.spi.IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(dicomBytes);
             javax.imageio.stream.ImageInputStream iis =
                     new javax.imageio.stream.MemoryCacheImageInputStream(bais)) {

            org.dcm4che3.imageio.plugins.dcm.DicomImageReader reader =
                    new org.dcm4che3.imageio.plugins.dcm.DicomImageReader(
                            new org.dcm4che3.imageio.plugins.dcm.DicomImageReaderSpi());

            reader.setInput(iis, false, false);
            java.awt.image.BufferedImage img = reader.read(0);

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "PNG", baos);
            reader.dispose();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Eroare la randarea PNG din DICOM (fallback)", e);
        }
    }

    @Transactional(readOnly = true)
    public UploadResponse findById(long id) {
        DicomObj obj = repo.findById(id).orElseThrow();
        return mapToUploadResponse(obj, null);
    }

    @Transactional(readOnly = true)
    public Page<UploadResponse> list(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int size   = pageable.getPageSize();

        List<UploadResponse> content = repo.findPage(offset, size)
                .stream().map(o -> mapToUploadResponse(o, null)).toList();

        long total = repo.countAll();
        return new PageImpl<>(content, pageable, total);
    }

    private UploadResponse mapToUploadResponse(DicomObj obj, String originalFilenameIfKnown) {
        UploadResponse r = new UploadResponse();
        r.setId(obj.getId());
        r.setFilename(originalFilenameIfKnown);
        if (obj.getContentLength() != null) r.setContentLength(obj.getContentLength());
        r.setDescriere(obj.getDescriere());
        r.setModality(obj.getModality());
        r.setAnatomicRegion(obj.getAnatomicRegion());
        if (obj.getKvp() != null) r.setKvp(obj.getKvp());
        r.setSopClassUID(obj.getSopClassUid());
        r.setSopInstanceUID(obj.getSopInstanceUid());
        r.setStudyUID(obj.getStudyUid());
        r.setSeriesUID(obj.getSeriesUid());
        r.setCreatedAt(obj.getCreatedAt());
        r.setPatientName(obj.getPatientName());
        r.setPatientBirthdate(obj.getPatientBirthdate());
        r.setReferringPhysician(obj.getReferringPhysician());
        return r;
    }

    @Transactional
    public void delete(long id) {
        try {
            plsql.deleteObject(id);
        } catch (Exception e) {

        }

        try {
            if (repo.existsById(id)) {
                repo.deleteById(id);
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException ignore) {
        }
    }

}
