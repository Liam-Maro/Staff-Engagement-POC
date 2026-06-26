import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { InteractionService } from '../../services/interaction.service';
import { InteractionResponse } from '../../models/interaction.model';

@Component({
  selector: 'app-interaction-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './interaction-detail.component.html',
  styleUrl: './interaction-detail.component.css'
})
export class InteractionDetailComponent implements OnInit {

  interaction = signal<InteractionResponse | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  isNotFound = signal(false);

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
      this.router.navigate(['/tasks/create'], {
        queryParams: {
          interactionId: interaction.id,
          employeeId: interaction.employeeId,
          staffId: interaction.staffId
        }
      });
    }
  }
}
