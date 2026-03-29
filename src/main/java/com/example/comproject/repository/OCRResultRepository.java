package com.example.comproject.repository;

import com.example.comproject.entity.OCRResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

import java.util.Optional;

@Repository
public interface OCRResultRepository extends JpaRepository<OCRResult, UUID> {
    Optional<OCRResult> findByClaimDocumentId(Long claimDocumentId);
}
