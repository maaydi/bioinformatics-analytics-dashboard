package com.bioinformatics.dashboard.admin.validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Component
public class FileSizeValidator implements ConstraintValidator<ValidFileSize, MultipartFile> {

    @Value("${app.import.max-size}")
    private long maxSize;

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        if (file.getSize() > maxSize) {
            // TODO add handler with correct status from front
            throw new MaxUploadSizeExceededException(maxSize);
        }
        return true;
    }

}
