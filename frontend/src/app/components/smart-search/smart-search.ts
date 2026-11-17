import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Product, ProductService, SearchResponse } from '../../services/product.service';

@Component({
  selector: 'app-smart-search',
  imports: [FormsModule],
  templateUrl: './smart-search.html',
  styleUrl: './smart-search.css'
})
export class SmartSearch {
  private readonly productService = inject(ProductService);
  private readonly authService = inject(AuthService);

  readonly ejemplos = [
    'laptop para clases hasta 1500 soles en Lima Este',
    'libros de cálculo baratos',
    'mochila en buen estado en Ate'
  ];

  consulta = '';
  readonly isLoading = signal(false);
  readonly error = signal('');
  readonly response = signal<SearchResponse | null>(null);

  buscar(): void {
    const consulta = this.consulta.trim();
    this.error.set('');

    if (!consulta) {
      this.response.set(null);
      return;
    }

    if (!this.authService.getToken()) {
      this.response.set(null);
      this.error.set('Inicia sesión para buscar publicaciones.');
      return;
    }

    this.isLoading.set(true);
    this.productService.search(consulta).subscribe({
      next: response => {
        this.isLoading.set(false);
        this.response.set(response);
      },
      error: (error: unknown) => {
        this.isLoading.set(false);
        this.response.set(null);
        this.error.set(
          error instanceof HttpErrorResponse && error.status === 401
            ? 'Tu sesión venció. Inicia sesión de nuevo para buscar.'
            : 'No se pudo interpretar la búsqueda. Inténtalo de nuevo.'
        );
      }
    });
  }

  usarEjemplo(ejemplo: string): void {
    this.consulta = ejemplo;
    this.buscar();
  }

  limpiar(): void {
    this.consulta = '';
    this.response.set(null);
    this.error.set('');
  }

  esLlm(): boolean {
    return this.response()?.interpretacion.origen === 'LLM';
  }

  resumenInterpretacion(): string[] {
    const interpretacion = this.response()?.interpretacion;
    if (!interpretacion) {
      return [];
    }

    const resumen: string[] = [];
    if (interpretacion.categoria) {
      resumen.push(`Categoría: ${interpretacion.categoria}`);
    }
    if (interpretacion.campus) {
      resumen.push(`Campus: ${interpretacion.campus}`);
    }
    if (interpretacion.precioMaximo !== null) {
      resumen.push(`Hasta ${this.formatPriceValue(interpretacion.precioMaximo)}`);
    }
    return resumen;
  }

  formatPrice(product: Product): string {
    return this.formatPriceValue(product.precio);
  }

  private formatPriceValue(value: number): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN'
    }).format(value);
  }
}
