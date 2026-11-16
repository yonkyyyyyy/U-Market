import { FlashSaleStockService, flashStockChannel } from './flash-sale-stock.service';

describe('FlashSaleStockService', () => {
  it('publishes updates only to subscribers of the matching stock channel', () => {
    const service = new FlashSaleStockService();
    const received: number[] = [];

    service.watchStockChannel(1).subscribe(update => received.push(update.cuposDisponibles));
    service.watchStockChannel(2).subscribe(update => received.push(update.cuposDisponibles + 100));

    service.acceptStockUpdate({
      channel: flashStockChannel(1),
      productId: 1,
      cuposDisponibles: 3
    });

    expect(received).toEqual([3]);
  });

  it('rejects invalid stock updates instead of publishing them', () => {
    const service = new FlashSaleStockService();

    expect(() => service.acceptStockUpdate({
      channel: 'stock.1',
      productId: 1,
      cuposDisponibles: -1
    })).toThrow(RangeError);
  });
});
