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

  // LOGIN

  correo = '';
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

    const loginRequest = {
      correo: this.correo,
      password: this.password
    };

    console.log('Datos de login:', loginRequest);
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