import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ImportSkillsComponent } from './import-skills.component';
import { ImportResult } from '../../models/portfolio.models';

describe('ImportSkillsComponent', () => {
  let component: ImportSkillsComponent;
  let fixture: ComponentFixture<ImportSkillsComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImportSkillsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ImportSkillsComponent);
    component = fixture.componentInstance;
    component.employeeId = 'test-employee-id';
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    // Flush the getLinks call triggered by ngOnInit
    const linksReq = httpMock.expectOne(r => r.url.includes('/links'));
    linksReq.flush([]);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should render input field with correct placeholder', () => {
    const input = fixture.nativeElement.querySelector('.url-input') as HTMLInputElement;
    expect(input).toBeTruthy();
    expect(input.placeholder).toBe('https://github.com/username');
  });

  it('should render Import Skills button', () => {
    const button = fixture.nativeElement.querySelector('.import-btn') as HTMLButtonElement;
    expect(button).toBeTruthy();
    expect(button.textContent?.trim()).toBe('Import Skills');
  });

  it('should show validation error when input is empty', () => {
    component.urlControl.setValue('');
    component.importSkills();
    fixture.detectChanges();

    const error = fixture.nativeElement.querySelector('.validation-error');
    expect(error).toBeTruthy();
    expect(error.textContent).toContain('GitHub profile URL is required');
  });

  it('should show validation error when input is whitespace only', () => {
    component.urlControl.setValue('   ');
    component.importSkills();
    fixture.detectChanges();

    const error = fixture.nativeElement.querySelector('.validation-error');
    expect(error).toBeTruthy();
    expect(error.textContent).toContain('GitHub profile URL is required');
  });

  it('should not call backend when input is empty', () => {
    component.urlControl.setValue('');
    component.importSkills();
    httpMock.expectNone(() => true);
  });

  it('should show loading state during import', () => {
    component.urlControl.setValue('https://github.com/octocat');
    component.importSkills();
    fixture.detectChanges();

    expect(component.loading()).toBe(true);
    const spinner = fixture.nativeElement.querySelector('.spinner');
    expect(spinner).toBeTruthy();

    const loadingMsg = fixture.nativeElement.querySelector('.loading-message');
    expect(loadingMsg?.textContent).toContain('Import in progress');

    const button = fixture.nativeElement.querySelector('.import-btn') as HTMLButtonElement;
    expect(button.disabled).toBe(true);

    httpMock.expectOne(() => true).flush({});
  });

  it('should display results on success', () => {
    const mockResult: ImportResult = {
      skills: [
        { id: '1', name: 'Java', projectCount: 5, proficiency: 'EXPERT', source: 'GITHUB' },
        { id: '2', name: 'Python', projectCount: 3, proficiency: 'ADVANCED', source: 'GITHUB' }
      ],
      githubProfileUrl: 'https://github.com/octocat',
      repositoriesAnalysed: 10,
      skippedRepositories: []
    };

    component.urlControl.setValue('https://github.com/octocat');
    component.importSkills();

    const req = httpMock.expectOne(r => r.url.includes('github-import'));
    req.flush(mockResult);
    fixture.detectChanges();

    expect(component.result()).toEqual(mockResult);
    expect(component.loading()).toBe(false);

    const reposText = fixture.nativeElement.querySelector('.repos-analysed');
    expect(reposText?.textContent).toContain('10');

    const skillItems = fixture.nativeElement.querySelectorAll('.skill-item');
    expect(skillItems.length).toBe(2);
  });

  it('should display error message on failure', () => {
    component.urlControl.setValue('https://github.com/octocat');
    component.importSkills();

    const req = httpMock.expectOne(r => r.url.includes('github-import'));
    req.flush(
      { message: 'GitHub user not found: octocat' },
      { status: 404, statusText: 'Not Found' }
    );
    fixture.detectChanges();

    expect(component.error()).toBe('GitHub user not found: octocat');
    expect(component.loading()).toBe(false);

    const errorEl = fixture.nativeElement.querySelector('.error-message');
    expect(errorEl?.textContent).toContain('GitHub user not found: octocat');
  });

  it('should preserve input value on error', () => {
    component.urlControl.setValue('https://github.com/octocat');
    component.importSkills();

    const req = httpMock.expectOne(r => r.url.includes('github-import'));
    req.flush({ message: 'Error' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(component.urlControl.value).toBe('https://github.com/octocat');
  });

  it('should re-enable controls after error', () => {
    component.urlControl.setValue('https://github.com/octocat');
    component.importSkills();

    const req = httpMock.expectOne(r => r.url.includes('github-import'));
    req.flush({ message: 'Error' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    const button = fixture.nativeElement.querySelector('.import-btn') as HTMLButtonElement;
    expect(button.disabled).toBe(false);
  });

  it('should pre-populate input with existing GitHub link URL', async () => {
    // Create a fresh component to control ngOnInit timing
    const freshFixture = TestBed.createComponent(ImportSkillsComponent);
    const freshComponent = freshFixture.componentInstance;
    freshComponent.employeeId = 'test-employee-id';
    freshFixture.detectChanges();

    const linksReq = httpMock.expectOne(r => r.url.includes('/test-employee-id/links'));
    linksReq.flush([
      { id: 'link-1', employeeId: 'test-employee-id', url: 'https://github.com/octocat', label: 'GitHub', createdAt: '2025-01-01T00:00:00Z' }
    ]);
    freshFixture.detectChanges();

    expect(freshComponent.urlControl.value).toBe('https://github.com/octocat');
  });

  it('should not pre-populate input when no GitHub link exists', async () => {
    // Create a fresh component to control ngOnInit timing
    const freshFixture = TestBed.createComponent(ImportSkillsComponent);
    const freshComponent = freshFixture.componentInstance;
    freshComponent.employeeId = 'test-employee-id';
    freshFixture.detectChanges();

    const linksReq = httpMock.expectOne(r => r.url.includes('/test-employee-id/links'));
    linksReq.flush([
      { id: 'link-2', employeeId: 'test-employee-id', url: 'https://linkedin.com/in/someone', label: 'LinkedIn', createdAt: '2025-01-01T00:00:00Z' }
    ]);
    freshFixture.detectChanges();

    expect(freshComponent.urlControl.value).toBe('');
  });
});
