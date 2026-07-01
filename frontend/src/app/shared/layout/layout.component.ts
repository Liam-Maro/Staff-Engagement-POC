import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../auth/services/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app-layout">
      <aside class="sidebar">
        <div class="sidebar-brand">
          <h1>Staff Engagement</h1>
          <span>Management System</span>
        </div>

        <ul class="sidebar-nav">
          <li><a routerLink="/dashboard" routerLinkActive="active">Dashboard</a></li>
          <li><a routerLink="/employees" routerLinkActive="active">Employees</a></li>
          <li><a routerLink="/tasks" routerLinkActive="active">My Tasks</a></li>
          <li><a routerLink="/skills" routerLinkActive="active">Skills Register</a></li>
          <li><a routerLink="/interactions" routerLinkActive="active">Interactions</a></li>
          @if (authService.userRole().toLowerCase() === 'admin') {
            <li><a routerLink="/admin/staff" routerLinkActive="active">Manage Staff</a></li>
          }
        </ul>

        <div class="sidebar-footer">
          <div class="user-info">
            <strong>{{ authService.userEmail() }}</strong>
            {{ authService.userRole() }}
          </div>
          <button (click)="onLogout()">Sign out</button>
        </div>
      </aside>

      <main class="main-content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`:host { display: block; height: 100%; }`]
})
export class LayoutComponent {
  authService = inject(AuthService);

  onLogout(): void {
    this.authService.logout();
  }
}
