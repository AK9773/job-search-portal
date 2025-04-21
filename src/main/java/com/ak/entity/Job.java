package com.ak.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
public class Job {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String description;

	@ManyToOne
	@JoinColumn(name = "company_id", nullable = false)
	private Company company;

	@Column(nullable = false)
	private String location;

	private Double salary;

	@CreationTimestamp
	private LocalDateTime postedDate;

	@Column(nullable = false)
	private LocalDate expiryDate;

	@ManyToOne
	@JoinColumn(name = "posted_by", nullable = false)
	private Users postedBy;

	// Applications for this job
	@OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Application> applications = new ArrayList<>();
}