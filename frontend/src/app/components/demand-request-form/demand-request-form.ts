import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DemandRequest } from '../../services/demand.service';

export interface DemandDetails {
  request: DemandRequest;
  category: string;
  campus: string;
  validForDays: number;
}

@Component({
  selector: 'app-demand-request-form',
  imports: [FormsModule],
  templateUrl: './demand-request-form.html',
  styleUrl: './demand-request-form.css'
})
export class DemandRequestForm {
  @Output() readonly submitted = new EventEmitter<DemandDetails>();
  @Output() readonly cancelled = new EventEmitter<void>();
  @Input() submitting = false;

  readonly categories = [
    'Libros y apuntes',
    'Tecnología',
    'Accesorios',
    'Hogar y dormitorio',
    'Ropa y cuidado personal',
    'Otros'
  ];

  readonly campuses = [
    'Lima Centro',
    'Lima Este',
    'Lima Norte',
    'Lima Sur',
    'Ate',
    'Callao',
    'Arequipa',
    'Chiclayo',
    'Chimbote',
    'Huancayo',
    'Ica',
    'Piura',
    'Trujillo'
  ];

  readonly validityOptions = [
    { days: 1, label: '24 horas' },
    { days: 3, label: '3 días' },
    { days: 7, label: '1 semana' },
    { days: 14, label: '2 semanas' }
  ];

  titulo = '';
  descripcion = '';
  presupuesto: string | number | null = '';
  categoria = '';
  campus = '';
  vigencia = 7;

  submit(): void {
    if (!this.titulo.trim() || !this.categoria || !this.campus) {
      return;
    }

    const parsedBudget = this.presupuesto === '' || this.presupuesto === null
      ? null
      : Number(this.presupuesto);
    if (parsedBudget !== null && (!Number.isFinite(parsedBudget) || parsedBudget <= 0)) {
      return;
    }

    this.submitted.emit({
      request: {
        titulo: this.titulo.trim(),
        descripcion: this.descripcion.trim(),
        presupuestoEstimado: parsedBudget,
        categoria: this.categoria.toLocaleUpperCase('es')
      },
      category: this.categoria,
      campus: this.campus,
      validForDays: this.vigencia
    });
  }

  closeOnBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cancelled.emit();
    }
  }
}
