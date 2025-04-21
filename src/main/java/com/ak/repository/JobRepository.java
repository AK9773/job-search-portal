package com.ak.repository;

import com.ak.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
	Page<Job> findByTitleContainingIgnoreCase(String title, Pageable pageable);

	Page<Job> findByLocationContainingIgnoreCase(String location, Pageable pageable);

	Page<Job> findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCase(String title, String location, Pageable pageable);

	Page<Job> findByCompanyId(Long companyId, Pageable pageable);

	Page<Job> findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndCompanyId(String title, String location, Long companyId,
			Pageable pageable);
}