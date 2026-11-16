import { Component } from '@angular/core';
import { Navbar } from '../../components/navbar/navbar';
import { ProductCarousel } from '../../components/product-carousel/product-carousel';
import { CreatePost } from '../../components/create-post/create-post';
import { DemandWall } from '../../components/demand-wall/demand-wall';
import { FlashSale } from '../../components/flash-sale/flash-sale';

@Component({
  selector: 'app-publicaciones',
  imports: [
    Navbar,
    ProductCarousel,
    FlashSale,
    CreatePost,
    DemandWall
  ],
  templateUrl: './publicaciones.html',
  styleUrl: './publicaciones.css'
})
export class Publicaciones {

}