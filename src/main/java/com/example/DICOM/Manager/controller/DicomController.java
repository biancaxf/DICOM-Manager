package com.example.DICOM.Manager.controller;

import com.example.DICOM.Manager.dto.UploadResponse;
import com.example.DICOM.Manager.service.DicomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dicom")
public class DicomController {
    private final DicomService dicomService;

    public DicomController(DicomService dicomService) {
        this.dicomService = dicomService;
    }

    @ModelAttribute("page")
    public Page<UploadResponse> populatePage(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return dicomService.list(pageable);
    }

    @PostMapping(
            path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public org.springframework.http.ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "descriere", required = false) String descriere) {

        UploadResponse response = dicomService.uploadDicom(file, descriere);
        return org.springframework.http.ResponseEntity.ok(response);
    }

    @PostMapping(path = "/ui/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadUi(@RequestParam("file") MultipartFile file,
                           @RequestParam(value = "descriere", required = false) String descriere,
                           org.springframework.ui.Model model,
                           @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UploadResponse resp = dicomService.uploadDicom(file, descriere);
        model.addAttribute("previewUrl", "/dicom/" + resp.getId() + "/preview");
        model.addAttribute("obj", resp);
        return "home";
    }

    @GetMapping(path = "/{id}/preview", produces = org.springframework.http.MediaType.IMAGE_PNG_VALUE)
    public org.springframework.http.ResponseEntity<byte[]> preview(@PathVariable("id") Long id) {
        try {
            byte[] png = dicomService.getPreviewPng(id);
            return org.springframework.http.ResponseEntity
                    .ok()
                    .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                    .body(png);
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 404) {
                return org.springframework.http.ResponseEntity.notFound().build();
            }
            throw ex;
        }
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) Long id,
                       @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        if (id != null) {
            UploadResponse obj = dicomService.findById(id);
            model.addAttribute("obj", obj);
            model.addAttribute("previewUrl", "/dicom/" + id + "/preview");
        }
        return "home";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         RedirectAttributes ra) {
        try {
            dicomService.delete(id);
            ra.addFlashAttribute("msg", "Fisierul a fost șters.");
        } catch (Exception e) {
            ra.addFlashAttribute("err", "Stergerea a intampinat o problema.");
        }
        return "redirect:/dicom/?page=" + Math.max(page,0) + "&size=" + Math.max(size,1);
    }

}

