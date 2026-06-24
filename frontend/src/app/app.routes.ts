import { Routes } from '@angular/router';
import { authGuard } from './auth/guards/auth.guard';
import { adminGuard } from './auth/guards/admin.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/components/login/login.component').then(m => m.LoginComponent) },
  { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent), canActivate: [authGuard] },
  {
    path: 'employees',
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./employees/components/employee-list/employee-list.component').then(m => m.EmployeeListComponent) },
      { path: 'create', loadComponent: () => import('./employees/components/employee-create/employee-create.component').then(m => m.EmployeeCreateComponent) }
    ]
  },
  {
    path: 'admin/staff',
    canActivate: [authGuard, adminGuard],
    children: [
      { path: '', loadComponent: () => import('./staff/components/staff-list/staff-list.component').then(m => m.StaffListComponent) },
      { path: 'create', loadComponent: () => import('./staff/components/staff-create/staff-create.component').then(m => m.StaffCreateComponent) }
    ]
  },
  { path: 'skills', loadComponent: () => import('./skills/components/skill-search/skill-search.component').then(m => m.SkillSearchComponent), canActivate: [authGuard] },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];
