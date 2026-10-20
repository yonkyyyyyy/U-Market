import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { Navbar } from '../../components/navbar/navbar';
import {
  AdminDashboardService,
  ModerationDemand,
  ModerationMetrics
} from '../../services/admin-dashboard.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CommonModule, Navbar],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css'
})
export class AdminDashboard implements OnInit {
  private readonly adminDashboardService = inject(AdminDashboardService);
  private readonly toastService = inject(ToastService);

  metrics: ModerationMetrics | null = null;
  demands: ModerationDemand[] = [];
  loading = true;
  blockingIds = new Set<number>();

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;

    this.adminDashboardService.getMetrics().subscribe({
      next: (response) => {
        this.metrics = response;
      },
      error: () => {
        this.toastService.error('No se pudieron cargar las métricas', 'Revisa la conexión con el backend.');
      }
    });

    this.adminDashboardService.getDemands(0, 10).subscribe({
      next: (response) => {
        this.demands = response.content ?? [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.error('No se pudieron cargar las demandas', 'Intenta nuevamente más tarde.');
      }
    });
  }

  blockDemand(demand: ModerationDemand): void {
    this.blockingIds.add(demand.id);

    this.adminDashboardService.blockDemand(demand.id, 'Se bloquea la demanda por revisión del equipo de moderación.').subscribe({
      next: () => {
        this.blockingIds.delete(demand.id);
        this.demands = this.demands.filter(item => item.id !== demand.id);
        this.toastService.success('Demanda bloqueada', 'La publicación quedó ocultada para revisión.');
      },
      error: () => {
        this.blockingIds.delete(demand.id);
        this.toastService.error('No se pudo bloquear la demanda', 'El backend rechazó la acción.');
      }
    });
  }

  formatPercent(value: number | undefined): string {
    if (value === undefined || Number.isNaN(value)) {
      return '0%';
    }

    return `${(value * 100).toFixed(1)}%`;
  }
}
