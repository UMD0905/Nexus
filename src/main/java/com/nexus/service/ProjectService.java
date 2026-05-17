package com.nexus.service;

import com.nexus.model.Project;
import com.nexus.model.enums.ProjectStatus;
import com.nexus.repository.CategoryRepository;
import com.nexus.repository.ProjectRepository;

import java.util.List;
import java.util.Optional;

/** Business logic for {@link Project}. */
public class ProjectService {

    private final ProjectRepository  projectRepository;
    private final CategoryRepository categoryRepository;

    public ProjectService(ProjectRepository projectRepository, CategoryRepository categoryRepository) {
        this.projectRepository  = projectRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public List<Project> getProjectsByCategory(long categoryId) {
        return projectRepository.findByCategoryId(categoryId);
    }

    public Optional<Project> getProjectById(long id) {
        var project = projectRepository.findById(id);
        project.ifPresent(p -> {
            if (p.getCategoryId() != null) {
                categoryRepository.findById(p.getCategoryId()).ifPresent(p::setCategory);
            }
        });
        return project;
    }

    public Project createProject(Project project) {
        validate(project);
        if (project.getColor() == null) {
            project.setColor("#4A90D9");
        }
        if (project.getStatus() == null) {
            project.setStatus(ProjectStatus.ACTIVE);
        }
        return projectRepository.save(project);
    }

    public Project updateProject(Project project) {
        if (project.getId() == null) {
            throw new IllegalArgumentException("Cannot update project without id");
        }
        validate(project);
        return projectRepository.update(project);
    }

    public void deleteProject(long id) {
        projectRepository.delete(id);
    }

    private void validate(Project project) {
        if (project.getName() == null || project.getName().isBlank()) {
            throw new IllegalArgumentException("Project name must not be blank");
        }
    }
}
