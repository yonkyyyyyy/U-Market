import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

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
      error: () => {
        this.isLoggingIn = false;
        this.loginError = 'No se pudo iniciar sesión. Verifica tus credenciales e inténtalo de nuevo.';
      }
    });
  }



  register(): void {

    const nombreCompleto =
      `${this.name} ${this.lastName}`.trim();

    const registerRequest = {
      nombre: nombreCompleto,
      correo: this.registerEmail,
      password: this.registerPassword
    };

    console.log('Datos de registro:', registerRequest);
  }



  recoverPassword(): void {

    console.log('Correo para recuperación:', this.recoveryEmail);

  }

}