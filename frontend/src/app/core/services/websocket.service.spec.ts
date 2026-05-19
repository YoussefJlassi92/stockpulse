import { TestBed } from '@angular/core/testing';
import { WebsocketService } from './websocket.service';

const { mockClient, MockClient } = vi.hoisted(() => {
  const mockClient = {
    activate: vi.fn(),
    deactivate: vi.fn().mockResolvedValue(undefined),
    subscribe: vi.fn(),
    onConnect: null as ((frame: unknown) => void) | null,
    onDisconnect: null as ((frame: unknown) => void) | null,
  };

  // Must use a regular function (not arrow) so `new MockClient(...)` works.
  // Returning a non-primitive from a constructor returns that object instead of `this`.
  const MockClient = vi.fn().mockImplementation(function (config: {
    onConnect: (frame: unknown) => void;
    onDisconnect: (frame: unknown) => void;
  }) {
    mockClient.onConnect = config.onConnect;
    mockClient.onDisconnect = config.onDisconnect;
    return mockClient;
  });

  return { mockClient, MockClient };
});

vi.mock('@stomp/stompjs', () => ({ Client: MockClient }));

describe('WebsocketService', () => {
  let service: WebsocketService;

  beforeEach(() => {
    mockClient.activate.mockClear();
    mockClient.deactivate.mockClear();
    mockClient.subscribe.mockClear();
    MockClient.mockClear();

    TestBed.configureTestingModule({});
    service = TestBed.inject(WebsocketService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start disconnected', () => {
    expect(service.connected()).toBe(false);
  });

  it('should activate STOMP client on connect', () => {
    service.connect('test-token');
    expect(mockClient.activate).toHaveBeenCalled();
  });

  it('should set connected to true when STOMP onConnect fires', () => {
    service.connect('test-token');
    mockClient.onConnect!({});
    expect(service.connected()).toBe(true);
  });

  it('should set connected to false when STOMP onDisconnect fires', () => {
    service.connect('test-token');
    mockClient.onConnect!({});
    mockClient.onDisconnect!({});
    expect(service.connected()).toBe(false);
  });

  it('should deactivate client on disconnect', () => {
    service.connect('test-token');
    service.disconnect();
    expect(mockClient.deactivate).toHaveBeenCalled();
  });

  it('should set connected to false on disconnect', () => {
    service.connect('test-token');
    mockClient.onConnect!({});
    service.disconnect();
    expect(service.connected()).toBe(false);
  });

  it('should subscribe to correct portfolio topic after connection resolves', async () => {
    const mockSub = { unsubscribe: vi.fn() };
    mockClient.subscribe.mockReturnValue(mockSub);

    service.connect('test-token');
    mockClient.onConnect!({});

    const sub = service.subscribeToPortfolioUpdates(42).subscribe();
    await Promise.resolve();

    expect(mockClient.subscribe).toHaveBeenCalledWith(
      '/topic/portfolio/42',
      expect.any(Function)
    );
    sub.unsubscribe();
    expect(mockSub.unsubscribe).toHaveBeenCalled();
  });

  it('should emit parsed position after connection resolves', async () => {
    const position = { id: 1, symbol: 'AAPL', currentPrice: 210 };
    const mockSub = { unsubscribe: vi.fn() };
    let capturedCallback: ((msg: { body: string }) => void) | null = null;

    mockClient.subscribe.mockImplementation((_topic: string, cb: (msg: { body: string }) => void) => {
      capturedCallback = cb;
      return mockSub;
    });

    service.connect('test-token');
    mockClient.onConnect!({});

    const received: unknown[] = [];
    service.subscribeToPortfolioUpdates(1).subscribe((p) => received.push(p));
    await Promise.resolve();

    capturedCallback!({ body: JSON.stringify(position) });
    expect(received[0]).toEqual(position);
  });

  it('should complete immediately when client is null', () => {
    let completed = false;
    service.subscribeToPortfolioUpdates(1).subscribe({ complete: () => { completed = true; } });
    expect(completed).toBe(true);
  });
});
