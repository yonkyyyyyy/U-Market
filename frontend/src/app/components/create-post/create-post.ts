import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../services/product.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-create-post',
  imports: [FormsModule],
  templateUrl: './create-post.html',
  styleUrl: './create-post.css'
})
export class CreatePost {
  private readonly productService = inject(ProductService);
  private readonly toastService = inject(ToastService);

  title = '';
  description = '';
  price: number | null = null;
  isPublishing = false;
  publishError = '';

  publish(): void {
    const title = this.title.trim();
    const price = Number(this.price);

    if (!title || this.price === null || !Number.isFinite(price) || price <= 0) {
      this.publishError = 'Escribe un título y un precio mayor que cero.';
      return;
    }

    this.isPublishing = true;
    this.publishError = '';

    this.productService.create({
      titulo: title,
      descripcion: this.description.trim(),
      precio: price
    }).subscribe({
      next: () => {
        this.title = '';
        this.description = '';
        this.price = null;
        this.isPublishing = false;
        this.toastService.success('Publicación creada', 'Tu producto ya aparece en el muro.');
      },
      error: (error: unknown) => {
        this.isPublishing = false;
        this.publishError = this.getErrorMessage(error);
      }
    });
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      return 'Inicia sesión para publicar un producto.';
    }

    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }

    return 'No se pudo publicar el producto. Revisa tu conexión e inténtalo de nuevo.';
  }
}
