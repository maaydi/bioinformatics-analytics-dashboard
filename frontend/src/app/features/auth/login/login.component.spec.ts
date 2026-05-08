import {ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {of, throwError} from 'rxjs';

import {LoginComponent} from './login.component';
import {AuthService} from '@core/services/auth.service';
import {Router} from '@angular/router';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;

  let mockRouterNavigateCalled = false;
  let mockRouterNavigateArgs: any[] | null = null;
  const mockRouter = {
    navigate: (...args: any[]) => {
      mockRouterNavigateCalled = true;
      mockRouterNavigateArgs = args;
      return Promise.resolve(true);
    },
  } as unknown as Router;

  let mockAuth: { login: (payload: any) => any };

  beforeEach(async () => {
    mockRouterNavigateCalled = false;
    mockRouterNavigateArgs = null;
    mockAuth = {login: () => of({})};

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        {provide: AuthService, useValue: mockAuth},
        {provide: Router, useValue: mockRouter},
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable(); // ensure initial render is settled
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should disable submit when form is invalid', () => {
    fixture.detectChanges();
    const btn = fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement as HTMLButtonElement;
    expect(component.form.invalid).toBeTruthy();
    expect(btn.disabled).toBeTruthy();
  });

  it('should navigate to root on successful login', async () => {
    mockAuth.login = () => of({accessToken: 'a', refreshToken: 'r'});

    component.form.setValue({username: 'testuser', password: 'pwd'});
    fixture.detectChanges();

    const form = fixture.debugElement.query(By.css('form')).nativeElement as HTMLFormElement;
    form.dispatchEvent(new Event('submit', {bubbles: true, cancelable: true}));

    await fixture.whenStable();
    fixture.detectChanges(); // sync DOM after signal/router updates

    expect(mockRouterNavigateCalled).toBeTruthy();
    expect(mockRouterNavigateArgs).toEqual([['/']]);
  });

  it('should show error message on failed login and stop loading', async () => {
    mockAuth.login = () => throwError(() => new Error('Invalid credentials'));

    component.form.setValue({username: 'baduser', password: 'wrong'});
    fixture.detectChanges();

    const form = fixture.debugElement.query(By.css('form')).nativeElement as HTMLFormElement;
    form.dispatchEvent(new Event('submit', {bubbles: true, cancelable: true}));

    await fixture.whenStable();
    fixture.detectChanges(); // critical: update DOM before querying

    const errorEl = fixture.debugElement.query(By.css('.error-text'));
    expect(component.loading()).toBeFalsy();
    expect(component.errorMessage()).toBe('Invalid credentials. Please try again.');
    expect(errorEl).toBeTruthy();
    expect(errorEl!.nativeElement.textContent).toContain('Invalid credentials');
  });
});
