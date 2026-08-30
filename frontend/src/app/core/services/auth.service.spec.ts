import {TestBed} from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {PLATFORM_ID} from '@angular/core';
import {AuthService} from './auth.service';
import {provideHttpClient} from '@angular/common/http';
import {LoginRequest, TokenResponse} from '@core/models/auth.model';
import {firstValueFrom} from 'rxjs';

describe('AuthService', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [],
      providers: [AuthService, {
        provide: PLATFORM_ID,
        useValue: 'browser'
      }, provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('stores tokens on login and updates authenticated state', () => {
    const service = TestBed.inject(AuthService);
    const httpMock = TestBed.inject(HttpTestingController);

    const creds: LoginRequest = {username: 'user', password: 'pass'};
    const payload = {exp: Math.floor(Date.now() / 1000) + 3600, roles: 'ROLE_USER'};
    const accessToken = `h.${btoa(JSON.stringify(payload))}.s`;
    const tokens: TokenResponse = {accessToken, refreshToken: 'rft', expiresIn: 12, tokenType: 'Bearer'};

    service.login(creds).subscribe(async (res) => {
      expect(res).toEqual(tokens);
      expect(sessionStorage.getItem('accessToken')).toBe(tokens.accessToken);
      expect(sessionStorage.getItem('refreshToken')).toBe(tokens.refreshToken);

      const v = await firstValueFrom(service.isAuthenticated$);
      expect(v).toBeTruthy();
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/login'));
    expect(req.request.method).toBe('POST');
    req.flush(tokens);
    httpMock.verify();
  });

  it('parses roles from stored token and reports admin role', () => {
    // Prepare a token that contains ROLE_ADMIN in the payload
    sessionStorage.clear();
    const payload = {exp: Math.floor(Date.now() / 1000) + 3600, roles: ['ROLE_ADMIN', 'ROLE_USER']};
    const accessToken = `h.${btoa(JSON.stringify(payload))}.s`;
    sessionStorage.setItem('accessToken', accessToken);

    // Recreate service so constructor reads sessionStorage
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [],
      providers: [AuthService, {
        provide: PLATFORM_ID,
        useValue: 'browser'
      }, provideHttpClient(), provideHttpClientTesting()]
    });
    const service = TestBed.inject(AuthService);

    expect(service.isAdmin()).toBeTruthy();
  });
});

