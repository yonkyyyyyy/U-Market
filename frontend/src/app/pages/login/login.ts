import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {

  email = '';
  password = '';

  constructor(private router: Router) {}

  login(): void {
    console.log('Correo:', this.email);
    console.log('Contraseña:', this.password);
  }

  goToRegister(): void {
    this.router.navigate(['/registro']);
  }
}