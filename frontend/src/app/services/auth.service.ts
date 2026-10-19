import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoginRequest {
  correo: string;
  password: string;
}

export interface RegisterRequest {
  nombre: string;
  correo: string;
  password: string;
}

export interface User {
  id: number;
  nombre: string;
  correo: string;
  rol: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly loginUrl = '/api/auth/login';
  private readonly tokenKey = 'umarket_token';
  private readonly userKey = 'umarket_user';

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.loginUrl, request);
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', request);
  }

  saveSession(response: AuthResponse): void {
    sessionStorage.setItem(this.tokenKey, response.token);
    sessionStorage.setItem(this.userKey, JSON.stringify(response.user));
  }

  getToken(): string | null {
    return sessionStorage.getItem(this.tokenKey);
  }
}
