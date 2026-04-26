import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '@tmpmgmt/core';

import {
  AppSettingsResponse,
  RoleResponse,
  SyncResponse,
  UpdateAppSettingsRequest,
  UserResponse,
  UserRoleResponse,
} from '../models/admin';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  private get base(): string {
    return this.config.apiBase;
  }

  listUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.base}/users`);
  }

  getUser(id: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.base}/users/${id}`);
  }

  syncUsers(): Observable<SyncResponse> {
    return this.http.post<SyncResponse>(`${this.base}/users/sync`, {});
  }

  listRoles(): Observable<RoleResponse[]> {
    return this.http.get<RoleResponse[]>(`${this.base}/roles`);
  }

  listUserRoles(userId: string): Observable<UserRoleResponse[]> {
    return this.http.get<UserRoleResponse[]>(`${this.base}/users/${userId}/roles`);
  }

  grantRole(userId: string, roleCode: string): Observable<UserRoleResponse> {
    return this.http.post<UserRoleResponse>(
      `${this.base}/users/${userId}/roles/${roleCode}`,
      {},
    );
  }

  revokeRole(userId: string, roleCode: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/users/${userId}/roles/${roleCode}`);
  }

  getSettings(): Observable<AppSettingsResponse> {
    return this.http.get<AppSettingsResponse>(`${this.base}/admin/settings`);
  }

  updateSettings(req: UpdateAppSettingsRequest): Observable<AppSettingsResponse> {
    return this.http.put<AppSettingsResponse>(`${this.base}/admin/settings`, req);
  }
}
