import { Routes } from '@angular/router';
import { authGuard } from './auth/guards/auth.guard';
import { adminGuard } from './auth/guards/admin.guard';
import { LayoutComponent } from './shared/layout/layout.component';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/components/login/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'employees', loadComponent: () => import('./employees/components/employee-list/employee-list.component').then(m => m.EmployeeListComponent) },
      { path: 'employees/create', loadComponent: () => import('./employees/components/employee-create/employee-create.component').then(m => m.EmployeeCreateComponent) },
      { path: 'skills', loadComponent: () => import('./skills/components/skill-search/skill-search.component').then(m => m.SkillSearchComponent) },
      { path: 'admin/staff', loadComponent: () => import('./staff/components/staff-list/staff-list.component').then(m => m.StaffListComponent), canActivate: [adminGuard] },
      { path: 'admin/staff/create', loadComponent: () => import('./staff/components/staff-create/staff-create.component').then(m => m.StaffCreateComponent), canActivate: [adminGuard] },
      { path: 'portfolio/:employeeId', loadComponent: () => import('./portfolio/components/portfolio-view/portfolio-view.component').then(m => m.PortfolioViewComponent) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
