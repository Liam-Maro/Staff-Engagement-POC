import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideLocationMocks } from '@angular/common/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { AuthService } from './auth/services/auth.service';
import { routes } from './app.routes';

describe('App Routes', () => {
  describe('/tasks route definition', () => {
    it('should define a /tasks route that lazy-loads MyTasksPageComponent', () => {
      // Find the authenticated layout route (path: '')
      const layoutRoute = routes.find(r => r.path === '' && r.children);
      expect(layoutRoute).toBeDefined();
      expect(layoutRoute!.children).toBeDefined();

      // Find the /tasks child route
      const tasksRoute = layoutRoute!.children!.find(r => r.path === 'tasks');
      expect(tasksRoute).toBeDefined();
      expect(tasksRoute!.loadComponent).toBeDefined();
    });

    it('should lazy-load MyTasksPageComponent via dynamic import', async () => {
      const layoutRoute = routes.find(r => r.path === '' && r.children);
      const tasksRoute = layoutRoute!.children!.find(r => r.path === 'tasks');

      // The loadComponent function uses .then(m => m.MyTasksPageComponent),
      // so the resolved value is the component class itself
      const loadedComponent = await (tasksRoute!.loadComponent as Function)();
      expect(loadedComponent).toBeDefined();
      expect(loadedComponent.name).toBe('MyTasksPageComponent');
    });

    it('should have the auth guard on the parent layout route protecting /tasks', () => {
      const layoutRoute = routes.find(r => r.path === '' && r.children);
      expect(layoutRoute).toBeDefined();
      expect(layoutRoute!.canActivate).toBeDefined();
      expect(layoutRoute!.canActivate!.length).toBeGreaterThan(0);
    });
  });

  describe('auth guard redirects unauthenticated users', () => {
    let router: Router;
    let mockAuthService: any;

    beforeEach(async () => {
      mockAuthService = {
        getAccessToken: () => null, // unauthenticated
        userEmail: signal(''),
        userRole: signal(''),
        logout: vi.fn(),
      };

      await TestBed.configureTestingModule({
        providers: [
          provideRouter(routes),
          provideLocationMocks(),
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: AuthService, useValue: mockAuthService },
        ],
      }).compileComponents();

      router = TestBed.inject(Router);
      await router.initialNavigation();
    });

    it('should block unauthenticated users from accessing /tasks', async () => {
      const navigated = await router.navigate(['/tasks']);

      // The auth guard returns false and prevents navigation to /tasks
      expect(router.url).not.toBe('/tasks');
    });

    it('should allow authenticated users to access /tasks', async () => {
      // Override to be authenticated
      mockAuthService.getAccessToken = () => 'valid-token';

      await router.navigate(['/tasks']);

      expect(router.url).toBe('/tasks');
    });
  });
});
