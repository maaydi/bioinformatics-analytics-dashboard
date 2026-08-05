import {isPlatformBrowser} from '@angular/common';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {Inject, inject, Injectable, PLATFORM_ID} from '@angular/core';
import {Router} from '@angular/router';
import {BehaviorSubject, catchError, map, Observable, of, tap} from 'rxjs';
import {environment} from '@env/environment';
import {
  ChangePasswordRequest,
  ChangePasswordResponse,
  JwtPayload,
  LoginRequest,
  TokenResponse,
  UserRole
} from '../models/auth.model';
import {NotificationService} from '@shared/directive/notification.service';

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
@Injectable({providedIn: 'root'})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);

  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  private readonly isBrowser: boolean;
  private readonly _isAuthenticated$: BehaviorSubject<boolean>;
  readonly isAuthenticated$: Observable<boolean>;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    const initialStatus = this.hasValidToken();
    this._isAuthenticated$ = new BehaviorSubject<boolean>(initialStatus);
    this.isAuthenticated$ = this._isAuthenticated$.asObservable();
  }

  login(credentials: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.baseUrl}/login`, credentials).pipe(
      tap((tokens) => this.storeTokens(tokens)),
      catchError((error: HttpErrorResponse) => {
        this.notify.error(
          `Login failed: ${error.error?.message || error.message || 'Unknown error'}. Please try again.`,
        );
        throw error;
      }),
    );
  }

  refresh(): Observable<TokenResponse> {
    const refreshToken = this.isBrowser ? sessionStorage.getItem('refreshToken') : null;
    return this.http.post<TokenResponse>(`${this.baseUrl}/refresh`, {refreshToken}).pipe(
      tap((tokens) => this.storeTokens(tokens)),
      catchError((error: HttpErrorResponse) => {
        this.notify.error('Session expired. For your security, please sign in again to continue.');
        throw error;
      }),
    );
  }

  logout(): void {
    if (this.isBrowser) {
      this.http.post<TokenResponse>(`${this.baseUrl}/logout`, {}).subscribe({
        next: () => {
          this.notify.success('Logged out successfully');
          this.clearTokens();
        },
        error: (err: HttpErrorResponse) => {
          this.notify.error(`Logout failed: ${err.message}`);
          this.clearTokens();
        },
      });
    }
    this._isAuthenticated$.next(false);
    this.router
      .navigate(['/login'])
      .then((r) => console.log('Navigated to /login after logout:', r));
  }

  getAccessToken(): string | null {
    return this.isBrowser ? sessionStorage.getItem('accessToken') : null;
  }

  isAdmin(): boolean {
    return this.extractRoles().includes('ROLE_ADMIN');
  }

  changePassword(data: ChangePasswordRequest): Observable<ChangePasswordResponse> {
    if (!this.isBrowser) {
      return of({success: false, message: 'Not running in a browser environment'});
    }
    return this.http.put<ChangePasswordResponse>(`${this.baseUrl}/password`, data).pipe(
      map((response) => {
        this.notify.success('Password changed successfully');
        this.clearTokens();
        return response;
      }),
      catchError((err) => {
        this.notify.error(`Failed to change password: ${err.message || err}`);
        return of({success: false, message: err.message || 'Unknown error'});
      }),
    );
  }

  private storeTokens(tokens: TokenResponse): void {
    if (this.isBrowser) {
      sessionStorage.setItem('accessToken', tokens.accessToken);
      sessionStorage.setItem('refreshToken', tokens.refreshToken);
    }
    this._isAuthenticated$.next(true);
  }

  private clearTokens(): void {
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');
  }

  private hasValidToken(): boolean {
    if (!this.isBrowser) return false;
    const token = sessionStorage.getItem('accessToken');
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return Math.floor(Date.now() / 1000) < payload.exp;
    } catch {
      return false;
    }
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
