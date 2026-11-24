import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface CopyGenerationRequest {
  titulo: string;
  descripcion: string;
  precio?: number;
  campus?: string;
  plataforma: string;
  tono: string;
  publicoObjetivo?: string;
}

export interface CopyGenerationResponse {
  copys: string[];
}

@Injectable({
  providedIn: 'root'
})
export class CopywritingService {
  private readonly http = inject(HttpClient);

  generate(request: CopyGenerationRequest): Observable<CopyGenerationResponse> {
    return this.http.post<CopyGenerationResponse>('/api/copys/generar', request);
  }
}
