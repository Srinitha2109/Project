package com.example.comproject.repository;

import com.example.comproject.entity.GeneralLiabilityDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneralLiabilityDetailRepository extends JpaRepository<GeneralLiabilityDetail, Long> {
}
