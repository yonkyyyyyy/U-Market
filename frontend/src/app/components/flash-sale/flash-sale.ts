import { Component, OnDestroy, computed, effect, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { FlashSaleStockService } from '../../services/flash-sale-stock.service';
import { Product, ProductService } from '../../services/product.service';

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
