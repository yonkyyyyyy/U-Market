import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

type AuthView = 'login' | 'register' | 'forgot';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {

  currentView: AuthView = 'login';

  email = '';
  password = '';

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
    console.log('Login:', {
      email: this.email,
      password: this.password
    });
  }

  register(): void {
    console.log('Registro:', {
      name: this.name,
      lastName: this.lastName,
      email: this.registerEmail,
      password: this.registerPassword,
      confirmPassword: this.confirmPassword
    });
  }

  recoverPassword(): void {
    console.log('Recuperación:', this.recoveryEmail);
  }

}