import { Component } from '@angular/core';
import { Navbar } from '../../components/navbar/navbar';
import { ProductCarousel } from '../../components/product-carousel/product-carousel';
import { CreatePost } from '../../components/create-post/create-post';

@Component({
  selector: 'app-publicaciones',
  imports: [
    Navbar,
    ProductCarousel,
    CreatePost
  ],
  templateUrl: './publicaciones.html',
  styleUrl: './publicaciones.css'
})
export class Publicaciones {

}