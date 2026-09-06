package com.bioinformatics.importservice.listener;

import com.bioinformatics.common.gene.entity.ProteinEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ImportUniprotSkipListener implements SkipListener<String, ProteinEntry> {

    @Override
    public void onSkipInWrite(ProteinEntry item, Throwable t) {
        log.error("Failed to write protein with accession: {} Reason: [{}] {}",
                item.getAccession(), t.getClass().getName(), t.getMessage());
    }
}
