import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID, inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { JwtPayload, LoginRequest, TokenResponse, UserRole } from '../models/auth.model';

/**
 * Authentication service — manages JWT tokens and user session state.
 *
 * Responsibilities:
 * - POST /api/auth/login and /api/auth/refresh
 * - Store / retrieve tokens from sessionStorage
 * - Expose current user state as an Observable
 * - Token decoding for role checks
 *
 * @see documentation/api-contract.md §5 — Authentication Endpoints
 * @see documentation/validation-rules.md §4 — Authentication Rules
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  private isBrowser: boolean;
  private readonly _isAuthenticated$: BehaviorSubject<boolean>;
  readonly isAuthenticated$: Observable<boolean>;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    this._isAuthenticated$ = new BehaviorSubject<boolean>(this.hasValidToken());
    this.isAuthenticated$ = this._isAuthenticated$.asObservable();
  }

  login(credentials: LoginRequest): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${this.baseUrl}/login`, credentials)
      .pipe(tap((tokens) => this.storeTokens(tokens)));
  }

  refresh(): Observable<TokenResponse> {
    const refreshToken = this.isBrowser ? sessionStorage.getItem('refreshToken') : null;
    return this.http
      .post<TokenResponse>(`${this.baseUrl}/refresh`, { refreshToken })
      .pipe(tap((tokens) => this.storeTokens(tokens)));
  }

  logout(): void {
    if (this.isBrowser) {
      sessionStorage.removeItem('accessToken');
      sessionStorage.removeItem('refreshToken');
    }
    this._isAuthenticated$.next(false);
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null {
    return this.isBrowser ? sessionStorage.getItem('accessToken') : null;
  }

  isAdmin(): boolean {
    return this.extractRoles().includes('ROLE_ADMIN');
  }

  private storeTokens(tokens: TokenResponse): void {
    if (this.isBrowser) {
      sessionStorage.setItem('accessToken', tokens.accessToken);
      sessionStorage.setItem('refreshToken', tokens.refreshToken);
    }
    this._isAuthenticated$.next(true);
  }

  private hasValidToken(): boolean {
    return this.isBrowser ? !!sessionStorage.getItem('accessToken') : false;
  }

  private extractRoles(): UserRole[] {
    const token = this.getAccessToken();
    if (!token) return [];
    try {
      const payload = JSON.parse(atob(token.split('.')[1])) as JwtPayload;
      return payload.roles ? (payload.roles.split(',') as UserRole[]) : [];
    } catch {
      return [];
    }
  }
}
