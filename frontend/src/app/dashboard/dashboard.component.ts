import { Component } from '@angular/core';
import { AuthService } from '../auth/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  template: `
    <div class="page-header">
      <h2>Dashboard</h2>
    </div>
    <div class="dashboard-welcome">
      <div class="welcome-card">
        <h3>Welcome back, {{ authService.userEmail() }}</h3>
        <p class="text-muted">You are signed in as <strong>{{ authService.userRole() }}</strong></p>
      </div>
    </div>
  `,
  styles: [`
    .welcome-card {
      background: var(--color-bg-white);
      border: 1px solid var(--color-border);
      border-radius: var(--radius);
      padding: 2rem;
      box-shadow: var(--shadow-sm);
    }
    h3 { margin-bottom: 0.5rem; font-size: 1.2rem; }
  `]
})
export class DashboardComponent {
  constructor(public authService: AuthService) {}
}
