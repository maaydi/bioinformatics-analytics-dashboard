/**
 * TypeScript models for authentication API.
 *
 * Schemas defined in documentation/api-contract.md §5.
 */

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: 'Bearer';
}

/** Decoded JWT payload (subset of claims used by the frontend). */
export interface JwtPayload {
  sub: string; // username
  exp: number; // expiry timestamp (seconds)
  iat: number; // issued-at timestamp
  type: 'access' | 'refresh';
  roles: UserRole[]; // comma-separated, e.g. "ROLE_USER" or "ROLE_ADMIN"
}

export type UserRole = 'ROLE_USER' | 'ROLE_ADMIN';

export interface ChangePasswordResponse {
  success: boolean;
  message: String;
}

export interface ChangePasswordRequest {
  newPassword: string;
  currentPassword: string;
}
