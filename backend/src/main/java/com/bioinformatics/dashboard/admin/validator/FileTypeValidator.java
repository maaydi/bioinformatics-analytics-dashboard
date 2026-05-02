package com.bioinformatics.dashboard.admin.validator;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import com.bioinformatics.dashboard.admin.exception.UnsupportedFileTypeException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FileTypeValidator implements ConstraintValidator<ValidFileType, MultipartFile> {

    @Value("${app.upload.max-size}")
    private long maxSize;

    @Value("$app.upload.extensions")
    private List<String> extensions;

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        var fname = file.getOriginalFilename();
        if (fname != null) {
            var ext = fname.substring(fname.lastIndexOf(".") + 1).toLowerCase();
            if (!extensions.contains(ext)) {
                throw new UnsupportedFileTypeException(String.format("Extension %s is not supported.", ext));
            }
        }
        return true;
    }

}
