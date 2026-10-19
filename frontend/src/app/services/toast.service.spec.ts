import { TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
  });

  afterEach(() => {
    for (const message of service.messages()) {
      service.dismiss(message.id);
    }
  });

  it('adds typed notices and removes a dismissed notice', () => {
    service.success('Solicitud publicada');
    service.error('No se pudo cargar', 'Inténtalo de nuevo.');

    const [success, error] = service.messages();
    expect(success.kind).toBe('success');
    expect(error.kind).toBe('error');
    expect(error.description).toBe('Inténtalo de nuevo.');

    service.dismiss(success.id);
    expect(service.messages()).toEqual([error]);
  });
});
