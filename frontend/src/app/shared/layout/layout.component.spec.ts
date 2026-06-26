import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideLocationMocks } from '@angular/common/testing';
import { Component, signal } from '@angular/core';
import { LayoutComponent } from './layout.component';
import { AuthService } from '../../auth/services/auth.service';

@Component({ standalone: true, template: '<p>Tasks Page</p>' })
class FakeMyTasksPageComponent {}

@Component({ standalone: true, template: '<p>Dashboard</p>' })
class FakeDashboardComponent {}

describe('LayoutComponent', () => {
  let fixture: ComponentFixture<LayoutComponent>;
  let component: LayoutComponent;
  let mockAuthService: any;

  beforeEach(async () => {
    mockAuthService = {
      userEmail: signal('test@example.com'),
      userRole: signal('USER'),
      getAccessToken: () => 'fake-token',
      logout: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [LayoutComponent],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        provideRouter([
          { path: 'dashboard', component: FakeDashboardComponent },
          { path: 'tasks', component: FakeMyTasksPageComponent },
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
        ]),
        provideLocationMocks(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render "My Tasks" navigation link for authenticated users', () => {
    const links = fixture.nativeElement.querySelectorAll('.sidebar-nav a');
    const myTasksLink = Array.from(links).find(
      (link: any) => link.textContent.trim() === 'My Tasks'
    ) as HTMLAnchorElement | undefined;

    expect(myTasksLink).toBeDefined();
    expect(myTasksLink!.textContent!.trim()).toBe('My Tasks');
  });

  it('should have routerLink pointing to /tasks on the "My Tasks" link', () => {
    const links = fixture.nativeElement.querySelectorAll('.sidebar-nav a');
    const myTasksLink = Array.from(links).find(
      (link: any) => link.textContent.trim() === 'My Tasks'
    ) as HTMLAnchorElement | undefined;

    expect(myTasksLink).toBeDefined();
    expect(myTasksLink!.getAttribute('href')).toBe('/tasks');
  });

  it('should apply active class on "My Tasks" link when route is /tasks', async () => {
    const router = TestBed.inject(Router);
    await router.navigate(['/tasks']);
    fixture.detectChanges();

    const links = fixture.nativeElement.querySelectorAll('.sidebar-nav a');
    const myTasksLink = Array.from(links).find(
      (link: any) => link.textContent.trim() === 'My Tasks'
    ) as HTMLAnchorElement | undefined;

    expect(myTasksLink).toBeDefined();
    expect(myTasksLink!.classList.contains('active')).toBe(true);
  });

  it('should NOT apply active class on "My Tasks" link when route is /dashboard', async () => {
    const router = TestBed.inject(Router);
    await router.navigate(['/dashboard']);
    fixture.detectChanges();

    const links = fixture.nativeElement.querySelectorAll('.sidebar-nav a');
    const myTasksLink = Array.from(links).find(
      (link: any) => link.textContent.trim() === 'My Tasks'
    ) as HTMLAnchorElement | undefined;

    expect(myTasksLink).toBeDefined();
    expect(myTasksLink!.classList.contains('active')).toBe(false);
  });
});
