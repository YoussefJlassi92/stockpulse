export interface LoginRequest {
  userId: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: string;
  expiresIn: number;
}

export interface AuthState {
  token: string | null;
  userId: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}
