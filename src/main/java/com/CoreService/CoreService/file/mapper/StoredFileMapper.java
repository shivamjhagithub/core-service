package com.CoreService.CoreService.file.mapper;

import com.CoreService.CoreService.file.dto.StoredFileRef;
import com.CoreService.CoreService.file.entity.StoredFile;
import org.springframework.stereotype.Component;

@Component
public class StoredFileMapper {

    public StoredFileRef toRef(StoredFile storedFile) {
        return new StoredFileRef(
                storedFile.getId(),
                storedFile.getOriginalFileName(),
                storedFile.getContentType(),
                storedFile.getFileSize());
    }
}
