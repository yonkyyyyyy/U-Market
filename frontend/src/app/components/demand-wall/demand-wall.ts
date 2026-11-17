import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Demand, DemandService } from '../../services/demand.service';
import { ToastService } from '../../services/toast.service';
import { DemandDetails, DemandRequestForm } from '../demand-request-form/demand-request-form';

@Component({
  selector: 'app-demand-wall',
  imports: [FormsModule, DemandRequestForm],
  templateUrl: './demand-wall.html',
  styleUrl: './demand-wall.css'
})
export class DemandWall implements OnInit {
  private readonly demandService = inject(DemandService);
  private readonly toastService = inject(ToastService);

  readonly demands = signal<Demand[]>([]);
  readonly totalElements = signal(0);
  readonly isLoading = signal(true);
  readonly isLoadingMore = signal(false);
  readonly hasMore = signal(false);
  readonly isSubmitting = signal(false);
  readonly pageError = signal('');
  readonly isFormOpen = signal(false);
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

  searchTerm = '';
  selectedCategory = '';
  selectedCampus = '';
  private readonly detailsById = new Map<number, DemandDetails>();
  private currentPage = 0;

  ngOnInit(): void {
    this.loadDemands();
  }

  loadDemands(loadMore = false): void {
    if (loadMore && (this.isLoadingMore() || !this.hasMore())) {
      return;
    }

    const page = loadMore ? this.currentPage + 1 : 0;
    if (loadMore) {
      this.isLoadingMore.set(true);
    } else {
      this.isLoading.set(true);
      this.pageError.set('');
    }

    this.demandService.list(page).subscribe({
      next: result => {
        this.currentPage = result.number;
        this.hasMore.set(!result.last);
        this.totalElements.set(result.totalElements);
        this.demands.update(current => {
          const combined = loadMore ? [...current, ...result.content] : result.content;
          return [...combined].sort((a, b) => this.getTimestamp(b) - this.getTimestamp(a));
        });
        this.isLoading.set(false);
        this.isLoadingMore.set(false);
      },
      error: (error: unknown) => {
        this.isLoading.set(false);
        this.isLoadingMore.set(false);
        if (!loadMore) {
          this.pageError.set('No pudimos cargar las solicitudes. Revisa tu conexión e inténtalo otra vez.');
        }
        this.toastService.error(
          loadMore ? 'No se pudieron cargar más solicitudes' : 'No se pudo cargar el muro',
          this.getErrorMessage(error)
        );
      }
    });
  }

  filteredDemands(): Demand[] {
    const search = this.searchTerm.trim().toLocaleLowerCase('es');
    return this.demands().filter(demand => {
      const details = this.detailsById.get(demand.id);
      const matchesSearch = !search || [
        demand.titulo,
        demand.descripcion ?? '',
        demand.usuarioNombre
      ].some(value => value.toLocaleLowerCase('es').includes(search));
      const category = demand.categoria ?? details?.category ?? '';
      const matchesCategory = !this.selectedCategory
        || this.normalize(category) === this.normalize(this.selectedCategory);
      const matchesCampus = !this.selectedCampus || details?.campus === this.selectedCampus;
      const isStillValid = !details || this.getExpiryTimestamp(demand, details) > Date.now();

      return matchesSearch && matchesCategory && matchesCampus && isStillValid;
    });
  }

  openForm(): void {
    this.isFormOpen.set(true);
  }

  closeForm(): void {
    this.isFormOpen.set(false);
  }

  createDemand(details: DemandDetails): void {
    if (this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.demandService.create(details.request).subscribe({
      next: demand => {
        this.detailsById.set(demand.id, details);
        this.demands.update(current => [demand, ...current]);
        this.totalElements.update(total => total + 1);
        this.selectedCategory = '';
        this.selectedCampus = '';
        this.searchTerm = '';
        this.isSubmitting.set(false);
        this.isFormOpen.set(false);
        this.toastService.success(
          'Solicitud publicada',
          'La categoría quedó guardada. Campus y vigencia se conservan mientras esta vista esté abierta.'
        );
      },
      error: (error: unknown) => {
        this.isSubmitting.set(false);
        this.toastService.error('No se pudo publicar la solicitud', this.getErrorMessage(error));
      }
    });
  }

  categoryFor(demand: Demand): string {
    const category = demand.categoria ?? this.detailsById.get(demand.id)?.category;
    if (!category) {
      return 'Sin categoría';
    }
    return this.categories.find(option => this.normalize(option) === this.normalize(category)) ?? category;
  }

  campusFor(demand: Demand): string {
    return this.detailsById.get(demand.id)?.campus ?? 'Campus por confirmar';
  }

  validUntil(demand: Demand): string {
    const details = this.detailsById.get(demand.id);
    if (!details) {
      return 'Vigencia no definida';
    }

    return new Intl.DateTimeFormat('es-PE', {
      day: 'numeric',
      month: 'short'
    }).format(new Date(this.getExpiryTimestamp(demand, details)));
  }

  publishedAt(demand: Demand): string {
    if (!demand.fechaCreacion) {
      return 'Recién publicada';
    }

    const date = new Date(demand.fechaCreacion);
    if (Number.isNaN(date.getTime())) {
      return 'Fecha no disponible';
    }

    return new Intl.DateTimeFormat('es-PE', {
      day: 'numeric',
      month: 'short'
    }).format(date);
  }

  budgetFor(demand: Demand): string {
    if (demand.presupuestoEstimado === null) {
      return 'A convenir';
    }

    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN'
    }).format(demand.presupuestoEstimado);
  }

  initials(name: string): string {
    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map(part => part.charAt(0))
      .join('')
      .toLocaleUpperCase('es');
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedCategory = '';
    this.selectedCampus = '';
  }

  private getTimestamp(demand: Demand): number {
    const timestamp = demand.fechaCreacion ? new Date(demand.fechaCreacion).getTime() : 0;
    return Number.isNaN(timestamp) ? 0 : timestamp;
  }

  private normalize(value: string): string {
    return value.trim().toLocaleUpperCase('es');
  }

  private getExpiryTimestamp(demand: Demand, details: DemandDetails): number {
    return this.getTimestamp(demand) + details.validForDays * 24 * 60 * 60 * 1000;
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      return 'Inicia sesión de nuevo para consultar o publicar solicitudes.';
    }

    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }

    return 'Inténtalo de nuevo en unos momentos.';
  }
}
