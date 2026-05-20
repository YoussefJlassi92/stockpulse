import { Component, inject, signal, computed } from '@angular/core';
import { httpResource } from '@angular/common/http';
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
import { ALERTS } from '../../core/services/api.config';

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
export class AlertsComponent {
  private readonly alertService = inject(AlertService);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  private readonly userId = signal(this.authService.getUserId() ?? '');

  readonly alertsResource = httpResource<AlertRule[]>(
    () => this.userId() ? `${ALERTS}/${this.userId()}` : undefined
  );

  readonly alerts = computed(() => this.alertsResource.value() ?? []);
  readonly loading = computed(() => this.alertsResource.isLoading());
  readonly showCreateForm = signal(false);

  readonly displayedColumns = ['symbol', 'alertType', 'thresholdPrice', 'email', 'status', 'actions'];

  readonly createForm = this.fb.group({
    symbol: ['', [Validators.required]],
    thresholdPrice: [null as number | null, [Validators.required, Validators.min(0)]],
    alertType: ['' as AlertType | '', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
  });

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
        this.alertsResource.reload();
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
        this.alertsResource.reload();
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
}
