import { Component } from '@angular/core';

interface Product {
  id: number;
  image: string;
  name: string;
  price: string;
  seller: string;
}

@Component({
  selector: 'app-product-carousel',
  imports: [],
  templateUrl: './product-carousel.html',
  styleUrl: './product-carousel.css'
})
export class ProductCarousel {

  currentSlide = 0;

  products: Product[] = [
    {
      id: 1,
      image: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3',
      name: 'Laptop para estudiante',
      price: 'S/ 1,200',
      seller: 'Carlos Mendoza'
    },
    {
      id: 2,
      image: 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed',
      name: 'Laptop HP',
      price: 'S/ 950',
      seller: 'Ana Torres'
    },
    {
      id: 3,
      image: 'https://images.unsplash.com/photo-1541807084-5c52b6b3adef',
      name: 'MacBook Air',
      price: 'S/ 2,500',
      seller: 'Luis Ramírez'
    },
    {
      id: 4,
      image: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853',
      name: 'Laptop Lenovo',
      price: 'S/ 1,100',
      seller: 'María López'
    }
  ];

  nextSlide(): void {
    this.currentSlide =
      (this.currentSlide + 1) % this.products.length;
  }

  previousSlide(): void {
    this.currentSlide =
      (this.currentSlide - 1 + this.products.length) %
      this.products.length;
  }

  goToSlide(index: number): void {
    this.currentSlide = index;
  }
}