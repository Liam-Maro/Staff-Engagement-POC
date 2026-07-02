/**
 * Bug Condition Exploration Test — Admin role casing mismatch blocks admin routes
 *
 * **Validates: Requirements 1.1, 1.2**
 *
 * This test encodes the EXPECTED behavior: adminGuard should return true for any
 * case-variant of "admin". On unfixed code, tests 1-3 will FAIL because the guard
 * uses strict equality `=== 'Admin'` — only title-case passes.
 *
 * Counterexamples demonstrate the bug: role "ADMIN" (backend value) is rejected
 * because `"ADMIN" === 'Admin'` evaluates to false.
 */
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { EnvironmentInjector, runInInjectionContext, signal } from '@angular/core';
import { adminGuard } from './admin.guard';
import { AuthService } from '../services/auth.service';
import * as fc from 'fast-check';

describe('adminGuard — Bug Condition Exploration (case-sensitivity mismatch)', () => {
  let mockRouter: { navigate: ReturnType<typeof vi.fn> };
  let injector: EnvironmentInjector;
  let userRoleSignal: ReturnType<typeof signal<string>>;

  function setupGuard(role: string) {
    userRoleSignal = signal(role);

    mockRouter = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: { userRole: userRoleSignal },
        },
        {
          provide: Router,
          useValue: mockRouter,
        },
      ],
    });

    injector = TestBed.inject(EnvironmentInjector);
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('should return true for role "ADMIN" (actual backend value)', () => {
    setupGuard('ADMIN');

    const result = runInInjectionContext(injector, () => adminGuard({} as any, {} as any));

    expect(result).toBe(true);
    expect(mockRouter.navigate).not.toHaveBeenCalled();
  });

  it('should return true for role "admin" (lowercase)', () => {
    setupGuard('admin');

    const result = runInInjectionContext(injector, () => adminGuard({} as any, {} as any));

    expect(result).toBe(true);
    expect(mockRouter.navigate).not.toHaveBeenCalled();
  });

  it('should return true for role "AdMiN" (mixed-case)', () => {
    setupGuard('AdMiN');

    const result = runInInjectionContext(injector, () => adminGuard({} as any, {} as any));

    expect(result).toBe(true);
    expect(mockRouter.navigate).not.toHaveBeenCalled();
  });

  it('should return true for role "Admin" (title-case — only case that passes on unfixed code)', () => {
    setupGuard('Admin');

    const result = runInInjectionContext(injector, () => adminGuard({} as any, {} as any));

    expect(result).toBe(true);
    expect(mockRouter.navigate).not.toHaveBeenCalled();
  });
});

/**
 * Preservation Property Tests — Non-admin users blocked from admin routes
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4**
 *
 * Property: for all role strings that are NOT case-insensitively equal to "admin",
 * adminGuard returns false and triggers router.navigate(['/dashboard']).
 *
 * These tests MUST PASS on unfixed code — they confirm baseline behavior that
 * the fix must preserve.
 */
describe('adminGuard — Preservation (non-admin users blocked)', () => {
  let mockRouter: { navigate: ReturnType<typeof vi.fn> };
  let injector: EnvironmentInjector;
  let userRoleSignal: ReturnType<typeof signal<string>>;

  function setupGuard(role: string) {
    userRoleSignal = signal(role);

    mockRouter = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: { userRole: userRoleSignal },
        },
        {
          provide: Router,
          useValue: mockRouter,
        },
      ],
    });

    injector = TestBed.inject(EnvironmentInjector);
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  // Observation tests — concrete non-admin roles on unfixed code
  it('should block role "STAFF" and redirect to /dashboard', () => {
    setupGuard('STAFF');

    const result = runInInjectionContext(injector, () => adminGuard({} as any, {} as any));

    expect(result).toBe(false);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should block role "Staff" and redirect to /dashboard', () => {
    setupGuard('Staff');

    const result = runInInjectionContext(injector, () => adminGuard({} as any, {} as any));

    expect(result).toBe(false);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should block role "" (empty) and redirect to /dashboard', () => {
    setupGuard('');

    const result = runInInjectionContext(injector, () => adminGuard({} as any, {} as any));

    expect(result).toBe(false);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should block role "MANAGER" (arbitrary non-admin) and redirect to /dashboard', () => {
    setupGuard('MANAGER');

    const result = runInInjectionContext(injector, () => adminGuard({} as any, {} as any));

    expect(result).toBe(false);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  // Property-based test: any non-admin role string is blocked
  it('property: for all non-admin role strings, guard returns false and redirects to /dashboard', () => {
    /**
     * Generate arbitrary strings that are NOT case-insensitively "admin".
     * Includes random alphanumeric, known roles like STAFF/Staff/staff,
     * empty strings, and other arbitrary values.
     */
    const nonAdminRoleArbitrary = fc.oneof(
      // Known non-admin roles
      fc.constantFrom('STAFF', 'Staff', 'staff', 'MANAGER', 'Manager', 'manager', 'user', 'USER', 'User', ''),
      // Random alphanumeric strings filtered to exclude "admin" variants
      fc.string({ minLength: 0, maxLength: 20 }).filter(s => s.toLowerCase() !== 'admin')
    );

    fc.assert(
      fc.property(nonAdminRoleArbitrary, (role) => {
        TestBed.resetTestingModule();
        setupGuard(role);

        const result = runInInjectionContext(injector, () => adminGuard({} as any, {} as any));

        expect(result).toBe(false);
        expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
      }),
      { numRuns: 100 }
    );
  });
});
