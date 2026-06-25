import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { StaffService } from '../../services/staff.service';

@Component({
  selector: 'app-staff-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './staff-create.component.html',
  styleUrls: ['./staff-create.component.css']
})
export class StaffCreateComponent {

  form: FormGroup;
  errorMessage = signal('');
  isLoading = signal(false);

  constructor(
    private fb: FormBuilder,
    private staffService: StaffService,
    private router: Router
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      role: ['STAFF', [Validators.required]]
    });
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
