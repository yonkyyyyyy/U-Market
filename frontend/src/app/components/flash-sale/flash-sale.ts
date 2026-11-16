import { Component, computed, inject } from '@angular/core';
import { Product, ProductService } from '../../services/product.service';

@Component({
  selector: 'app-flash-sale',
  imports: [],
  templateUrl: './flash-sale.html',
  styleUrl: './flash-sale.css'
})
export class FlashSale {
  private readonly productService = inject(ProductService);
  readonly offers = computed(() => this.productService.products()
    .filter((product): product is Product & { ofertaFlash: NonNullable<Product['ofertaFlash']> } =>
      product.ofertaFlash !== undefined));

  formatPrice(price: number): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN'
    }).format(price);
  }
}
