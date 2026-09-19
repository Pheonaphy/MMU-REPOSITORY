package com.mmu.repository.data;

import com.mmu.repository.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByStudentName(String studentName);

}