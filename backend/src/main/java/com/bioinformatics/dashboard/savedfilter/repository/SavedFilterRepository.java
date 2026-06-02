package com.bioinformatics.dashboard.savedfilter.repository;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.savedfilter.entity.SavedFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedFilterRepository extends JpaRepository<SavedFilter, Long> {

    List<SavedFilter> findByOwnerOrderByCreatedAtDesc(AppUser owner);

    boolean existsByOwnerAndName(AppUser owner, String name);


}
