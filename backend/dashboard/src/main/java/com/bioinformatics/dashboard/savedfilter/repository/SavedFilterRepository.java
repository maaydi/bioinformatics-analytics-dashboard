package com.bioinformatics.dashboard.savedfilter.repository;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.savedfilter.entity.SavedFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedFilterRepository extends JpaRepository<SavedFilter, Long> {

    Page<SavedFilter> findByOwner(AppUser owner, Pageable pageable);

    boolean existsByOwnerAndName(AppUser owner, String name);


}
