import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { StaffService } from '../../services/staff.service';
import { EmployeeService } from '../../../employees/services/employee.service';
import { Employee } from '../../../employees/models/employee.models';

@Component({
  selector: 'app-staff-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './staff-create.component.html',
  styleUrl: './staff-create.component.css'
})
export class StaffCreateComponent implements OnInit {

  form: FormGroup;
  employees = signal<Employee[]>([]);
  errorMessage = signal('');
  isLoading = signal(false);

  constructor(
    private fb: FormBuilder,
    private staffService: StaffService,
    private employeeService: EmployeeService,
    private router: Router
  ) {
    this.form = this.fb.group({
      employeeId: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      role: ['STAFF', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.employeeService.findAll().subscribe({
      next: (data) => this.employees.set(data),
      error: () => this.errorMessage.set('Failed to load employees.')
    });
  }

  onEmployeeSelect(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const employeeId = select.value;
    const employee = this.employees().find(e => e.id === employeeId);
    if (employee) {
      this.form.patchValue({ email: employee.email });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.staffService.create(this.form.value).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/admin/staff']);
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 409) {
          this.errorMessage.set('A staff member with this email already exists.');
        } else {
          this.errorMessage.set('Failed to create staff member. Please try again.');
        }
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/admin/staff']);
  }
}
