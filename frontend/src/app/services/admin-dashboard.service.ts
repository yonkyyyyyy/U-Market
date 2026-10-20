import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ModerationMetrics {
  verdaderoPositivo: number;
  falsoPositivo: number;
  falsoNegativo: number;
  verdaderoNegativo: number;
  revisados: number;
  pendientesDeRevision: number;
  fallidos: number;
  total: number;
  exactitud: number;
  precision: number;
  recall: number;
  f1: number;
}

export interface ModerationDemand {
  id: number;
  titulo: string;
  descripcion: string | null;
  categoria: string | null;
  estado: string;
  presupuestoEstimado: number | null;
  usuarioId: number;
  usuarioNombre: string;
  fechaCreacion: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminDashboardService {
  private readonly http = inject(HttpClient);

  getMetrics(): Observable<ModerationMetrics> {
    return this.http.get<ModerationMetrics>('/api/admin/moderacion/ia/metricas');
  }

  getDemands(page = 0, size = 10): Observable<PageResponse<ModerationDemand>> {
    return this.http.get<PageResponse<ModerationDemand>>('/api/admin/moderacion/demandas', {
      params: {
        page: String(page),
        size: String(size)
      }
    });
  }

  blockDemand(id: number, motivo: string): Observable<ModerationDemand> {
    return this.http.patch<ModerationDemand>(`/api/admin/moderacion/demandas/${id}/bloquear`, { motivo });
  }
}
