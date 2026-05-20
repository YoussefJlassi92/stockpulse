import { Component, computed, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, rxResource } from '@angular/core/rxjs-interop';
import { httpResource } from '@angular/common/http';
import { CurrencyPipe, DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, map, of } from 'rxjs';
import { PortfolioService } from '../../core/services/portfolio.service';
import { AuthService } from '../../core/services/auth.service';
import { WebsocketService } from '../../core/services/websocket.service';
import { Portfolio, Position } from '../../core/models/portfolio.model';
import { AlertRule } from '../../core/models/alert.model';
import { PORTFOLIOS, ALERTS } from '../../core/services/api.config';

interface PositionRow extends Position {
  pnl: number;
  pnlPercent: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    NgClass,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  private readonly portfolioService = inject(PortfolioService);
  private readonly authService = inject(AuthService);
  private readonly wsService = inject(WebsocketService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly userId = signal(this.authService.getUserId() ?? '');

  readonly portfoliosResource = httpResource<Portfolio[]>(
    () => this.userId() ? `${PORTFOLIOS}/user/${this.userId()}` : undefined
  );

  readonly alertsResource = httpResource<AlertRule[]>(
    () => this.userId() ? `${ALERTS}/${this.userId()}` : undefined
  );

  readonly positionsResource = rxResource<PositionRow[], Portfolio[] | undefined>({
    params: () => this.portfoliosResource.value(),
    stream: ({ params: portfolios }) => {
      if (!portfolios?.length) return of<PositionRow[]>([]);
      return forkJoin(portfolios.map((p) => this.portfolioService.getPositions(p.id))).pipe(
        map((results: Position[][]) =>
          results.flat().map((p): PositionRow => ({
            ...p,
            pnl: p.quantity * (p.currentPrice - p.avgBuyPrice),
            pnlPercent: ((p.currentPrice - p.avgBuyPrice) / p.avgBuyPrice) * 100,
          }))
        )
      );
    },
  });

  private readonly wsOverrides = signal<Map<number, number>>(new Map());
  readonly lastUpdated = signal<Date>(new Date());

  readonly positions = computed(() => {
    const base = this.positionsResource.value() ?? [];
    const overrides = this.wsOverrides();
    if (!overrides.size) return base;
    return base.map((row) => {
      const price = overrides.get(row.id);
      if (price === undefined) return row;
      return {
        ...row,
        currentPrice: price,
        pnl: row.quantity * (price - row.avgBuyPrice),
        pnlPercent: ((price - row.avgBuyPrice) / row.avgBuyPrice) * 100,
      };
    });
  });

  readonly loading = computed(
    () =>
      this.portfoliosResource.isLoading() ||
      this.positionsResource.isLoading() ||
      this.alertsResource.isLoading()
  );

  readonly error = computed(() => {
    const e =
      this.portfoliosResource.error() ??
      this.positionsResource.error() ??
      this.alertsResource.error();
    return e instanceof Error ? e.message : e ? String(e) : null;
  });

  readonly totalValue = computed(() =>
    this.positions().reduce((sum, p) => sum + p.quantity * p.currentPrice, 0)
  );

  readonly totalPnL = computed(() =>
    this.positions().reduce((sum, p) => sum + p.pnl, 0)
  );

  readonly activeAlerts = computed(() =>
    (this.alertsResource.value() ?? []).filter((a) => a.active).length
  );

  readonly wsConnected = computed(() => this.wsService.connected());

  readonly displayedColumns = [
    'symbol', 'quantity', 'avgBuyPrice', 'currentPrice', 'pnl', 'pnlPercent',
  ];

  constructor() {
    this.destroyRef.onDestroy(() => this.wsService.disconnect());

    effect(() => {
      const portfolios = this.portfoliosResource.value();
      if (!portfolios?.length) return;

      untracked(() => {
        const token = this.authService.getToken();
        if (!token) return;

        this.wsService.connect(token);
        portfolios.forEach((portfolio) => {
          this.wsService
            .subscribeToPortfolioUpdates(portfolio.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((updated) => {
              this.wsOverrides.update((map) => {
                const next = new Map(map);
                next.set(updated.id, updated.currentPrice);
                return next;
              });
              this.lastUpdated.set(new Date());
            });
        });
      });
    });
  }
}
