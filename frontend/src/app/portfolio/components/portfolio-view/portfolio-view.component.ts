import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { PortfolioService } from '../../services/portfolio.service';
import {
  FullPortfolio,
  Education,
  Project,
  PortfolioLink,
  ProficiencyLevel
} from '../../models/portfolio.models';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-portfolio-view',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './portfolio-view.component.html',
  styleUrls: ['./portfolio-view.component.css']
})
export class PortfolioViewComponent implements OnInit {

  portfolio = signal<FullPortfolio | null>(null);
  isLoading = signal(true);
  notFound = signal(false);
  errorMessage = signal<string | null>(null);

  proficiencyLevels: ProficiencyLevel[] = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'];

  // Education form
  showEducationForm = signal(false);
  editingEducationId = signal<string | null>(null);
  educationForm = { institution: '', degree: '', fieldOfStudy: '', graduationDate: '' };

  // Project form
  showProjectForm = signal(false);
  editingProjectId = signal<string | null>(null);
  projectForm = { projectName: '', description: '', role: '', technologies: '', startDate: '', endDate: '' };

  // Link form
  showLinkForm = signal(false);
  editingLinkId = signal<string | null>(null);
  linkForm = { url: '', label: '' };

  private employeeId = '';

  constructor(
    private route: ActivatedRoute,
    private portfolioService: PortfolioService
  ) {}

  ngOnInit(): void {
    this.employeeId = this.route.snapshot.paramMap.get('employeeId') ?? '';
    this.loadPortfolio();
  }

  loadPortfolio(): void {
    this.isLoading.set(true);
    this.notFound.set(false);
    this.errorMessage.set(null);

    this.portfolioService.getFullPortfolio(this.employeeId).subscribe({
      next: (data) => {
        this.portfolio.set(data);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        if (err.status === 404) {
          this.notFound.set(true);
        } else {
          this.errorMessage.set('Failed to load portfolio. Please try again.');
        }
      }
    });
  }

  retry(): void {
    this.loadPortfolio();
  }

  // ==================== Education ====================

  openAddEducationForm(): void {
    this.educationForm = { institution: '', degree: '', fieldOfStudy: '', graduationDate: '' };
    this.editingEducationId.set(null);
    this.showEducationForm.set(true);
  }

  openEditEducationForm(edu: Education): void {
    this.educationForm = {
      institution: edu.institution,
      degree: edu.degree,
      fieldOfStudy: edu.fieldOfStudy ?? '',
      graduationDate: edu.graduationDate ?? ''
    };
    this.editingEducationId.set(edu.id);
    this.showEducationForm.set(true);
  }

  cancelEducationForm(): void {
    this.showEducationForm.set(false);
    this.editingEducationId.set(null);
  }

  saveEducation(): void {
    const request = {
      institution: this.educationForm.institution,
      degree: this.educationForm.degree,
      fieldOfStudy: this.educationForm.fieldOfStudy || null,
      graduationDate: this.educationForm.graduationDate || null
    };
    const editId = this.editingEducationId();

    if (editId) {
      this.portfolioService.updateEducation(editId, request).subscribe({
        next: () => { this.cancelEducationForm(); this.loadPortfolio(); },
        error: () => this.errorMessage.set('Failed to update education.')
      });
    } else {
      this.portfolioService.createEducation(this.employeeId, request).subscribe({
        next: () => { this.cancelEducationForm(); this.loadPortfolio(); },
        error: () => this.errorMessage.set('Failed to add education.')
      });
    }
  }

  deleteEducation(edu: Education): void {
    if (!confirm(`Delete education "${edu.institution}"?`)) return;
    this.portfolioService.deleteEducation(edu.id).subscribe({
      next: () => this.loadPortfolio(),
      error: () => this.errorMessage.set('Failed to delete education.')
    });
  }

  // ==================== Projects ====================

  openAddProjectForm(): void {
    this.projectForm = { projectName: '', description: '', role: '', technologies: '', startDate: '', endDate: '' };
    this.editingProjectId.set(null);
    this.showProjectForm.set(true);
  }

  openEditProjectForm(project: Project): void {
    this.projectForm = {
      projectName: project.projectName,
      description: project.description ?? '',
      role: project.role,
      technologies: project.technologies.join(', '),
      startDate: project.startDate,
      endDate: project.endDate ?? ''
    };
    this.editingProjectId.set(project.id);
    this.showProjectForm.set(true);
  }

  cancelProjectForm(): void {
    this.showProjectForm.set(false);
    this.editingProjectId.set(null);
  }

  saveProject(): void {
    const technologies = this.projectForm.technologies
      .split(',')
      .map(t => t.trim())
      .filter(t => t.length > 0);

    const request = {
      projectName: this.projectForm.projectName,
      description: this.projectForm.description || null,
      role: this.projectForm.role,
      technologies,
      startDate: this.projectForm.startDate,
      endDate: this.projectForm.endDate || null
    };
    const editId = this.editingProjectId();

    if (editId) {
      this.portfolioService.updateProject(editId, request).subscribe({
        next: () => { this.cancelProjectForm(); this.loadPortfolio(); },
        error: () => this.errorMessage.set('Failed to update project.')
      });
    } else {
      this.portfolioService.createProject(this.employeeId, request).subscribe({
        next: () => { this.cancelProjectForm(); this.loadPortfolio(); },
        error: () => this.errorMessage.set('Failed to add project.')
      });
    }
  }

  deleteProject(project: Project): void {
    if (!confirm(`Delete project "${project.projectName}"?`)) return;
    this.portfolioService.deleteProject(project.id).subscribe({
      next: () => this.loadPortfolio(),
      error: () => this.errorMessage.set('Failed to delete project.')
    });
  }

  // ==================== Links ====================

  openAddLinkForm(): void {
    this.linkForm = { url: '', label: '' };
    this.editingLinkId.set(null);
    this.showLinkForm.set(true);
  }

  openEditLinkForm(link: PortfolioLink): void {
    this.linkForm = { url: link.url, label: link.label };
    this.editingLinkId.set(link.id);
    this.showLinkForm.set(true);
  }

  cancelLinkForm(): void {
    this.showLinkForm.set(false);
    this.editingLinkId.set(null);
  }

  saveLink(): void {
    const request = { url: this.linkForm.url, label: this.linkForm.label };
    const editId = this.editingLinkId();

    if (editId) {
      this.portfolioService.updateLink(editId, request).subscribe({
        next: () => { this.cancelLinkForm(); this.loadPortfolio(); },
        error: () => this.errorMessage.set('Failed to update link.')
      });
    } else {
      this.portfolioService.createLink(this.employeeId, request).subscribe({
        next: () => { this.cancelLinkForm(); this.loadPortfolio(); },
        error: () => this.errorMessage.set('Failed to add link.')
      });
    }
  }

  deleteLink(link: PortfolioLink): void {
    if (!confirm(`Delete link "${link.label}"?`)) return;
    this.portfolioService.deleteLink(link.id).subscribe({
      next: () => this.loadPortfolio(),
      error: () => this.errorMessage.set('Failed to delete link.')
    });
  }
}
