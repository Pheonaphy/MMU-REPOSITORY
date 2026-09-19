package com.mmu.repository.controller;

import com.mmu.repository.data.ProjectRepository;
import com.mmu.repository.data.UserRepository;
import com.mmu.repository.model.Project;
import com.mmu.repository.model.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepo;

    @Autowired
    private UserRepository userRepo;

    // ─────────────────────────────────────────────
    // ROOT / LOGIN
    // ─────────────────────────────────────────────

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("projects", projectRepo.findAll());
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session) {

        User user = userRepo.findByUsername(username);

        if (user == null) {
            return "redirect:/?error=userNotFound";
        }

        if (!user.getPassword().equals(password)) {
            return "redirect:/?error=invalidCredentials";
        }

        if (!user.isApproved()) {
            return "redirect:/?error=awaitingApproval";
        }

        session.setAttribute("user", user);

        // Explicit switch cases matching SPONSOR
        switch (user.getRole()) {
            case "ADMIN":       return "redirect:/admin-dashboard";
            case "SUPERVISOR":  return "redirect:/supervisor-dashboard";
            case "SPONSOR":     return "redirect:/sponsor-dashboard";
            case "STUDENT":     return "redirect:/student-dashboard";
            default:            return "redirect:/";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logoutGet(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ─────────────────────────────────────────────
    // REGISTRATION
    // ─────────────────────────────────────────────

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam("username") String username,
                               @RequestParam("email") String email,
                               @RequestParam("password") String password,
                               @RequestParam(value = "role", defaultValue = "STUDENT") String role) {
        try {
            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email.trim());
            newUser.setPassword(password.trim());

            String sanitizedRole = role.trim().toUpperCase();
            
            if (List.of("SUPERVISOR", "SPONSOR", "STUDENT").contains(sanitizedRole)) {
                newUser.setRole(sanitizedRole);
            } else {
                newUser.setRole("STUDENT");
            }

            newUser.setApproved(false);
            userRepo.save(newUser);

            return "redirect:/?registered=true";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/register?error=true";
        }
    }

    // ─────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────

    @GetMapping("/admin-dashboard")
    public String adminDash(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) return "redirect:/";

        model.addAttribute("projects", projectRepo.findAll());
        model.addAttribute("users", userRepo.findAll());
        model.addAttribute("activeTab", "dashboard");
        return "admin-dashboard";
    }

    @GetMapping("/admin-manage-users")
    public String manageUsers(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) return "redirect:/";

        model.addAttribute("projects", projectRepo.findAll());
        model.addAttribute("users", userRepo.findAll());
        model.addAttribute("activeTab", "users");
        return "admin-dashboard";
    }

    @GetMapping("/admin-funding")
public String showAdminFundingDashboard(Model model, HttpSession session) {
    // 1. Enforce admin role validation barrier
    User user = (User) session.getAttribute("user");
    if (user == null || !"ADMIN".equals(user.getRole())) {
        return "redirect:/";
    }

    // 2. Add structural metric counts for the overview statistic cards seen in image_c44ee1.png
    long pendingUsersCount = userRepo.findAll().stream().filter(u -> !u.isApproved()).count();
    long totalProjectsCount = projectRepo.count();
    
    model.addAttribute("usersAwaitingApprovalCount", pendingUsersCount);
    model.addAttribute("totalSystemProjectsCount", totalProjectsCount);

    // 3. CRITICAL FIX: Fetch all projects that have requested funding or are forwarded to sponsors
    // If you don't have a custom finder, use findAll() and filter for presentation
    List<Project> fundingAppeals = projectRepo.findAll().stream()
            .filter(p -> "APPLIED_FOR_FUNDING".equals(p.getStatus()) || p.isForwardedToSponsor())
            .toList();
    
    // Inject the collection precisely named for your Thymeleaf layout template loops
    model.addAttribute("fundingAppeals", fundingAppeals);
    model.addAttribute("currentUser", user);

    // Make sure this points exactly to your admin template file name
    return "admin-funding"; 
}

    @PostMapping("/approve-user")
    public String approveUser(@RequestParam Long userId) {
        userRepo.findById(userId).ifPresent(u -> {
            u.setApproved(true);
            userRepo.save(u);
        });
        return "redirect:/admin-manage-users";
    }

    @PostMapping("/reject-user")
    public String rejectUser(@RequestParam Long userId) {
        userRepo.findById(userId).ifPresent(u -> {
            u.setApproved(false);
            userRepo.save(u);
        });
        return "redirect:/admin-manage-users";
    }

    @PostMapping("/delete-user")
    public String deleteUser(@RequestParam Long userId) {
        userRepo.deleteById(userId);
        return "redirect:/admin-manage-users";
    }

    @PostMapping("/approve-project")
    public String approveProject(@RequestParam Long projectId) {
        projectRepo.findById(projectId).ifPresent(p -> {
            p.setStatus("APPROVED");
            projectRepo.save(p);
        });
        return "redirect:/admin-dashboard";
    }

    @PostMapping("/delete-project")
    public String deleteProject(@RequestParam Long projectId) {
        projectRepo.deleteById(projectId);
        return "redirect:/admin-dashboard";
    }

    // ─────────────────────────────────────────────
    // STUDENT
    // ─────────────────────────────────────────────

    @GetMapping("/student-dashboard")
public String studentDash(@RequestParam(value = "tab", defaultValue = "submissions") String tab,
                          Model model,
                          HttpSession session,
                          jakarta.servlet.http.HttpServletRequest request) {
    User user = (User) session.getAttribute("user");
    if (user == null || !"STUDENT".equals(user.getRole())) {
        return "redirect:/";
    }

    model.addAttribute("currentUser", user);
    model.addAttribute("activeTab", tab);

    // --- NEW: EXTRACT FLASH ATTRIBUTES FROM REDIRECT ---
    java.util.Map<String, ?> flashMap = org.springframework.web.servlet.support.RequestContextUtils.getInputFlashMap(request);
    if (flashMap != null) {
        if (flashMap.containsKey("titleRepetitionWarning")) {
            model.addAttribute("titleRepetitionWarning", flashMap.get("titleRepetitionWarning"));
        }
        if (flashMap.containsKey("titleClearanceMessage")) {
            model.addAttribute("titleClearanceMessage", flashMap.get("titleClearanceMessage"));
        }
    }

    // Fetch student projects safely
    List<Project> myProjects = new ArrayList<>();
    try {
        List<Project> projectsBySub = projectRepo.findByStudentName(user.getUsername());
        if (projectsBySub != null) {
            myProjects.addAll(projectsBySub);
        }
    } catch (Exception e) {
        System.err.println("Error fetching student projects: " + e.getMessage());
    }
    model.addAttribute("myProjects", myProjects);

    // Fetch approved supervisors
    List<User> supervisors = userRepo.findAll().stream()
            .filter(u -> "SUPERVISOR".equals(u.getRole()) && u.isApproved())
            .toList();
    model.addAttribute("supervisors", supervisors);

    return "student-dashboard";
}

    @PostMapping("/submit-project")
    public String submitProject(@RequestParam("title") String title,
                                @RequestParam("description") String description,
                                @RequestParam("studentEmail") String studentEmail,
                                @RequestParam("supervisorName") String supervisorName,
                                @RequestParam(value = "projectFile", required = false) MultipartFile projectFile,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        // 1. Session & Role Validation Barrier
        User user = (User) session.getAttribute("user");
        if (user == null || !"STUDENT".equals(user.getRole())) return "redirect:/";

        // 2. Scan and calculate title similarities against old records
        List<Project> allExistingProjects = projectRepo.findAll();
        double highestTitleMatch = 0.0;
        String duplicateTitleFound = "";

        for (Project existing : allExistingProjects) {
            double currentScore = calculateTitleSimilarity(title.trim(), existing.getTitle());
            if (currentScore > highestTitleMatch) {
                highestTitleMatch = currentScore;
                duplicateTitleFound = existing.getTitle();
            }
        }

        // 3. Match Evaluation & Strict Rejection Logic
        if (highestTitleMatch > 70.0) {
            redirectAttributes.addFlashAttribute("titleRepetitionWarning", 
                String.format("Submission Rejected: This project has already been done! A project with a highly similar title ('%s') already exists in the institutional repository. Please use another title for your research.", 
                              duplicateTitleFound));
            
            // Halts execution and bounces back to the form input view layout immediately
            return "redirect:/student-dashboard?tab=upload";
        }

        // 4. Persistence Pipeline execution if the title signature passes validation safely
        redirectAttributes.addFlashAttribute("titleClearanceMessage", "Success: Project title is unique. No structural repetitions found in the institutional repository.");
        saveProjectToDatabase(title, description, studentEmail, supervisorName, projectFile, user, "PENDING_SUPERVISOR");
        
        return "redirect:/student-dashboard?tab=submissions&success=submitted";
    }

    // Helper process logic encapsulating standard upload system IO routines safely
    private void saveProjectToDatabase(String title, String description, String studentEmail, String supervisorName, 
                                       MultipartFile projectFile, User user, String status) {
        Project project = new Project();
        project.setTitle(title.trim());
        project.setDescription(description.trim());
        project.setStudentName(user.getUsername());          
        project.setStudentEmail(studentEmail.trim());        
        project.setSupervisorName(supervisorName.trim());
        project.setStatus(status);
        project.setForwardedToSponsor(false);
        project.setAllocatedFunds(null);

        if (projectFile != null && !projectFile.isEmpty()) {
            try {
                Path uploadDir = Paths.get(System.getProperty("user.home"), "mmu_uploads");
                if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

                String originalFilename = projectFile.getOriginalFilename();
                if (originalFilename == null || originalFilename.trim().isEmpty()) {
                    originalFilename = "file";
                }
                String safeFileName = System.currentTimeMillis() + "_"
                        + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
                Path savedPath = uploadDir.resolve(safeFileName);
                Files.write(savedPath, projectFile.getBytes());

                project.setFilePath(savedPath.toString());
            } catch (Exception e) {
                System.err.println("File upload failed: " + e.getMessage());
                project.setFilePath(null);
            }
        }
        projectRepo.save(project);
    }

    private double calculateTitleSimilarity(String titleA, String titleB) {
        if (titleA == null || titleB == null) {
            return 0.0;
        }

        String[] wordsA = titleA.trim().toLowerCase().split("\\s+");
        String[] wordsB = titleB.trim().toLowerCase().split("\\s+");
        if (wordsA.length == 0 || wordsB.length == 0) {
            return 0.0;
        }

        int commonCount = 0;
        for (String a : wordsA) {
            for (String b : wordsB) {
                if (a.equals(b)) {
                    commonCount++;
                    break;
                }
            }
        }

        int maxSize = Math.max(wordsA.length, wordsB.length);
        return maxSize == 0 ? 0.0 : (100.0 * commonCount / maxSize);
    }
    // ─────────────────────────────────────────────
    // UC-06 STUDENT APPLY FOR ACTIVE FUNDING
    // ─────────────────────────────────────────────
    @PostMapping("/student/apply-funding")
    public String applyForFunding(@RequestParam("projectId") Long projectId,
                                  @RequestParam("fundingOpportunityId") Long fundingOpportunityId,
                                  HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"STUDENT".equals(user.getRole())) return "redirect:/";

        projectRepo.findById(projectId).ifPresent(project -> {
            project.setStatus("APPLIED_FOR_FUNDING");
            projectRepo.save(project);
        });

        return "redirect:/student-dashboard?tab=submissions&success=applied";
    }

    // ─────────────────────────────────────────────
    // SUPERVISOR (LECTURER)
    // ─────────────────────────────────────────────

    @GetMapping("/supervisor-dashboard")
    public String supervisorDash(@RequestParam(value = "tab", defaultValue = "pending") String tab,
                                 Model model,
                                 HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"SUPERVISOR".equals(user.getRole())) return "redirect:/";

        model.addAttribute("currentUser", user);
        model.addAttribute("activeTab", tab);

        List<Project> myProjects = projectRepo.findAll().stream()
                .filter(p -> user.getUsername().equals(p.getSupervisorName()))
                .toList();

        List<Project> pendingProjects = myProjects.stream()
                .filter(p -> "PENDING_SUPERVISOR".equals(p.getStatus()))
                .toList();

        List<Project> approvedProjects = myProjects.stream()
                .filter(p -> "SUPERVISOR_APPROVED".equals(p.getStatus()) || 
                             "FORWARDED_TO_SPONSOR".equals(p.getStatus()) ||
                             "FUNDED".equals(p.getStatus()))
                .toList();

        List<Project> rejectedProjects = myProjects.stream()
                .filter(p -> "REJECTED_BY_SUPERVISOR".equals(p.getStatus()))
                .toList();

        // Fetches approved Sponsors
        List<User> sponsors = userRepo.findAll().stream()
                .filter(u -> "SPONSOR".equals(u.getRole()) && u.isApproved())
                .toList();

        model.addAttribute("pendingProjects", pendingProjects);
        model.addAttribute("approvedProjects", approvedProjects);
        model.addAttribute("rejectedProjects", rejectedProjects);
        model.addAttribute("sponsors", sponsors);

        return "supervisor-dashboard";
    }

    @PostMapping("/supervisor-evaluate")
    public String supervisorEvaluate(@RequestParam Long projectId,
                                     @RequestParam String action,
                                     HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"SUPERVISOR".equals(user.getRole())) return "redirect:/";

        projectRepo.findById(projectId).ifPresent(project -> {
            if ("APPROVE".equals(action)) {
                project.setStatus("SUPERVISOR_APPROVED");
            } else if ("REJECT".equals(action)) {
                project.setStatus("REJECTED_BY_SUPERVISOR");
            }
            projectRepo.save(project);
        });

        return "redirect:/supervisor-dashboard?tab=pending";
    }

    @PostMapping("/forward-to-sponsor")
    public String forwardToSponsor(@RequestParam Long projectId,
                                   @RequestParam String sponsorUsername,
                                   HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"SUPERVISOR".equals(user.getRole())) return "redirect:/";

        projectRepo.findById(projectId).ifPresent(project -> {
            project.setStatus("FORWARDED_TO_SPONSOR");
            project.setSponsorName(sponsorUsername);
            project.setForwardedToSponsor(true);
            projectRepo.save(project);
        });

        return "redirect:/supervisor-dashboard?tab=approved&success=forwarded";
    }

    // ─────────────────────────────────────────────
    // SPONSOR (DASHBOARD)
    // ─────────────────────────────────────────────

    @GetMapping("/sponsor-dashboard")
    public String sponsorDash(@RequestParam(value = "tab", defaultValue = "pipeline") String tab,
                              Model model,
                              HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"SPONSOR".equals(user.getRole())) return "redirect:/";

        model.addAttribute("currentUser", user);
        model.addAttribute("activeTab", tab);

        List<Project> forwardedProjects = projectRepo.findAll().stream()
                .filter(p -> user.getUsername().equals(p.getSponsorName()))
                .toList();

        List<Project> pendingFunding = forwardedProjects.stream()
                .filter(p -> "FORWARDED_TO_SPONSOR".equals(p.getStatus()) || "APPLIED_FOR_FUNDING".equals(p.getStatus()))
                .toList();

        List<Project> fundedProjects = forwardedProjects.stream()
                .filter(p -> "FUNDED".equals(p.getStatus()))
                .toList();

        model.addAttribute("pendingFunding", pendingFunding);
        model.addAttribute("fundedProjects", fundedProjects);

        return "sponsor-dashboard";
    }

    // ─────────────────────────────────────────────
    // UC-05 SPONSOR ADVERTISE NEW FUNDING CAMPAIGNS
    // ─────────────────────────────────────────────
    @PostMapping("/sponsor/advertise")
    public String sponsorAdvertise(@RequestParam("title") String title,
                                   @RequestParam("description") String description,
                                   @RequestParam("amount") Double amount,
                                   HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"SPONSOR".equals(user.getRole())) return "redirect:/";

        System.out.println("Sponsor " + user.getUsername() + " advertised campaign: " + title + " worth " + amount + " KES");
        
        return "redirect:/sponsor-dashboard?tab=pipeline&success=advertised";
    }

    @PostMapping("/sponsor-fund")
    public String sponsorFund(@RequestParam Long projectId,
                              @RequestParam Double fundingAmount,
                              HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"SPONSOR".equals(user.getRole())) return "redirect:/";

        projectRepo.findById(projectId).ifPresent(project -> {
            project.setStatus("FUNDED");
            project.setAllocatedFunds(fundingAmount);
            projectRepo.save(project);
            System.out.println("Project '" + project.getTitle() + "' funded: $" + fundingAmount
                    + " by Sponsor: " + user.getUsername());
        });

        return "redirect:/sponsor-dashboard?tab=pipeline&success=funded";
    }

    @PostMapping("/sponsor-decline")
    public String sponsorDecline(@RequestParam Long projectId,
                                 HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"SPONSOR".equals(user.getRole())) return "redirect:/";

        projectRepo.findById(projectId).ifPresent(project -> {
            project.setStatus("REJECTED_BY_SPONSOR");
            projectRepo.save(project);
        });

        return "redirect:/sponsor-dashboard?tab=pipeline";
    }

    // ─────────────────────────────────────────────
    // FILE MANAGEMENT PIPELINE
    // ─────────────────────────────────────────────

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam Long id) {
        return projectRepo.findById(id).map(project -> {
            if (project.getFilePath() == null) {
                return ResponseEntity.notFound().<Resource>build();
            }
            try {
                Path path = Paths.get(project.getFilePath());
                Resource resource = new UrlResource(path.toUri());
                if (resource.exists() || resource.isReadable()) {
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                            .body(resource);
                }
            } catch (Exception e) {
                System.err.println("File retrieval failed: " + e.getMessage());
            }
            return ResponseEntity.notFound().<Resource>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}

