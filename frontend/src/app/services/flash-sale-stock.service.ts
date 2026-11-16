import { Injectable } from '@angular/core';
import { filter, Observable, Subject } from 'rxjs';

export interface FlashStockUpdate {
  channel: string;
  productId: number;
  cuposDisponibles: number;
}

export function flashStockChannel(productId: number): string {
  return `stock.${productId}`;
}

@Injectable({
  providedIn: 'root'
})
export class FlashSaleStockService {
  private readonly updates = new Subject<FlashStockUpdate>();

  watchStockChannel(productId: number): Observable<FlashStockUpdate> {
    const channel = flashStockChannel(productId);
    return this.updates.pipe(filter(update => update.channel === channel));
  }

  acceptStockUpdate(update: FlashStockUpdate): void {
    if (!Number.isInteger(update.productId) || update.productId <= 0) {
      throw new RangeError('El identificador del producto debe ser un entero positivo.');
    }
    if (!Number.isInteger(update.cuposDisponibles) || update.cuposDisponibles < 0) {
      throw new RangeError('Los cupos disponibles deben ser un entero no negativo.');
    }
    if (update.channel !== flashStockChannel(update.productId)) {
      throw new Error('El canal de stock no coincide con el identificador del producto.');
    }

    this.updates.next(update);
  }
}
