package com.mmu.repository.model;

import jakarta.persistence.*;

@Entity
public class Project {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String description;
    private String studentName;
    private String studentEmail;       // NEW: student's contact email
    private String supervisorName;
    private String status;             // PENDING_SUPERVISOR → SUPERVISOR_APPROVED → FORWARDED_TO_SPONSOR → FUNDED / REJECTED
    private String plagiarismReport;
    private String filePath;           // NEW: stores uploaded file path (NOT studentName)
    private Double allocatedFunds;     // NEW: funding amount set by sponsor
    private boolean forwardedToSponsor; // NEW: tracks if supervisor forwarded it
    private String sponsorName;        // NEW: which sponsor it was forwarded to

    public Project() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPlagiarismReport() { return plagiarismReport; }
    public void setPlagiarismReport(String plagiarismReport) { this.plagiarismReport = plagiarismReport; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Double getAllocatedFunds() { return allocatedFunds; }
    public void setAllocatedFunds(Double allocatedFunds) { this.allocatedFunds = allocatedFunds; }

    public boolean isForwardedToSponsor() { return forwardedToSponsor; }
    public void setForwardedToSponsor(boolean forwardedToSponsor) { this.forwardedToSponsor = forwardedToSponsor; }

    public String getSponsorName() { return sponsorName; }
    public void setSponsorName(String sponsorName) { this.sponsorName = sponsorName; }
}
