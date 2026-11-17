import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface ProductRequest {
  titulo: string;
  descripcion: string;
  precio: number;
  campus?: string;
}

/**
 * Optional response contract for flash offers; current product responses omit it.
 * The server must provide an ISO-8601 deadline and non-negative available stock.
 */
export interface FlashSaleOffer {
  precioOferta: number;
  cuposDisponibles: number;
  finalizaEn: string;
  estado?: 'ACTIVA' | 'AGOTADA' | 'EXPIRADA' | 'SOBREVENTA';
  mensaje?: string;
}

export interface Product {
  id: number;
  titulo: string;
  descripcion: string | null;
  precio: number;
  categoria?: string | null;
  campus?: string | null;
  estado: string;
  createdAt: string | null;
  usuarioId: number;
  vendedorNombre: string;
  ofertaFlash?: FlashSaleOffer;
}

export interface SearchInterpretation {
  palabrasClave: string[];
  categoria: string | null;
  campus: string | null;
  precioMaximo: number | null;
  tags: string[];
  origen: 'LLM' | 'HEURISTICA';
}

export interface SearchResponse {
  consulta: string;
  interpretacion: SearchInterpretation;
  resultados: Product[];
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

  search(query: string): Observable<SearchResponse> {
    return this.http.get<SearchResponse>(`${this.productsUrl}/buscar`, {
      params: { q: query }
    });
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
