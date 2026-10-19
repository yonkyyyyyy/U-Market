import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { Product, ProductService } from '../../services/product.service';

@Component({
  selector: 'app-product-carousel',
  imports: [],
  templateUrl: './product-carousel.html',
  styleUrl: './product-carousel.css'
})
export class ProductCarousel {
  private readonly productService = inject(ProductService);
  private readonly authService = inject(AuthService);

  readonly products = this.productService.products;
  readonly isLoading = signal(true);
  readonly loadError = signal('');
  currentSlide = 0;

  constructor() {
    if (!this.authService.getToken()) {
      this.isLoading.set(false);
      this.loadError.set('Inicia sesión para ver las publicaciones.');
      return;
    }

    this.productService.list().subscribe({
      next: () => this.isLoading.set(false),
      error: (error: unknown) => {
        this.isLoading.set(false);
        this.loadError.set(
          error instanceof HttpErrorResponse && error.status === 401
            ? 'Tu sesión venció. Inicia sesión de nuevo para ver las publicaciones.'
            : 'No se pudieron cargar las publicaciones. Inténtalo de nuevo más tarde.'
        );
      }
    });
  }

  nextSlide(): void {
    if (this.products().length > 0) {
      this.currentSlide = (this.currentSlide + 1) % this.products().length;
    }
  }

  previousSlide(): void {
    const length = this.products().length;
    if (length > 0) {
      this.currentSlide = (this.currentSlide - 1 + length) % length;
    }
  }

  goToSlide(index: number): void {
    this.currentSlide = index;
  }

  formatPrice(product: Product): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN'
    }).format(product.precio);
  }
}
