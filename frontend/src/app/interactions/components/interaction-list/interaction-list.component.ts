import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { InteractionService, InteractionFilterParams } from '../../services/interaction.service';
import { InteractionResponse, InteractionType, INTERACTION_TYPES, PageResponse } from '../../models/interaction.model';
import { EmployeeService } from '../../../employees/services/employee.service';
import { Employee } from '../../../employees/models/employee.models';
import { InteractionContext } from '../../../tasks/models/task.model';
import { TaskFormComponent } from '../../../tasks/components/task-form/task-form.component';
import { ToastNotificationComponent } from '../../../shared/components/toast-notification/toast-notification.component';

@Component({
  selector: 'app-interaction-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, TaskFormComponent, ToastNotificationComponent],
  templateUrl: './interaction-list.component.html',
  styleUrl: './interaction-list.component.css'
})
export class InteractionListComponent implements OnInit {

  interactions = signal<InteractionResponse[]>([]);
  employees = signal<Employee[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Task form modal state
  showTaskFormModal = signal(false);
  taskFormContext = signal<InteractionContext | null>(null);

  // Toast notification state
  toastMessage = signal<string | null>(null);
  toastType = signal<'success' | 'error'>('success');

  // Pagination
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  pageSize = 20;

  // Filters
  selectedEmployeeId = '';
  selectedType: InteractionType | '' = '';
  interactionTypes = INTERACTION_TYPES;

  constructor(
    private interactionService: InteractionService,
    private employeeService: EmployeeService
  ) {}

  ngOnInit(): void {
    this.loadEmployees();
    this.loadInteractions();
  }

  loadEmployees(): void {
    this.employeeService.findAll().subscribe({
      next: (data) => this.employees.set(data),
      error: () => {} // silently fail — employee dropdown is non-critical
    });
  }

  loadInteractions(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const params: InteractionFilterParams = {
      page: this.currentPage(),
      size: this.pageSize
    };

    if (this.selectedEmployeeId) {
      params.employeeId = this.selectedEmployeeId;
    }
    if (this.selectedType) {
      params.type = this.selectedType;
    }

    this.interactionService.getAll(params).subscribe({
      next: (response: PageResponse<InteractionResponse>) => {
        this.interactions.set(response.content);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.currentPage.set(response.number);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load interactions. Please try again.');
        this.isLoading.set(false);
      }
    });
  }

  applyFilters(): void {
    this.currentPage.set(0);
    this.loadInteractions();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadInteractions();
    }
  }

  nextPage(): void {
    this.goToPage(this.currentPage() + 1);
  }

  previousPage(): void {
    this.goToPage(this.currentPage() - 1);
  }

  truncateNotes(notes: string | null): string {
    if (!notes) return '';
    if (notes.length > 100) {
      return notes.substring(0, 100) + '...';
    }
    return notes;
  }

  formatType(type: InteractionType): string {
    return type.replace(/_/g, ' ');
  }

  getEmployeeName(employeeId: string): string {
    const emp = this.employees().find(e => e.id === employeeId);
    return emp ? `${emp.firstName} ${emp.lastName}` : employeeId;
  }

  retry(): void {
    this.loadInteractions();
  }

  openCreateTask(interaction: InteractionResponse): void {
    this.taskFormContext.set({
      interactionId: interaction.id,
      employeeId: interaction.employeeId,
      interactionType: interaction.type,
      interactionDate: interaction.occurredAt
    });
    this.showTaskFormModal.set(true);
  }

  onModalClose(): void {
    this.showTaskFormModal.set(false);
  }

  onTaskCreated(): void {
    this.showTaskFormModal.set(false);
    this.toastMessage.set('Follow-up task created successfully');
    this.toastType.set('success');
  }

  onToastDismissed(): void {
    this.toastMessage.set(null);
  }
}
