import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface ProductRequest {
  titulo: string;
  descripcion: string;
  precio: number;
}

export interface Product {
  id: number;
  titulo: string;
  descripcion: string | null;
  precio: number;
  estado: string;
  createdAt: string | null;
  usuarioId: number;
  vendedorNombre: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly productsUrl = '/api/productos';
  readonly products = signal<Product[]>([]);

  list(): Observable<Product[]> {
    return this.http.get<Product[]>(this.productsUrl, {
      params: { estado: 'DISPONIBLE' }
    }).pipe(tap(products => this.products.set(products)));
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.productsUrl, request).pipe(
      tap(product => this.products.update(products => [
        product,
        ...products.filter(existing => existing.id !== product.id)
      ]))
    );
  }
}
