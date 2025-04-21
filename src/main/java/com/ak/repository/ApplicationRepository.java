package com.ak.repository;

import com.ak.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
	Page<Application> findByJobId(Long jobId, Pageable pageable);

	Page<Application> findByUserId(Long userId, Pageable pageable);
}