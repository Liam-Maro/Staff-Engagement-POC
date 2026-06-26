import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InteractionDetailComponent } from './interaction-detail.component';
import { InteractionService } from '../../services/interaction.service';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { InteractionResponse } from '../../models/interaction.model';

describe('InteractionDetailComponent', () => {
  let component: InteractionDetailComponent;
  let fixture: ComponentFixture<InteractionDetailComponent>;
  let interactionServiceMock: { getById: ReturnType<typeof vi.fn> };
  let routerMock: { navigate: ReturnType<typeof vi.fn> };

  const testId = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';

  const mockInteraction: InteractionResponse = {
    id: testId,
    employeeId: '11111111-1111-1111-1111-111111111111',
    staffId: '22222222-2222-2222-2222-222222222222',
    type: 'CHECK_IN',
    notes: 'Discussed quarterly goals and progress.',
    occurredAt: '2024-06-15T10:30:00',
    createdAt: '2024-06-15T10:35:00',
    updatedAt: '2024-06-15T10:35:00'
  };

  beforeEach(async () => {
    interactionServiceMock = {
      getById: vi.fn()
    };

    routerMock = {
      navigate: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [InteractionDetailComponent],
      providers: [
        { provide: InteractionService, useValue: interactionServiceMock },
        { provide: Router, useValue: routerMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => key === 'id' ? testId : null
              }
            }
          }
        }
      ]
    }).compileComponents();
  });

  describe('Component creation', () => {
    it('should create successfully', () => {
      interactionServiceMock.getById.mockReturnValue(of(mockInteraction));
      fixture = TestBed.createComponent(InteractionDetailComponent);
      component = fixture.componentInstance;
      expect(component).toBeTruthy();
    });
  });

  describe('Loading interaction on init', () => {
    it('should call InteractionService.getById with route param ID on init', () => {
      interactionServiceMock.getById.mockReturnValue(of(mockInteraction));
      fixture = TestBed.createComponent(InteractionDetailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(interactionServiceMock.getById).toHaveBeenCalledWith(testId);
    });
  });

  describe('Loading state', () => {
    it('should show loading state initially before data resolves', () => {
      interactionServiceMock.getById.mockReturnValue(of(mockInteraction));
      fixture = TestBed.createComponent(InteractionDetailComponent);
      component = fixture.componentInstance;

      // Before detectChanges, isLoading should be true (default)
      expect(component.isLoading()).toBe(true);
    });

    it('should render loading indicator in the template', () => {
      // Use a Subject that hasn't emitted yet to keep loading state
      interactionServiceMock.getById.mockReturnValue(new Subject());
      fixture = TestBed.createComponent(InteractionDetailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.loading-container')).toBeTruthy();
      expect(compiled.textContent).toContain('Loading interaction...');
    });
  });

  describe('Successful data display', () => {
    beforeEach(() => {
      interactionServiceMock.getById.mockReturnValue(of(mockInteraction));
      fixture = TestBed.createComponent(InteractionDetailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should set isLoading to false after data loads', () => {
      expect(component.isLoading()).toBe(false);
    });

    it('should store interaction data in signal', () => {
      expect(component.interaction()).toEqual(mockInteraction);
    });

    it('should display the interaction type', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      // formatType('CHECK_IN') → 'CHECK IN' (replaces _ with space, capitalizes word starts - already uppercase)
      expect(compiled.querySelector('.type-badge')?.textContent).toContain('CHECK IN');
    });

    it('should display the employee ID', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain(mockInteraction.employeeId);
    });

    it('should display the staff member ID', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain(mockInteraction.staffId);
    });

    it('should display the notes content', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.notes-content')?.textContent).toContain('Discussed quarterly goals and progress.');
    });

    it('should display "No notes recorded." when notes is null', () => {
      const noNotesInteraction = { ...mockInteraction, notes: null };
      interactionServiceMock.getById.mockReturnValue(of(noNotesInteraction));
      fixture = TestBed.createComponent(InteractionDetailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('No notes recorded.');
    });
  });

  describe('Not found state (404)', () => {
    beforeEach(() => {
      interactionServiceMock.getById.mockReturnValue(
        throwError(() => ({ status: 404 }))
      );
      fixture = TestBed.createComponent(InteractionDetailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should set isNotFound to true on 404', () => {
      expect(component.isNotFound()).toBe(true);
    });

    it('should set isLoading to false', () => {
      expect(component.isLoading()).toBe(false);
    });

    it('should display the not-found message', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.not-found')).toBeTruthy();
      expect(compiled.textContent).toContain('Interaction Not Found');
    });

    it('should provide a link back to the interactions list', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const link = compiled.querySelector('.not-found a');
      expect(link).toBeTruthy();
      // RouterLink doesn't produce href in test environment; check the routerLink attribute instead
      expect(link?.textContent).toContain('Back to Interactions');
    });
  });

  describe('Error state (5xx)', () => {
    beforeEach(() => {
      interactionServiceMock.getById.mockReturnValue(
        throwError(() => ({ status: 500 }))
      );
      fixture = TestBed.createComponent(InteractionDetailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should set errorMessage on 5xx error', () => {
      expect(component.errorMessage()).toBe('Failed to load interaction. Please try again.');
    });

    it('should set isLoading to false', () => {
      expect(component.isLoading()).toBe(false);
    });

    it('should not set isNotFound', () => {
      expect(component.isNotFound()).toBe(false);
    });

    it('should display error message in template', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.error-container')).toBeTruthy();
      expect(compiled.textContent).toContain('Failed to load interaction. Please try again.');
    });

    it('should display a retry button', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const retryBtn = compiled.querySelector('.error-container button');
      expect(retryBtn).toBeTruthy();
      expect(retryBtn?.textContent).toContain('Retry');
    });
  });

  describe('Retry behavior', () => {
    it('should re-call the service when retry button is clicked', () => {
      // First call fails
      interactionServiceMock.getById.mockReturnValue(
        throwError(() => ({ status: 500 }))
      );
      fixture = TestBed.createComponent(InteractionDetailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(interactionServiceMock.getById).toHaveBeenCalledTimes(1);

      // Set up second call to succeed
      interactionServiceMock.getById.mockReturnValue(of(mockInteraction));

      // Click retry
      const compiled = fixture.nativeElement as HTMLElement;
      const retryBtn = compiled.querySelector('.error-container button') as HTMLButtonElement;
      retryBtn.click();
      fixture.detectChanges();

      expect(interactionServiceMock.getById).toHaveBeenCalledTimes(2);
      expect(interactionServiceMock.getById).toHaveBeenCalledWith(testId);
      expect(component.interaction()).toEqual(mockInteraction);
      expect(component.errorMessage()).toBeNull();
    });
  });

  describe('formatType', () => {
    beforeEach(() => {
      interactionServiceMock.getById.mockReturnValue(of(mockInteraction));
      fixture = TestBed.createComponent(InteractionDetailComponent);
      component = fixture.componentInstance;
    });

    it('should format CHECK_IN as "CHECK IN"', () => {
      expect(component.formatType('CHECK_IN')).toBe('CHECK IN');
    });

    it('should format MENTORING as "MENTORING"', () => {
      expect(component.formatType('MENTORING')).toBe('MENTORING');
    });

    it('should format CATCH_UP as "CATCH UP"', () => {
      expect(component.formatType('CATCH_UP')).toBe('CATCH UP');
    });

    it('should format PERFORMANCE_REVIEW as "PERFORMANCE REVIEW"', () => {
      expect(component.formatType('PERFORMANCE_REVIEW')).toBe('PERFORMANCE REVIEW');
    });

    it('should format INFORMAL as "INFORMAL"', () => {
      expect(component.formatType('INFORMAL')).toBe('INFORMAL');
    });
  });

  describe('Create Follow-up Task navigation', () => {
    beforeEach(() => {
      interactionServiceMock.getById.mockReturnValue(of(mockInteraction));
      fixture = TestBed.createComponent(InteractionDetailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should navigate to task creation with query params when button is clicked', () => {
      component.createFollowUpTask();

      expect(routerMock.navigate).toHaveBeenCalledWith(['/tasks/create'], {
        queryParams: {
          interactionId: mockInteraction.id,
          employeeId: mockInteraction.employeeId,
          staffId: mockInteraction.staffId
        }
      });
    });

    it('should not navigate if interaction is null', () => {
      component.interaction.set(null);
      component.createFollowUpTask();

      expect(routerMock.navigate).not.toHaveBeenCalled();
    });
  });
});
