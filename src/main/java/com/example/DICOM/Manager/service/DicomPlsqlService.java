package com.example.DICOM.Manager.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DicomPlsqlService {

    private final JdbcTemplate jdbc;

    public DicomPlsqlService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long initObject(String descriere) {
        return jdbc.execute(
                (org.springframework.jdbc.core.CallableStatementCreator) con -> {
                    java.sql.CallableStatement cs =
                            con.prepareCall("{ call dicom_pkg.init_object(?, ?) }");
                    cs.registerOutParameter(1, java.sql.Types.NUMERIC);
                    cs.setString(2, descriere);
                    return cs;
                },
                (org.springframework.jdbc.core.CallableStatementCallback<Long>) cs -> {
                    cs.execute();
                    return cs.getLong(1);
                }
        );
    }

    public void importFromBlob(long id, String filename, byte[] data) {
        jdbc.execute(
                (org.springframework.jdbc.core.CallableStatementCreator) con -> {
                    java.sql.CallableStatement cs =
                            con.prepareCall("{ call dicom_pkg.import_from_blob(?, ?, ?) }");
                    cs.setLong(1, id);
                    cs.setString(2, filename);
                    cs.setBytes(3, data);
                    return cs;
                },
                (org.springframework.jdbc.core.CallableStatementCallback<Void>) cs -> {
                    cs.execute();
                    return null;
                }
        );
    }

    public void importFromBlob(long id, String filename, MultipartFile file) {
        jdbc.execute((java.sql.Connection conn) -> {
            try {
                importFromBlob(id, filename, file.getBytes());
            } catch (java.io.IOException e) {
                throw new RuntimeException("Eroare la citirea fișierului", e);
            }
            return null;
        });
    }

    public void makePreview(Long id, String format) {
        jdbc.execute(
                (org.springframework.jdbc.core.CallableStatementCreator) con -> {
                    java.sql.CallableStatement cs =
                            con.prepareCall("{ call dicom_pkg.make_preview(?, ?) }");
                    cs.setLong(1, id);
                    cs.setString(2, format);
                    return cs;
                },
                (org.springframework.jdbc.core.CallableStatementCallback<Void>) cs -> {
                    cs.execute();
                    return null;
                }
        );
    }

    public byte[] readPreview(Long id) {
        return fetchPreviewBytes(id);
    }

    public byte[] fetchPreviewBytes(Long id) {
        return jdbc.query(
                "select t.dicom_preview.getContent() as content from dicom_objs t where t.id = ?",
                ps -> ps.setLong(1, id),
                rs -> {
                    if (!rs.next()) return null;
                    java.sql.Blob blob = rs.getBlob("content");
                    if (blob == null) return null;
                    long len = blob.length();
                    if (len <= 0L) return null;
                    return blob.getBytes(1, (int) len);
                }
        );
    }

    public byte[] fetchDicomBytes(Long id) {
        return jdbc.query(
                "select t.dicom_obj.getContent() as content from dicom_objs t where t.id = ?",
                ps -> ps.setLong(1, id),
                rs -> {
                    if (!rs.next()) return null;
                    java.sql.Blob blob = rs.getBlob("content");
                    if (blob == null) return null;
                    long len = blob.length();
                    if (len <= 0L) return null;
                    return blob.getBytes(1, (int) len);
                }
        );
    }

    public void savePreviewBytes(Long id, byte[] pngBytes) {
        jdbc.execute(
                (org.springframework.jdbc.core.CallableStatementCreator) con -> {
                    java.sql.CallableStatement cs =
                            con.prepareCall("{ call dicom_pkg.save_preview(?, ?) }");
                    cs.setLong(1, id);
                    cs.setBytes(2, pngBytes);
                    return cs;
                },
                (org.springframework.jdbc.core.CallableStatementCallback<Void>) cs -> {
                    cs.execute();
                    return null;
                }
        );
    }

    public void deleteObject(Long id) {
        jdbc.update("BEGIN DICOM_PKG.DELETE_OBJECT(?); END;", id);
    }

}
