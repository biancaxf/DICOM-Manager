package com.example.DICOM.Manager;

import com.example.DICOM.Manager.repository.DicomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DicomTestRunner implements CommandLineRunner {

    private final DicomRepository dicomObjRepository;

    public DicomTestRunner(DicomRepository dicomObjRepository) {
        this.dicomObjRepository = dicomObjRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== Test conexiune DICOM + Oracle ===");

        long count = dicomObjRepository.count();
        System.out.println("Total înregistrări în DICOM_OBJS: " + count);

        // Încearcă o căutare simplă (schimbă UID cu unul existent în baza ta)
        var studyUid = "1.2.3.study";
        var results = dicomObjRepository.findByStudyUid(studyUid);

        System.out.println("Rezultate pentru studyUid=" + studyUid + ": " + results.size());
        results.forEach(obj ->
                System.out.println(" -> ID: " + obj.getId() + ", descriere: " + obj.getDescriere())
        );

        System.out.println("=== Test terminat ===");
    }
}
