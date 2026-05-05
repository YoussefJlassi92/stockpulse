export type AlertType = 'ABOVE' | 'BELOW';

export interface AlertRule {
  id: number;
  userId: string;
  symbol: string;
  thresholdPrice: number;
  alertType: AlertType;
  email: string;
  active: boolean;
  createdAt: string;
}

export interface CreateAlertRequest {
  userId: string;
  symbol: string;
  thresholdPrice: number;
  alertType: AlertType;
  email: string;
}
