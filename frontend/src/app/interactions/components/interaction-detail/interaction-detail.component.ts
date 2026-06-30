import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { InteractionService } from '../../services/interaction.service';
import { InteractionResponse } from '../../models/interaction.model';
import { InteractionContext } from '../../../tasks/models/task.model';
import { TaskFormComponent } from '../../../tasks/components/task-form/task-form.component';
import { ToastNotificationComponent } from '../../../shared/components/toast-notification/toast-notification.component';

@Component({
  selector: 'app-interaction-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, TaskFormComponent, ToastNotificationComponent],
  templateUrl: './interaction-detail.component.html',
  styleUrl: './interaction-detail.component.css'
})
export class InteractionDetailComponent implements OnInit {

  interaction = signal<InteractionResponse | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  isNotFound = signal(false);
  showTaskFormModal = signal(false);
  taskFormContext = signal<InteractionContext | null>(null);
  toastMessage = signal<string | null>(null);
  toastType = signal<'success' | 'error'>('success');

  private interactionId = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private interactionService: InteractionService
  ) {}

  ngOnInit(): void {
    this.interactionId = this.route.snapshot.paramMap.get('id') ?? '';
    this.loadInteraction();
  }

  loadInteraction(): void {
    if (!this.interactionId) {
      this.isNotFound.set(true);
      this.isLoading.set(false);
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.isNotFound.set(false);

    this.interactionService.getById(this.interactionId).subscribe({
      next: (data) => {
        this.interaction.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 404) {
          this.isNotFound.set(true);
        } else {
          this.errorMessage.set('Failed to load interaction. Please try again.');
        }
      }
    });
  }

  formatType(type: string): string {
    return type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  createFollowUpTask(): void {
    const interaction = this.interaction();
    if (interaction) {
      this.taskFormContext.set({
        interactionId: interaction.id,
        employeeId: interaction.employeeId,
        interactionType: interaction.type,
        interactionDate: interaction.occurredAt
      });
      this.showTaskFormModal.set(true);
    }
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
