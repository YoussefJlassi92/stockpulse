import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CurrencyPipe, DecimalPipe, NgClass } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { PortfolioService } from '../../core/services/portfolio.service';
import { AuthService } from '../../core/services/auth.service';
import { Portfolio, Position, AddPositionRequest } from '../../core/models/portfolio.model';

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CurrencyPipe,
    DecimalPipe,
    NgClass,
    MatCardModule,
    MatSelectModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatIconModule,
  ],
  templateUrl: './portfolio.component.html',
  styleUrl: './portfolio.component.scss',
})
export class PortfolioComponent implements OnInit {
  private readonly portfolioService = inject(PortfolioService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly portfolios = signal<Portfolio[]>([]);
  readonly positions = signal<Position[]>([]);
  readonly selectedPortfolioId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly showAddForm = signal(false);

  readonly totalValue = computed(() =>
    this.positions().reduce((sum, p) => sum + p.quantity * p.currentPrice, 0)
  );

  readonly totalPnL = computed(() =>
    this.positions().reduce((sum, p) => sum + this.calculatePnL(p), 0)
  );

  readonly totalPnLPercent = computed(() => {
    const totalCost = this.positions().reduce(
      (sum, p) => sum + p.quantity * p.avgBuyPrice, 0
    );
    return totalCost > 0 ? (this.totalPnL() / totalCost) * 100 : 0;
  });

  readonly displayedColumns = [
    'symbol', 'quantity', 'avgBuyPrice', 'currentPrice', 'pnl', 'pnlPercent',
  ];

  readonly addForm = this.fb.group({
    symbol: ['', [Validators.required]],
    quantity: [null as number | null, [Validators.required, Validators.min(1)]],
    price: [null as number | null, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;

    this.portfolioService.getPortfolios(userId).subscribe({
      next: (portfolios) => {
        this.portfolios.set(portfolios);
        if (portfolios.length > 0) {
          this.selectedPortfolioId.set(portfolios[0].id);
          this.loadPositions(portfolios[0].id);
        } else {
          this.loading.set(false);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  onPortfolioChange(portfolioId: number): void {
    this.selectedPortfolioId.set(portfolioId);
    this.showAddForm.set(false);
    this.loadPositions(portfolioId);
  }

  onAddPosition(): void {
    const portfolioId = this.selectedPortfolioId();
    if (this.addForm.invalid || portfolioId === null) {
      this.addForm.markAllAsTouched();
      return;
    }

    const { symbol, quantity, price } = this.addForm.getRawValue();
    const request: AddPositionRequest = {
      symbol: symbol!.toUpperCase(),
      quantity: quantity!,
      price: price!,
    };

    this.portfolioService.addPosition(portfolioId, request).subscribe({
      next: () => {
        this.showAddForm.set(false);
        this.addForm.reset();
        this.loadPositions(portfolioId);
      },
    });
  }

  calculatePnL(position: Position): number {
    return (position.currentPrice - position.avgBuyPrice) * position.quantity;
  }

  calculatePnLPercent(position: Position): number {
    return ((position.currentPrice - position.avgBuyPrice) / position.avgBuyPrice) * 100;
  }

  private loadPositions(portfolioId: number): void {
    this.loading.set(true);
    this.portfolioService.getPositions(portfolioId).subscribe({
      next: (positions) => {
        this.positions.set(positions);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
