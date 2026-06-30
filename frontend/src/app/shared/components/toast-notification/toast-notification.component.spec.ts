import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { ToastNotificationComponent } from './toast-notification.component';

describe('ToastNotificationComponent', () => {
  let fixture: ComponentFixture<ToastNotificationComponent>;
  let component: ToastNotificationComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastNotificationComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ToastNotificationComponent);
    component = fixture.componentInstance;
  });

  describe('auto-dismiss timer', () => {
    it('should emit dismissed after autoDismissMs elapses', fakeAsync(() => {
      component.message = 'Task created';
      component.autoDismissMs = 3000;
      const dismissedSpy = vi.fn();
      component.dismissed.subscribe(dismissedSpy);

      fixture.detectChanges(); // triggers ngOnInit

      expect(dismissedSpy).not.toHaveBeenCalled();

      tick(3000);

      expect(dismissedSpy).toHaveBeenCalledTimes(1);
    }));

    it('should use default 5000ms when autoDismissMs not set', fakeAsync(() => {
      component.message = 'Success';
      const dismissedSpy = vi.fn();
      component.dismissed.subscribe(dismissedSpy);

      fixture.detectChanges();

      tick(4999);
      expect(dismissedSpy).not.toHaveBeenCalled();

      tick(1);
      expect(dismissedSpy).toHaveBeenCalledTimes(1);
    }));
  });

  describe('manual dismiss', () => {
    it('should cancel auto-dismiss timer and emit dismissed when dismiss() called', fakeAsync(() => {
      component.message = 'Task created';
      component.autoDismissMs = 5000;
      const dismissedSpy = vi.fn();
      component.dismissed.subscribe(dismissedSpy);

      fixture.detectChanges();

      tick(2000); // partway through timer
      component.dismiss();

      expect(dismissedSpy).toHaveBeenCalledTimes(1);

      // Timer should be cancelled — no second emission after remaining time
      tick(5000);
      expect(dismissedSpy).toHaveBeenCalledTimes(1);
    }));
  });

  describe('dismiss button in template', () => {
    it('should emit dismissed when dismiss button (×) is clicked', fakeAsync(() => {
      component.message = 'Follow-up task created';
      component.type = 'success';
      const dismissedSpy = vi.fn();
      component.dismissed.subscribe(dismissedSpy);

      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('.toast-dismiss') as HTMLButtonElement;
      expect(button).toBeTruthy();
      button.click();

      expect(dismissedSpy).toHaveBeenCalledTimes(1);

      // Auto-dismiss timer should be cancelled
      tick(5000);
      expect(dismissedSpy).toHaveBeenCalledTimes(1);
    }));
  });

  describe('ngOnDestroy cancels timer', () => {
    it('should not emit dismissed after component is destroyed', fakeAsync(() => {
      component.message = 'Task created';
      component.autoDismissMs = 3000;
      const dismissedSpy = vi.fn();
      component.dismissed.subscribe(dismissedSpy);

      fixture.detectChanges();

      tick(1000); // partway through timer
      component.ngOnDestroy();

      tick(5000); // well past original timeout
      expect(dismissedSpy).not.toHaveBeenCalled();
    }));
  });

  describe('success/error styling', () => {
    it('should apply .toast-success class when type is success', () => {
      component.message = 'Created successfully';
      component.type = 'success';

      fixture.detectChanges();

      const toastEl = fixture.nativeElement.querySelector('.toast');
      expect(toastEl.classList.contains('toast-success')).toBe(true);
      expect(toastEl.classList.contains('toast-error')).toBe(false);
    });

    it('should apply .toast-error class when type is error', () => {
      component.message = 'Something went wrong';
      component.type = 'error';

      fixture.detectChanges();

      const toastEl = fixture.nativeElement.querySelector('.toast');
      expect(toastEl.classList.contains('toast-error')).toBe(true);
      expect(toastEl.classList.contains('toast-success')).toBe(false);
    });

    it('should not render toast when message is empty', () => {
      component.message = '';

      fixture.detectChanges();

      const toastEl = fixture.nativeElement.querySelector('.toast');
      expect(toastEl).toBeNull();
    });
  });
});
