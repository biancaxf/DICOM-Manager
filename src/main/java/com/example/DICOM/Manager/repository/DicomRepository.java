package com.example.DICOM.Manager.repository;

import com.example.DICOM.Manager.model.DicomObj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DicomRepository extends JpaRepository<DicomObj, Long> {

    Optional<DicomObj> findBySopInstanceUid(String sopInstanceUid);

    //boolean existsBySopInstanceUid(String sopInstanceUid);

    @Query(value = "select count(1) from dicom_objs where sop_instance_uid = :uid", nativeQuery = true)
    int countBySopInstanceUidNative(@Param("uid") String uid);

    List<DicomObj> findByStudyUid(String studyUid);

    List<DicomObj> findBySeriesUid(String seriesUid);

    List<DicomObj> findByDescriereContainingIgnoreCase(String descriere);

    List<DicomObj> findByStudyUidAndSeriesUid(String studyUid, String seriesUid);

    @Modifying
    @Transactional
    @Query("UPDATE DicomObj d SET d.modality = :modality, d.anatomicRegion = :anatomicRegion, d.kvp = :kvp WHERE d.id = :id")
    void updateMetadata(@Param("id") Long id,
                        @Param("modality") String modality,
                        @Param("anatomicRegion") String anatomicRegion,
                        @Param("kvp") String kvp);

    @Query(value = """
      SELECT * FROM (
        SELECT t.*, ROW_NUMBER() OVER (ORDER BY t.created_at DESC) rn
        FROM dicom_objs t
      )
      WHERE rn BETWEEN :offset + 1 AND :offset + :size
      """,
            nativeQuery = true)
    List<DicomObj> findPage(@Param("offset") int offset, @Param("size") int size);

    @Query(value = "SELECT COUNT(*) FROM dicom_objs", nativeQuery = true)
    long countAll();
}
