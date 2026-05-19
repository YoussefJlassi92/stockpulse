import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CurrencyPipe, DecimalPipe, NgClass } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { PortfolioService } from '../../core/services/portfolio.service';
import { AlertService } from '../../core/services/alert.service';
import { AuthService } from '../../core/services/auth.service';
import { Portfolio, Position } from '../../core/models/portfolio.model';
import { AlertRule } from '../../core/models/alert.model';

interface PositionRow extends Position {
  pnl: number;
  pnlPercent: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CurrencyPipe,
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
export class DashboardComponent implements OnInit {
  private readonly portfolioService = inject(PortfolioService);
  private readonly alertService = inject(AlertService);
  private readonly authService = inject(AuthService);

  readonly portfolios = signal<Portfolio[]>([]);
  readonly positions = signal<PositionRow[]>([]);
  readonly alerts = signal<AlertRule[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly totalValue = computed(() =>
    this.positions().reduce((sum, p) => sum + p.quantity * p.currentPrice, 0)
  );

  readonly totalPnL = computed(() =>
    this.positions().reduce((sum, p) => sum + p.pnl, 0)
  );

  readonly activeAlerts = computed(() =>
    this.alerts().filter((a) => a.active).length
  );

  readonly displayedColumns = [
    'symbol', 'quantity', 'avgBuyPrice', 'currentPrice', 'pnl', 'pnlPercent',
  ];

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;

    this.portfolioService
      .getPortfolios(userId)
      .pipe(
        switchMap((portfolios) => {
          this.portfolios.set(portfolios);
          const positions$ =
            portfolios.length > 0
              ? forkJoin(portfolios.map((p) => this.portfolioService.getPositions(p.id)))
              : of<Position[][]>([]);
          return forkJoin({ positions: positions$, alerts: this.alertService.getAlerts(userId) });
        })
      )
      .subscribe({
        next: ({ positions, alerts }) => {
          const flat = positions.flat().map((p): PositionRow => ({
            ...p,
            pnl: p.quantity * (p.currentPrice - p.avgBuyPrice),
            pnlPercent: ((p.currentPrice - p.avgBuyPrice) / p.avgBuyPrice) * 100,
          }));
          this.positions.set(flat);
          this.alerts.set(alerts);
          this.loading.set(false);
        },
        error: (err: { message?: string }) => {
          this.error.set(err.message ?? 'Failed to load dashboard data');
          this.loading.set(false);
        },
      });
  }
}
