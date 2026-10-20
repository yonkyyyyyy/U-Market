import { Component, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

type AuthView = 'login' | 'register' | 'forgot';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  currentView: AuthView = 'login';

  correo = '';
  password = '';
  isLoggingIn = false;
  loginError = '';

  name = '';
  lastName = '';
  registerEmail = '';
  registerPassword = '';
  confirmPassword = '';
  isRegistering = false;
  registerError = '';


  recoveryEmail = '';

  showRegister(): void {
    this.currentView = 'register';
  }

  showForgot(): void {
    this.currentView = 'forgot';
  }

  showLogin(): void {
    this.currentView = 'login';
  }



  login(): void {
    this.isLoggingIn = true;
    this.loginError = '';

    this.authService.login({
      correo: this.correo,
      password: this.password
    }).subscribe({
      next: (response) => {
        this.authService.saveSession(response);
        this.isLoggingIn = false;
        void this.router.navigate(['/publicaciones']);
      },
      error: (error: unknown) => {
        this.isLoggingIn = false;
        this.loginError = this.getLoginError(error);
      }
    });
  }



  register(): void {
    const nombreCompleto =
      `${this.name} ${this.lastName}`.trim();

    if (!nombreCompleto || !this.registerEmail.trim()) {
      this.registerError = 'Completa tu nombre y correo institucional.';
      return;
    }

    if (!/^[A-Za-z0-9._%+-]+@utp\.edu\.pe$/i.test(this.registerEmail.trim())) {
      this.registerError = 'Regístrate con tu correo institucional @utp.edu.pe.';
      return;
    }

    if (this.registerPassword.length < 8) {
      this.registerError = 'La contraseña debe tener al menos 8 caracteres.';
      return;
    }

    if (this.registerPassword !== this.confirmPassword) {
      this.registerError = 'Las contraseñas no coinciden.';
      return;
    }

    this.isRegistering = true;
    this.registerError = '';

    this.authService.register({
      nombre: nombreCompleto,
      correo: this.registerEmail.trim(),
      password: this.registerPassword
    }).subscribe({
      next: response => {
        this.authService.saveSession(response);
        this.isRegistering = false;
        this.toastService.success('Cuenta creada', 'Ya puedes publicar en U-Market.');
        void this.router.navigate(['/publicaciones']);
      },
      error: (error: unknown) => {
        this.isRegistering = false;
        this.registerError = this.getRegisterError(error);
      }
    });
  }

  private getRegisterError(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se pudo conectar con el servidor. Inténtalo de nuevo más tarde.';
    }

    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }
    return 'No se pudo crear la cuenta. Revisa tus datos e inténtalo de nuevo.';
  }

  private getLoginError(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se pudo conectar con el servidor. Inténtalo de nuevo más tarde.';
    }

    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }

    return 'No se pudo iniciar sesión. Verifica tus credenciales e inténtalo de nuevo.';
  }


  recoverPassword(): void {

    console.log('Correo para recuperación:', this.recoveryEmail);

  }

}