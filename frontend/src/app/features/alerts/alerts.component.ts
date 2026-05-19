import { Component, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CurrencyPipe, NgClass } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AlertService } from '../../core/services/alert.service';
import { AuthService } from '../../core/services/auth.service';
import { AlertRule, AlertType, CreateAlertRequest } from '../../core/models/alert.model';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CurrencyPipe,
    NgClass,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './alerts.component.html',
  styleUrl: './alerts.component.scss',
})
export class AlertsComponent implements OnInit {
  private readonly alertService = inject(AlertService);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly alerts = signal<AlertRule[]>([]);
  readonly loading = signal(true);
  readonly showCreateForm = signal(false);

  readonly displayedColumns = ['symbol', 'alertType', 'thresholdPrice', 'email', 'status', 'actions'];

  readonly createForm = this.fb.group({
    symbol: ['', [Validators.required]],
    thresholdPrice: [null as number | null, [Validators.required, Validators.min(0)]],
    alertType: ['' as AlertType | '', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
  });

  ngOnInit(): void {
    this.loadAlerts();
  }

  onCreateAlert(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    const userId = this.authService.getUserId();
    if (!userId) return;

    const { symbol, thresholdPrice, alertType, email } = this.createForm.getRawValue();
    const request: CreateAlertRequest = {
      userId,
      symbol: symbol!.toUpperCase(),
      thresholdPrice: thresholdPrice!,
      alertType: alertType as AlertType,
      email: email!,
    };

    this.alertService.createAlert(request).subscribe({
      next: () => {
        this.showCreateForm.set(false);
        this.createForm.reset();
        this.loadAlerts();
        this.snackBar.open('Alert created successfully', 'Close', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Failed to create alert', 'Close', { duration: 3000 });
      },
    });
  }

  onDeleteAlert(alertId: number): void {
    this.alertService.deleteAlert(alertId).subscribe({
      next: () => {
        this.loadAlerts();
        this.snackBar.open('Alert deleted', 'Close', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Failed to delete alert', 'Close', { duration: 3000 });
      },
    });
  }

  getAlertStatusClass(alert: AlertRule): string {
    return alert.active ? 'active' : 'inactive';
  }

  private loadAlerts(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;

    this.loading.set(true);
    this.alertService.getAlerts(userId).subscribe({
      next: (alerts) => {
        this.alerts.set(alerts);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
