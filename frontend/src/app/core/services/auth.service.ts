import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { LoginRequest, TokenResponse, UserRole } from '../models/auth.model';
import { environment } from '../../../environments/environment';

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

  private readonly http   = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  private readonly _isAuthenticated$ = new BehaviorSubject<boolean>(this.hasValidToken());

  readonly isAuthenticated$ = this._isAuthenticated$.asObservable();

  login(credentials: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.baseUrl}/login`, credentials).pipe(
      tap(tokens => this.storeTokens(tokens)),
    );
  }

  refresh(): Observable<TokenResponse> {
    const refreshToken = sessionStorage.getItem('refreshToken');
    return this.http.post<TokenResponse>(`${this.baseUrl}/refresh`, { refreshToken }).pipe(
      tap(tokens => this.storeTokens(tokens)),
    );
  }

  logout(): void {
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');
    this._isAuthenticated$.next(false);
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null {
    return sessionStorage.getItem('accessToken');
  }

  isAdmin(): boolean {
    return this.extractRoles().includes('ROLE_ADMIN');
  }

  private storeTokens(tokens: TokenResponse): void {
    sessionStorage.setItem('accessToken', tokens.accessToken);
    sessionStorage.setItem('refreshToken', tokens.refreshToken);
    this._isAuthenticated$.next(true);
  }

  private hasValidToken(): boolean {
    return !!sessionStorage.getItem('accessToken');
  }

  private extractRoles(): UserRole[] {
    // TODO: decode JWT payload and extract roles claim
    return [];
  }
}
