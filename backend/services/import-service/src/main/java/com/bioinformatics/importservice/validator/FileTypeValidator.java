package com.bioinformatics.importservice.validator;

import com.bioinformatics.common.exception.UnsupportedFileTypeException;
import com.bioinformatics.importservice.config.ApplicationProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploaded files to ensure their extension matches the allowed list.
 */
@Component
@RequiredArgsConstructor
public class FileTypeValidator implements ConstraintValidator<ValidFileType, MultipartFile> {

    private final ApplicationProperties appProperties;

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        var fName = file.getOriginalFilename();
        if (fName != null) {
            var ext = fName.substring(fName.lastIndexOf(".") + 1).toLowerCase();
            var extensions = appProperties.importConfig().extensions();
            if (!extensions.contains(ext)) {
                throw new UnsupportedFileTypeException(
                        String.format("Extension %s is not supported.", ext));
            }
        }
        return true;
    }

}
