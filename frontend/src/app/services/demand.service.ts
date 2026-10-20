import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface DemandRequest {
  titulo: string;
  descripcion: string;
  presupuestoEstimado: number | null;
  categoria: string;
}

export interface Demand {
  id: number;
  titulo: string;
  descripcion: string | null;
  presupuestoEstimado: number | null;
  categoria: string | null;
  estado: string;
  fechaCreacion: string | null;
  usuarioId: number;
  usuarioNombre: string;
}

export interface DemandPage {
  content: Demand[];
  number: number;
  totalPages: number;
  totalElements: number;
  last: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class DemandService {
  private readonly http = inject(HttpClient);
  private readonly demandsUrl = '/api/demandas';

  list(page: number, size = 20): Observable<DemandPage> {
    return this.http.get<DemandPage>(this.demandsUrl, {
      params: { estado: 'ACTIVA', page, size }
    });
  }

  create(request: DemandRequest): Observable<Demand> {
    return this.http.post<Demand>(this.demandsUrl, request);
  }
}
