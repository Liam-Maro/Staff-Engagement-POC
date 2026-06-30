import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SkillService } from '../../services/skill.service';
import { Skill, SkillSearchResult } from '../../models/skill.models';
import { EmployeeService } from '../../../employees/services/employee.service';
import { Employee } from '../../../employees/models/employee.models';
import { ConfirmModalComponent } from '../../../shared/components/confirm-modal/confirm-modal.component';

@Component({
  selector: 'app-skill-search',
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmModalComponent],
  templateUrl: './skill-search.component.html',
  styleUrls: ['./skill-search.component.css']
})
export class SkillSearchComponent implements OnInit {

  query = '';
  results = signal<SkillSearchResult[]>([]);
  hasSearched = signal(false);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  // Add skill form
  showAddForm = signal(false);
  employees = signal<Employee[]>([]);
  proficiencyLevels = ['Beginner', 'Intermediate', 'Advanced', 'Expert'];
  skillForm = {
    employeeId: '',
    name: '',
    yearsExperience: 0,
    projectCount: 0,
    proficiency: 'INTERMEDIATE'
  };
  successMessage = signal<string | null>(null);

  // Edit/manage skills
  showManageSection = signal(false);
  managedEmployeeId = '';
  managedSkills = signal<any[]>([]);
  editingSkillId = signal<string | null>(null);
  editForm = { name: '', yearsExperience: 0, projectCount: 0, proficiency: 'Intermediate' };

  // Delete confirmation modal
  showModal = signal(false);
  modalTitle = '';
  modalMessage = '';
  modalConfirmText = 'Delete';
  private pendingAction: (() => void) | null = null;

  constructor(
    private skillService: SkillService,
    private employeeService: EmployeeService
  ) {}

  ngOnInit(): void {
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.employeeService.findAll().subscribe({
      next: (data) => this.employees.set(data),
      error: () => {}
    });
  }

  onSearch(): void {
    if (!this.query.trim()) return;

    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.isLoading.set(true);
    this.hasSearched.set(true);

    this.skillService.search(this.query.trim()).subscribe({
      next: (data) => {
        this.results.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('Search could not be completed. Please try again.');
      }
    });
  }

  openAddForm(): void {
    this.skillForm = { employeeId: '', name: '', yearsExperience: 0, projectCount: 0, proficiency: 'INTERMEDIATE' };
    this.showAddForm.set(true);
    this.successMessage.set(null);
  }

  cancelAddForm(): void {
    this.showAddForm.set(false);
  }

  saveSkill(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const request = {
      employeeId: this.skillForm.employeeId,
      name: this.skillForm.name,
      yearsExperience: this.skillForm.yearsExperience,
      projectCount: this.skillForm.projectCount,
      proficiency: this.skillForm.proficiency
    };

    this.skillService.create(request).subscribe({
      next: () => {
        this.successMessage.set(`Skill "${this.skillForm.name}" added successfully.`);
        this.showAddForm.set(false);
        // Re-run search if there was one active
        if (this.hasSearched() && this.query.trim()) {
          this.onSearch();
        }
      },
      error: () => {
        this.errorMessage.set('Failed to add skill. Please check all fields and try again.');
      }
    });
  }

  get isFormValid(): boolean {
    return !!this.skillForm.employeeId && !!this.skillForm.name.trim() && !!this.skillForm.proficiency;
  }

  // ==================== Manage Skills ====================

  openManageSection(): void {
    this.showManageSection.set(true);
    this.managedEmployeeId = '';
    this.managedSkills.set([]);
    this.editingSkillId.set(null);
  }

  closeManageSection(): void {
    this.showManageSection.set(false);
    this.editingSkillId.set(null);
  }

  loadEmployeeSkills(): void {
    if (!this.managedEmployeeId) return;
    this.skillService.findByEmployee(this.managedEmployeeId).subscribe({
      next: (skills) => this.managedSkills.set(skills),
      error: () => this.errorMessage.set('Failed to load skills.')
    });
  }

  openEditForm(skill: Skill): void {
    this.editForm = {
      name: skill.name,
      yearsExperience: skill.yearsExperience,
      projectCount: skill.projectCount,
      proficiency: skill.proficiency
    };
    this.editingSkillId.set(skill.id);
  }

  cancelEdit(): void {
    this.editingSkillId.set(null);
  }

  saveEdit(): void {
    const id = this.editingSkillId();
    if (!id) return;

    this.skillService.update(id, this.editForm).subscribe({
      next: () => {
        this.successMessage.set('Skill updated successfully.');
        this.editingSkillId.set(null);
        this.loadEmployeeSkills();
        if (this.hasSearched() && this.query.trim()) this.onSearch();
      },
      error: () => this.errorMessage.set('Failed to update skill.')
    });
  }

  deleteSkill(skill: Skill): void {
    this.modalTitle = 'Delete Skill';
    this.modalMessage = `Are you sure you want to delete "${skill.name}"? This action cannot be undone.`;
    this.modalConfirmText = 'Delete';
    this.pendingAction = () => {
      this.skillService.delete(skill.id).subscribe({
        next: () => {
          this.successMessage.set(`Skill "${skill.name}" deleted.`);
          this.loadEmployeeSkills();
          if (this.hasSearched() && this.query.trim()) this.onSearch();
        },
        error: () => this.errorMessage.set('Failed to delete skill.')
      });
    };
    this.showModal.set(true);
  }

  onModalConfirm(): void {
    this.showModal.set(false);
    if (this.pendingAction) {
      this.pendingAction();
      this.pendingAction = null;
    }
  }

  onModalCancel(): void {
    this.showModal.set(false);
    this.pendingAction = null;
  }
}
