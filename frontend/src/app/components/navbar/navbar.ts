import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { inject, signal, computed } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  imports: [RouterLink],
  selector: 'app-navbar',
  styleUrl: './navbar.css',
  templateUrl: './navbar.html',
})
export class Navbar {
  private readonly authService = inject(AuthService);
  protected readonly isMenuOpen = signal(false);
  protected readonly isAdmin = computed(() => this.authService.isAdmin());

  protected toggleMenu(): void {
    this.isMenuOpen.update((isOpen: boolean) => !isOpen);git
  }

  protected closeMenu(): void {
    this.isMenuOpen.set(false);
  }
}
