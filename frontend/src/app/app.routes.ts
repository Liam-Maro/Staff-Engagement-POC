import { Routes } from '@angular/router';
import { authGuard } from './auth/guards/auth.guard';
import { adminGuard } from './auth/guards/admin.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/components/login/login.component').then(m => m.LoginComponent) },
  { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent), canActivate: [authGuard] },
  {
    path: 'admin/staff',
    canActivate: [authGuard, adminGuard],
    children: [
      { path: '', loadComponent: () => import('./staff/components/staff-list/staff-list.component').then(m => m.StaffListComponent) },
      { path: 'create', loadComponent: () => import('./staff/components/staff-create/staff-create.component').then(m => m.StaffCreateComponent) }
    ]
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];
