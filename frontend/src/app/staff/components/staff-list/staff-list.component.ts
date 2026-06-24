import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { StaffService } from '../../services/staff.service';
import { StaffMember } from '../../models/staff.models';
import { ConfirmModalComponent } from '../../../shared/components/confirm-modal/confirm-modal.component';

@Component({
  selector: 'app-staff-list',
  standalone: true,
  imports: [CommonModule, RouterLink, ConfirmModalComponent],
  templateUrl: './staff-list.component.html',
  styleUrl: './staff-list.component.css'
})
export class StaffListComponent implements OnInit {

  staffMembers = signal<StaffMember[]>([]);
  isLoading = signal(true);

  // Modal state
  showModal = signal(false);
  modalTitle = '';
  modalMessage = '';
  modalConfirmText = '';
  private pendingAction: (() => void) | null = null;

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
    this.modalTitle = 'Deactivate Staff Member';
    this.modalMessage = 'This will revoke their login access. They will no longer be able to sign in.';
    this.modalConfirmText = 'Deactivate';
    this.pendingAction = () => {
      this.staffService.deactivate(id).subscribe(() => this.loadStaff());
    };
    this.showModal.set(true);
  }

  onActivate(id: string, role: string): void {
    this.modalTitle = 'Reactivate Staff Member';
    this.modalMessage = 'This will restore their login access. They will be able to sign in again.';
    this.modalConfirmText = 'Activate';
    this.pendingAction = () => {
      this.staffService.update(id, { role: role as 'STAFF' | 'ADMIN', active: true }).subscribe(() => this.loadStaff());
    };
    this.showModal.set(true);
  }

  onDelete(id: string): void {
    this.modalTitle = 'Delete Staff Member';
    this.modalMessage = 'This will permanently remove this staff member. This action cannot be undone.';
    this.modalConfirmText = 'Delete';
    this.pendingAction = () => {
      this.staffService.delete(id).subscribe(() => this.loadStaff());
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
