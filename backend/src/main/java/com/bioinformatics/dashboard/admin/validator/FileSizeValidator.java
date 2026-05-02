package com.bioinformatics.dashboard.admin.validator;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FileSizeValidator implements ConstraintValidator<ValidFileType, MultipartFile> {

    @Value("${app.upload.max-size}")
    private long maxSize;

    @Value("$app.upload.extensions")
    private List<String> extensions;

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        if (file.getSize() > maxSize) {
            throw new MaxUploadSizeExceededException(maxSize);
        }
        return true;
    }

}
