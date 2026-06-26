import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { InteractionService } from '../../services/interaction.service';
import { EmployeeService } from '../../../employees/services/employee.service';
import { Employee } from '../../../employees/models/employee.models';
import {
  INTERACTION_TYPES,
  InteractionType,
  CreateInteractionRequest,
  UpdateInteractionRequest,
  InteractionResponse
} from '../../models/interaction.model';

@Component({
  selector: 'app-interaction-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './interaction-form.component.html',
  styleUrl: './interaction-form.component.css'
})
export class InteractionFormComponent implements OnInit {

  form!: FormGroup;
  isEditMode = signal(false);
  interactionId = signal<string | null>(null);
  isLoading = signal(false);
  isLoadingData = signal(false);
  errorMessage = signal('');
  fieldErrors = signal<Record<string, string>>({});
  employees = signal<Employee[]>([]);
  filteredEmployees = signal<Employee[]>([]);
  employeeSearchTerm = signal('');
  interactionTypes = INTERACTION_TYPES;

  // Hardcoded staffId for now (would come from auth context in production)
  private readonly currentStaffId = '00000000-0000-0000-0000-000000000001';

  constructor(
    private fb: FormBuilder,
    private interactionService: InteractionService,
    private employeeService: EmployeeService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadEmployees();

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.interactionId.set(id);
      this.loadInteraction(id);
    }
  }

  private initForm(): void {
    this.form = this.fb.group({
      employeeId: ['', [Validators.required]],
      type: ['', [Validators.required]],
      occurredAt: ['', [Validators.required, this.notInFutureValidator]],
      notes: ['', [Validators.maxLength(5000)]]
    });
  }

  private notInFutureValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    const selectedDate = new Date(control.value);
    const now = new Date();
    if (selectedDate > now) {
      return { futureDate: true };
    }
    return null;
  }

  private loadEmployees(): void {
    this.employeeService.findAll().subscribe({
      next: (employees) => {
        this.employees.set(employees);
        this.filteredEmployees.set(employees);
      },
      error: () => {
        // Silently handle — employees dropdown will be empty
      }
    });
  }

  private loadInteraction(id: string): void {
    this.isLoadingData.set(true);
    this.interactionService.getById(id).subscribe({
      next: (interaction: InteractionResponse) => {
        this.isLoadingData.set(false);
        this.populateForm(interaction);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoadingData.set(false);
        if (err.status === 404) {
          this.errorMessage.set('Interaction not found.');
        } else {
          this.errorMessage.set('Failed to load interaction data.');
        }
      }
    });
  }

  private populateForm(interaction: InteractionResponse): void {
    // Format occurredAt for datetime-local input
    const occurredAtLocal = this.toDateTimeLocal(interaction.occurredAt);

    this.form.patchValue({
      employeeId: interaction.employeeId,
      type: interaction.type,
      occurredAt: occurredAtLocal,
      notes: interaction.notes || ''
    });

    // Disable employeeId in edit mode
    this.form.get('employeeId')?.disable();
  }

  private toDateTimeLocal(isoString: string): string {
    if (!isoString) return '';
    const date = new Date(isoString);
    // Format as YYYY-MM-DDTHH:mm for datetime-local input
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  onEmployeeSearch(event: Event): void {
    const term = (event.target as HTMLInputElement).value.toLowerCase();
    this.employeeSearchTerm.set(term);
    const all = this.employees();
    if (!term) {
      this.filteredEmployees.set(all);
    } else {
      this.filteredEmployees.set(
        all.filter(e =>
          `${e.firstName} ${e.lastName}`.toLowerCase().includes(term) ||
          e.email.toLowerCase().includes(term)
        )
      );
    }
  }

  getEmployeeName(employeeId: string): string {
    const emp = this.employees().find(e => e.id === employeeId);
    return emp ? `${emp.firstName} ${emp.lastName}` : '';
  }

  formatType(type: InteractionType): string {
    return type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  onSubmit(): void {
    if (this.form.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set('');
    this.fieldErrors.set({});

    if (this.isEditMode()) {
      this.submitUpdate();
    } else {
      this.submitCreate();
    }
  }

  private submitCreate(): void {
    const formValue = this.form.getRawValue();
    const request: CreateInteractionRequest = {
      employeeId: formValue.employeeId,
      staffId: this.currentStaffId,
      type: formValue.type as InteractionType,
      occurredAt: new Date(formValue.occurredAt).toISOString(),
      notes: formValue.notes || undefined
    };

    this.interactionService.create(request).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/interactions']);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.handleSubmitError(err);
      }
    });
  }

  private submitUpdate(): void {
    const formValue = this.form.getRawValue();
    const request: UpdateInteractionRequest = {
      type: formValue.type as InteractionType,
      occurredAt: new Date(formValue.occurredAt).toISOString(),
      notes: formValue.notes || undefined
    };

    this.interactionService.update(this.interactionId()!, request).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/interactions']);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.handleSubmitError(err);
      }
    });
  }

  private handleSubmitError(err: HttpErrorResponse): void {
    if (err.status === 400) {
      // Parse validation errors from server
      const body = err.error;
      if (body && typeof body === 'object') {
        // Handle field-level validation errors
        if (body.errors && Array.isArray(body.errors)) {
          const errors: Record<string, string> = {};
          for (const fieldError of body.errors) {
            errors[fieldError.field] = fieldError.message || fieldError.defaultMessage;
          }
          this.fieldErrors.set(errors);
        } else if (body.fieldErrors && typeof body.fieldErrors === 'object') {
          this.fieldErrors.set(body.fieldErrors);
        } else if (body.message) {
          this.errorMessage.set(body.message);
        } else {
          this.errorMessage.set('Validation failed. Please check the form fields.');
        }
      } else {
        this.errorMessage.set('Validation failed. Please check the form fields.');
      }
    } else if (err.status >= 500) {
      this.errorMessage.set('A server error occurred. Please try again.');
    } else {
      this.errorMessage.set(err.error?.message || 'An unexpected error occurred.');
    }
  }

  onRetry(): void {
    this.onSubmit();
  }

  onCancel(): void {
    this.router.navigate(['/interactions']);
  }

  getFieldError(fieldName: string): string {
    const serverErrors = this.fieldErrors();
    if (serverErrors[fieldName]) {
      return serverErrors[fieldName];
    }

    const control = this.form.get(fieldName);
    if (control && control.touched && control.invalid) {
      if (control.errors?.['required']) return `${this.getFieldLabel(fieldName)} is required.`;
      if (control.errors?.['maxlength']) return `Maximum ${control.errors['maxlength'].requiredLength} characters allowed.`;
      if (control.errors?.['futureDate']) return 'Date cannot be in the future.';
    }
    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      employeeId: 'Employee',
      type: 'Interaction type',
      occurredAt: 'Date and time',
      notes: 'Notes'
    };
    return labels[fieldName] || fieldName;
  }
}
