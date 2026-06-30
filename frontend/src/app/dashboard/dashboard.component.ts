import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../auth/services/auth.service';
import { DashboardService, DashboardData } from './services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  data = signal<DashboardData | null>(null);
  isLoading = signal(true);

  constructor(
    public authService: AuthService,
    private dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    const role = this.authService.userRole();
    const staffId = this.authService.staffId();
    this.dashboardService.getDashboardData(role, staffId).subscribe({
      next: (result) => {
        this.data.set(result);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  get totalTasks(): number {
    const d = this.data();
    if (!d) return 0;
    return d.taskBreakdown.open + d.taskBreakdown.inProgress + d.taskBreakdown.completed;
  }

  taskPercentage(count: number): number {
    const total = this.totalTasks;
    if (total === 0) return 0;
    return Math.round((count / total) * 100);
  }

  daysRemaining(dueDate: string): string {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const due = new Date(dueDate);
    due.setHours(0, 0, 0, 0);
    const diff = Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

    if (diff < 0) return `${Math.abs(diff)}d overdue`;
    if (diff === 0) return 'Due today';
    if (diff === 1) return '1d left';
    return `${diff}d left`;
  }

  dueUrgency(dueDate: string): string {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const due = new Date(dueDate);
    due.setHours(0, 0, 0, 0);
    const diff = Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

    if (diff < 0) return 'overdue';
    if (diff <= 2) return 'urgent';
    return 'normal';
  }
}
