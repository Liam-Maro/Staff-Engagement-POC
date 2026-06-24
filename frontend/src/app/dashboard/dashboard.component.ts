import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="dashboard">
      <header>
        <h1>Staff Engagement</h1>
        <nav>
          <a routerLink="/employees" class="nav-link">Employees</a>
          @if (authService.userRole() === 'ADMIN') {
            <a routerLink="/admin/staff" class="nav-link">Manage Staff</a>
          }
          <button (click)="onLogout()">Logout</button>
        </nav>
      </header>
      <main>
        <p>Welcome, {{ authService.userEmail() }}! You are logged in as <strong>{{ authService.userRole() }}</strong>.</p>
      </main>
    </div>
  `,
  styles: [`
    .dashboard { padding: 2rem; }
    header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; border-bottom: 1px solid #eee; padding-bottom: 1rem; }
    h1 { margin: 0; font-size: 1.5rem; }
    nav { display: flex; align-items: center; gap: 1rem; }
    .nav-link { color: #1976d2; text-decoration: none; font-weight: 500; }
    .nav-link:hover { text-decoration: underline; }
    button { padding: 0.5rem 1rem; background: #d32f2f; color: white; border: none; border-radius: 4px; cursor: pointer; }
    button:hover { background: #b71c1c; }
  `]
})
export class DashboardComponent {
  constructor(public authService: AuthService) {}

  onLogout(): void {
    this.authService.logout();
  }
}
