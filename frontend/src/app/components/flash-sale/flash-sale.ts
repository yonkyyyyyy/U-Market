import { Component, OnDestroy, computed, effect, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { FlashSaleStockService } from '../../services/flash-sale-stock.service';
import { FlashSaleOffer, Product, ProductService } from '../../services/product.service';

export type FlashSaleDisplayState = NonNullable<FlashSaleOffer['estado']>;

export function formatCountdown(finalizaEn: string, now = Date.now()): string {
  const endTime = Date.parse(finalizaEn);
  if (Number.isNaN(endTime)) {
    return 'Horario no disponible';
  }
  if (endTime <= now) {
    return 'Finalizada';
  }

  const totalSeconds = Math.ceil((endTime - now) / 1000);
  const days = Math.floor(totalSeconds / 86_400);
  const hours = Math.floor((totalSeconds % 86_400) / 3_600);
  const minutes = Math.floor((totalSeconds % 3_600) / 60);
  const seconds = totalSeconds % 60;
  const time = [hours, minutes, seconds]
    .map(value => value.toString().padStart(2, '0'))
    .join(':');

  return days > 0 ? `${days}d ${time}` : time;
}

@Component({
  selector: 'app-flash-sale',
  imports: [],
  templateUrl: './flash-sale.html',
  styleUrl: './flash-sale.css'
})
export class FlashSale implements OnDestroy {
  private readonly productService = inject(ProductService);
  private readonly stockService = inject(FlashSaleStockService);
  private readonly now = signal(Date.now());
  private readonly timerId = setInterval(() => this.now.set(Date.now()), 250);
  readonly stockOverrides = signal<Record<number, number>>({});
  readonly offers = computed(() => this.productService.products()
    .filter((product): product is Product & { ofertaFlash: NonNullable<Product['ofertaFlash']> } =>
      product.ofertaFlash !== undefined));

  constructor() {
    effect(onCleanup => {
      const subscriptions: Subscription[] = this.offers().map(product =>
        this.stockService.watchStockChannel(product.id).subscribe(update => {
          this.stockOverrides.update(current => ({
            ...current,
            [update.productId]: update.cuposDisponibles
          }));
        }));

      onCleanup(() => subscriptions.forEach(subscription => subscription.unsubscribe()));
    });
  }

  ngOnDestroy(): void {
    clearInterval(this.timerId);
  }

  availableSpots(productId: number, initialSpots: number): number {
    return this.stockOverrides()[productId] ?? initialSpots;
  }

  offerState(product: Product & { ofertaFlash: FlashSaleOffer }): FlashSaleDisplayState {
    const offer = product.ofertaFlash;
    if (offer.estado === 'SOBREVENTA') {
      return 'SOBREVENTA';
    }
    if (offer.estado === 'EXPIRADA' || this.countdown(offer.finalizaEn) === 'Finalizada') {
      return 'EXPIRADA';
    }
    if (offer.estado === 'AGOTADA' || this.availableSpots(product.id, offer.cuposDisponibles) === 0) {
      return 'AGOTADA';
    }
    return 'ACTIVA';
  }

  stateLabel(state: FlashSaleDisplayState): string {
    switch (state) {
      case 'AGOTADA':
        return 'Agotada';
      case 'EXPIRADA':
        return 'Expirada';
      case 'SOBREVENTA':
        return 'Sin cupos al confirmar';
      default:
        return 'Disponible';
    }
  }

  stateMessage(state: FlashSaleDisplayState, message?: string): string {
    if (message) {
      return message;
    }
    switch (state) {
      case 'AGOTADA':
        return 'Ya no quedan cupos disponibles para esta oferta.';
      case 'EXPIRADA':
        return 'El tiempo para aprovechar esta oferta terminó.';
      case 'SOBREVENTA':
        return 'Los cupos se agotaron antes de confirmar la operación.';
      default:
        return '';
    }
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN'
    }).format(price);
  }

  countdown(finalizaEn: string): string {
    return formatCountdown(finalizaEn, this.now());
  }
}
