export interface UserResponse {
  id: string;
  keycloakSubject: string;
  username: string;
  email: string;
  displayName?: string;
  active: boolean;
  createdAt: string;
  lastSyncedAt: string;
}

export interface RoleResponse {
  code: string;
  displayName: string;
  description?: string;
}

export interface UserRoleResponse {
  userId: string;
  roleCode: string;
  grantedAt: string;
  grantedBy?: string;
}

export interface SyncResponse {
  created: number;
  updated: number;
  totalFetched: number;
}

export interface AppSettingsResponse {
  locale: string;
  timezone: string;
  currency: string;
  updatedAt: string;
  updatedBy?: string;
}

export interface UpdateAppSettingsRequest {
  locale: string;
  timezone: string;
  currency: string;
}
