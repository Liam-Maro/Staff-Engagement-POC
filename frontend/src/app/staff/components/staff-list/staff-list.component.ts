import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { StaffService } from '../../services/staff.service';
import { StaffMember } from '../../models/staff.models';

@Component({
  selector: 'app-staff-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './staff-list.component.html',
  styleUrl: './staff-list.component.css'
})
export class StaffListComponent implements OnInit {

  staffMembers = signal<StaffMember[]>([]);
  isLoading = signal(true);

  constructor(private staffService: StaffService) {}

  ngOnInit(): void {
    this.loadStaff();
  }

  loadStaff(): void {
    this.isLoading.set(true);
    this.staffService.findAll().subscribe({
      next: (members) => {
        this.staffMembers.set(members);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  onDeactivate(id: string): void {
    if (confirm('Are you sure you want to deactivate this staff member?')) {
      this.staffService.deactivate(id).subscribe(() => this.loadStaff());
    }
  }

  onDelete(id: string): void {
    if (confirm('Are you sure you want to permanently delete this staff member?')) {
      this.staffService.delete(id).subscribe(() => this.loadStaff());
    }
  }
}
