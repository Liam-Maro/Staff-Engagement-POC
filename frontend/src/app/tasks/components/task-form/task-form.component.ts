import { Component, EventEmitter, HostListener, Input, OnInit, Output, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';

import { TaskService } from '../../services/task.service';
import { AuthService } from '../../../auth/services/auth.service';
import { StaffService } from '../../../staff/services/staff.service';
import { EmployeeService } from '../../../employees/services/employee.service';
import { StaffMember } from '../../../staff/models/staff.models';
import { Employee } from '../../../employees/models/employee.models';
import { TaskResponse, CreateTaskRequest, UpdateTaskRequest, InteractionSummary, InteractionContext } from '../../models/task.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="task-form-overlay" (click)="onClose()">
      <div class="task-form-container" (click)="$event.stopPropagation()">
        <div class="task-form-header">
          <h3>{{ editMode ? 'Edit Task' : 'Create Task' }}</h3>
          <button class="close-btn" (click)="onClose()" aria-label="Close form">&times;</button>
        </div>
        <div class="task-form-body">
          @if (showContextBanner()) {
            <div class="context-banner">
              <span class="context-banner-icon">ℹ</span>
              <span class="context-banner-text">{{ contextBannerText() }}</span>
            </div>
          }
          <form [formGroup]="taskForm" (ngSubmit)="onSubmit()">

            <!-- Description -->
            <div class="form-field">
              <label for="description">Description <span class="required">*</span></label>
              <textarea
                id="description"
                formControlName="description"
                rows="4"
                maxlength="2000"
                placeholder="Enter task description..."
              ></textarea>
              <div class="field-footer">
                <span class="char-count" [class.warning]="descriptionLength() > 1800">
                  {{ remainingChars() }} characters remaining
                </span>
                @if (showFieldError('description')) {
                  <span class="field-error">{{ getFieldError('description') }}</span>
                }
              </div>
              @if (getInlineError('description')) {
                <span class="inline-error">{{ getInlineError('description') }}</span>
              }
            </div>

            <!-- Assigned To -->
            <div class="form-field">
              <label for="assigneeId">Assigned To <span class="required">*</span></label>
              <select id="assigneeId" formControlName="assigneeId">
                <option value="">-- Select Staff --</option>
                @for (staff of activeStaff(); track staff.id) {
                  <option [value]="staff.id">{{ staff.email }}{{ staff.id === currentStaffId() ? ' (Me)' : '' }}</option>
                }
              </select>
              @if (showFieldError('assigneeId')) {
                <span class="field-error">{{ getFieldError('assigneeId') }}</span>
              }
              @if (getInlineError('assigneeId')) {
                <span class="inline-error">{{ getInlineError('assigneeId') }}</span>
              }
            </div>

            <!-- Individual -->
            <div class="form-field">
              <label for="individualId">Individual <span class="required">*</span></label>
              <select id="individualId" formControlName="individualId">
                <option value="">-- Select Individual --</option>
                @for (emp of employees(); track emp.id) {
                  <option [value]="emp.id">{{ emp.firstName }} {{ emp.lastName }}</option>
                }
              </select>
              @if (showFieldError('individualId')) {
                <span class="field-error">{{ getFieldError('individualId') }}</span>
              }
              @if (getInlineError('individualId')) {
                <span class="inline-error">{{ getInlineError('individualId') }}</span>
              }
            </div>

            <!-- Due Date -->
            <div class="form-field">
              <label for="dueDate">Due Date</label>
              <input type="date" id="dueDate" formControlName="dueDate" [min]="todayDate" />
              @if (showFieldError('dueDate')) {
                <span class="field-error">{{ getFieldError('dueDate') }}</span>
              }
              @if (getInlineError('dueDate')) {
                <span class="inline-error">{{ getInlineError('dueDate') }}</span>
              }
            </div>

            <!-- Link Interaction Toggle -->
            <div class="form-field">
              <label class="toggle-label">
                <input type="checkbox" formControlName="linkInteraction" />
                Link Interaction
              </label>
            </div>

            <!-- Interaction Dropdown -->
            @if (taskForm.get('linkInteraction')?.value) {
              <div class="form-field">
                <label for="interactionId">Interaction</label>
                @if (!taskForm.get('individualId')?.value) {
                  <select id="interactionId" disabled>
                    <option value="">-- Select an individual first --</option>
                  </select>
                  <span class="field-hint">Select an individual to view available interactions</span>
                } @else {
                  <select id="interactionId" formControlName="interactionId">
                    <option value="">-- Select Interaction --</option>
                    @for (interaction of interactions(); track interaction.id) {
                      <option [value]="interaction.id">{{ interaction.occurredAt | date:'short' }} - {{ interaction.type }}</option>
                    }
                  </select>
                }
                @if (getInlineError('interactionId')) {
                  <span class="inline-error">{{ getInlineError('interactionId') }}</span>
                }
              </div>
            }

            <!-- Server Error -->
            @if (serverError()) {
              <div class="server-error">{{ serverError() }}</div>
            }

            <!-- Form Actions -->
            <div class="form-actions">
              <button type="button" class="cancel-btn" (click)="onClose()">Cancel</button>
              <button type="submit" class="submit-btn" [disabled]="submitting()">
                {{ submitting() ? 'Saving...' : (editMode ? 'Update Task' : 'Create Task') }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .task-form-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .task-form-container {
      background: white;
      border-radius: 8px;
      width: 90%;
      max-width: 600px;
      max-height: 80vh;
      overflow-y: auto;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    }

    .task-form-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid #e5e7eb;
    }

    .task-form-header h3 {
      margin: 0;
      font-size: 1.25rem;
      color: #111827;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 1.5rem;
      cursor: pointer;
      color: #6b7280;
      padding: 0.25rem;
      line-height: 1;
    }

    .close-btn:hover {
      color: #111827;
    }

    .task-form-body {
      padding: 1.5rem;
    }

    .form-field {
      margin-bottom: 1.25rem;
    }

    .form-field label {
      display: block;
      font-weight: 500;
      margin-bottom: 0.5rem;
      color: #374151;
    }

    .required {
      color: #ef4444;
    }

    .form-field textarea,
    .form-field input[type="date"],
    .form-field select {
      width: 100%;
      padding: 0.625rem;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      font-size: 0.9rem;
      box-sizing: border-box;
    }

    .form-field textarea:focus,
    .form-field input:focus,
    .form-field select:focus {
      outline: none;
      border-color: #2563eb;
      box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
    }

    .field-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 0.25rem;
    }

    .char-count {
      font-size: 0.8rem;
      color: #6b7280;
    }

    .char-count.warning {
      color: #d97706;
    }

    .field-error {
      font-size: 0.8rem;
      color: #ef4444;
    }

    .inline-error {
      display: block;
      font-size: 0.8rem;
      color: #ef4444;
      margin-top: 0.25rem;
    }

    .field-hint {
      display: block;
      font-size: 0.8rem;
      color: #6b7280;
      font-style: italic;
      margin-top: 0.25rem;
    }

    .toggle-label {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      cursor: pointer;
    }

    .toggle-label input[type="checkbox"] {
      width: auto;
    }

    .server-error {
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 6px;
      padding: 0.75rem;
      color: #dc2626;
      margin-bottom: 1rem;
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      padding-top: 1rem;
      border-top: 1px solid #e5e7eb;
    }

    .cancel-btn {
      padding: 0.625rem 1.25rem;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      background: white;
      cursor: pointer;
      color: #374151;
    }

    .cancel-btn:hover {
      background: #f9fafb;
    }

    .submit-btn {
      padding: 0.625rem 1.25rem;
      border: none;
      border-radius: 6px;
      background: #2563eb;
      color: white;
      cursor: pointer;
      font-weight: 500;
    }

    .submit-btn:hover:not(:disabled) {
      background: #1d4ed8;
    }

    .submit-btn:disabled {
      background: #93c5fd;
      cursor: not-allowed;
    }

    .context-banner {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      border-radius: 6px;
      padding: 0.75rem 1rem;
      margin-bottom: 1.25rem;
      color: #1e40af;
      font-size: 0.875rem;
    }

    .context-banner-icon {
      font-size: 1rem;
      flex-shrink: 0;
    }

    .context-banner-text {
      line-height: 1.4;
    }
  `]
})
export class TaskFormComponent implements OnInit {
  @Input() editTask: TaskResponse | null = null;
  @Input() interactionContext: InteractionContext | null = null;
  @Output() closed = new EventEmitter<void>();
  @Output() taskCreated = new EventEmitter<void>();
  @Output() taskUpdated = new EventEmitter<void>();

  private readonly UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  private readonly fb = inject(FormBuilder);
  private readonly taskService = inject(TaskService);
  private readonly authService = inject(AuthService);
  private readonly staffService = inject(StaffService);
  private readonly employeeService = inject(EmployeeService);

  taskForm!: FormGroup;
  todayDate = new Date().toISOString().split('T')[0];

  activeStaff = signal<StaffMember[]>([]);
  employees = signal<Employee[]>([]);
  interactions = signal<InteractionSummary[]>([]);
  submitting = signal(false);
  serverError = signal<string | null>(null);
  inlineErrors = signal<Record<string, string>>({});
  currentStaffId = signal<string>('');
  private applyingContext = false;

  get editMode(): boolean {
    return !!this.editTask;
  }

  descriptionLength = signal(0);

  remainingChars = computed(() => {
    return 2000 - this.descriptionLength();
  });

  showContextBanner = computed(() => {
    const ctx = this.interactionContext;
    if (!ctx) return false;
    const hasValidEmployeeId = !!ctx.employeeId && this.isValidUuid(ctx.employeeId);
    const hasValidInteractionId = !!ctx.interactionId && this.isValidUuid(ctx.interactionId);
    return hasValidEmployeeId && hasValidInteractionId && !!ctx.interactionType && !!ctx.interactionDate;
  });

  contextBannerText = computed(() => {
    const ctx = this.interactionContext;
    if (!ctx || !ctx.interactionType || !ctx.interactionDate) return '';
    return `Creating task from ${ctx.interactionType} interaction on ${ctx.interactionDate}`;
  });

  ngOnInit(): void {
    this.initForm();
    this.loadStaff();
    this.loadEmployees();
    this.resolveCurrentStaff();

    if (this.editTask) {
      this.populateForEdit();
    } else if (this.interactionContext) {
      this.applyInteractionContext(this.interactionContext);
    }
  }

  private isValidUuid(value: string): boolean {
    return this.UUID_REGEX.test(value);
  }

  private applyInteractionContext(context: InteractionContext): void {
    const hasValidEmployeeId = !!context.employeeId && this.isValidUuid(context.employeeId);
    const hasValidInteractionId = !!context.interactionId && this.isValidUuid(context.interactionId);

    if (hasValidEmployeeId && hasValidInteractionId) {
      // Full pre-population: both UUIDs valid
      this.applyingContext = true;
      // Set individualId — if employee doesn't exist in the list, field stays empty (Req 5.6)
      this.taskForm.get('individualId')?.setValue(context.employeeId);
      // Enable link interaction toggle (Req 3.2)
      this.taskForm.get('linkInteraction')?.setValue(true);
      this.applyingContext = false;
      // Load interactions and pre-select the matching one (Req 3.1, 3.3)
      this.loadInteractionsAndPreselect(context.employeeId, context.interactionId);
    } else if (hasValidEmployeeId && !hasValidInteractionId) {
      // Partial pre-population: only employeeId valid → set individual, leave toggle off
      this.taskForm.get('individualId')?.setValue(context.employeeId);
      // linkInteraction stays false, no interaction pre-selected
    } else {
      // Only interactionId valid (no employeeId) OR both malformed → ignore entirely
      // Behave as standard form with no pre-population
    }
  }

  private loadInteractionsAndPreselect(employeeId: string, interactionId: string): void {
    this.taskService.getInteractionsForIndividual(employeeId).subscribe({
      next: (interactions) => {
        this.interactions.set(interactions);
        // Pre-select matching interaction if it exists in the list (Req 3.3)
        // If interaction not found, leave dropdown unselected (Req 5.7)
        const match = interactions.find(i => i.id === interactionId);
        if (match) {
          this.taskForm.get('interactionId')?.setValue(interactionId);
        }
      }
    });
  }

  private initForm(): void {
    this.taskForm = this.fb.group({
      description: ['', [Validators.required, this.notBlankValidator]],
      assigneeId: ['', [Validators.required]],
      individualId: ['', [Validators.required]],
      dueDate: [''],
      linkInteraction: [false],
      interactionId: ['']
    });

    // Track description length for character count
    this.taskForm.get('description')?.valueChanges.subscribe(value => {
      this.descriptionLength.set((value ?? '').length);
    });

    // Watch individual changes to load interactions (Req 3.5, 4.4)
    this.taskForm.get('individualId')?.valueChanges.subscribe(individualId => {
      if (this.applyingContext) return;
      // Always clear interaction selection when individual changes
      this.taskForm.get('interactionId')?.setValue('', { emitEvent: false });
      if (individualId && this.taskForm.get('linkInteraction')?.value) {
        // Reload interactions for new employee, keep toggle in current state
        this.loadInteractions(individualId);
      } else {
        this.interactions.set([]);
      }
    });

    // Watch toggle changes (Req 4.3, 4.5)
    this.taskForm.get('linkInteraction')?.valueChanges.subscribe(linked => {
      if (this.applyingContext) return;
      const individualId = this.taskForm.get('individualId')?.value;
      if (linked && individualId) {
        this.loadInteractions(individualId);
      } else {
        // Toggle off: clear interactionId immediately and reset interactions
        this.interactions.set([]);
        this.taskForm.get('interactionId')?.setValue('');
      }
    });
  }

  private notBlankValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (value && value.trim().length === 0) {
      return { notBlank: true };
    }
    return null;
  }

  private loadStaff(): void {
    this.staffService.findAll().subscribe({
      next: (staff) => {
        this.activeStaff.set(staff.filter(s => s.active));
      }
    });
  }

  private loadEmployees(): void {
    this.employeeService.findAll().subscribe({
      next: (employees) => {
        this.employees.set(employees);
      }
    });
  }

  private resolveCurrentStaff(): void {
    const email = this.authService.userEmail();
    this.staffService.findAll().subscribe({
      next: (staff) => {
        const current = staff.find(s => s.email === email);
        if (current) {
          this.currentStaffId.set(current.id);
          // Default assignee to current user on create
          if (!this.editTask && !this.taskForm.get('assigneeId')?.value) {
            this.taskForm.get('assigneeId')?.setValue(current.id);
          }
        }
      }
    });
  }

  private loadInteractions(individualId: string): void {
    this.taskService.getInteractionsForIndividual(individualId).subscribe({
      next: (interactions) => {
        this.interactions.set(interactions);
      }
    });
  }

  private populateForEdit(): void {
    if (!this.editTask) return;

    this.taskForm.patchValue({
      description: this.editTask.description,
      assigneeId: this.editTask.assigneeId,
      individualId: this.editTask.individualId,
      dueDate: this.editTask.dueDate ?? '',
      linkInteraction: !!this.editTask.interactionId,
      interactionId: this.editTask.interactionId ?? ''
    });

    if (this.editTask.interactionId && this.editTask.individualId) {
      this.loadInteractions(this.editTask.individualId);
    }
  }

  showFieldError(fieldName: string): boolean {
    const control = this.taskForm.get(fieldName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  getFieldError(fieldName: string): string {
    const control = this.taskForm.get(fieldName);
    if (!control || !control.errors) return '';

    if (control.errors['required']) return `${this.getFieldLabel(fieldName)} is required`;
    if (control.errors['notBlank']) return 'Description must contain text (not only whitespace)';
    if (control.errors['maxlength']) return `Maximum ${control.errors['maxlength'].requiredLength} characters`;
    return '';
  }

  getInlineError(fieldName: string): string {
    return this.inlineErrors()[fieldName] ?? '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      description: 'Description',
      assigneeId: 'Assigned To',
      individualId: 'Individual',
      dueDate: 'Due Date',
      interactionId: 'Interaction'
    };
    return labels[fieldName] || fieldName;
  }

  onSubmit(): void {
    // Mark all as touched so errors show
    this.taskForm.markAllAsTouched();

    // Block submission when required fields are empty
    if (this.taskForm.invalid) {
      return;
    }

    // Additional whitespace-only check
    const description = this.taskForm.get('description')?.value?.trim();
    if (!description) {
      return;
    }

    this.submitting.set(true);
    this.serverError.set(null);
    this.inlineErrors.set({});

    const formValue = this.taskForm.value;

    if (this.editMode && this.editTask) {
      const request: UpdateTaskRequest = {
        individualId: formValue.individualId,
        assigneeId: formValue.assigneeId,
        description: formValue.description.trim(),
        dueDate: formValue.dueDate || null
      };
      if (formValue.linkInteraction && formValue.interactionId) {
        request.interactionId = formValue.interactionId;
      }

      this.taskService.updateTask(this.editTask.id, request).subscribe({
        next: () => {
          this.submitting.set(false);
          this.taskUpdated.emit();
          this.closed.emit();
        },
        error: (err: HttpErrorResponse) => this.handleError(err)
      });
    } else {
      const request: CreateTaskRequest = {
        individualId: formValue.individualId,
        assigneeId: formValue.assigneeId,
        description: formValue.description.trim(),
      };
      if (formValue.dueDate) {
        request.dueDate = formValue.dueDate;
      }
      if (formValue.linkInteraction && formValue.interactionId) {
        request.interactionId = formValue.interactionId;
      }

      this.taskService.createTask(request).subscribe({
        next: () => {
          this.submitting.set(false);
          this.taskCreated.emit();
          this.closed.emit();
        },
        error: (err: HttpErrorResponse) => this.handleError(err)
      });
    }
  }

  private handleError(err: HttpErrorResponse): void {
    this.submitting.set(false);

    if (err.status === 0 || !err.status) {
      // Network error — no response received from server
      this.serverError.set('Unable to connect. Please check your network and try again.');
    } else if (err.status >= 500) {
      // Server error — preserve form data for retry
      this.serverError.set('Server error. Please try again.');
    } else if (err.status === 400 && err.error) {
      // Validation errors — show inline
      if (typeof err.error === 'object' && err.error.errors && Array.isArray(err.error.errors) && err.error.errors.length > 0) {
        const errors: Record<string, string> = {};
        for (const e of err.error.errors) {
          if (e.field) {
            errors[e.field] = e.message || e.defaultMessage;
          }
        }
        this.inlineErrors.set(errors);
      } else if (typeof err.error === 'object' && err.error.fieldErrors && typeof err.error.fieldErrors === 'object') {
        // Flat fieldErrors object format: { fieldErrors: { description: "required" } }
        this.inlineErrors.set(err.error.fieldErrors);
      } else if (typeof err.error === 'object' && err.error.message) {
        this.serverError.set(err.error.message);
      } else if (typeof err.error === 'string') {
        this.serverError.set(err.error);
      } else {
        this.serverError.set('Validation failed. Please check your input.');
      }
    } else {
      this.serverError.set(err.error?.message || 'An unexpected error occurred.');
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    this.onClose();
  }

  onClose(): void {
    this.closed.emit();
  }
}
