import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../environments/environment';

@Injectable({ providedIn: 'root' })
export class KeycloakService {
  private kc = new Keycloak(environment.keycloak);

  async init(): Promise<void> {
    await this.kc.init({
      onLoad: 'login-required',
      checkLoginIframe: false,
    });
  }

  get username(): string {
    return this.kc.tokenParsed?.['preferred_username'] ?? '';
  }

  get role(): string {
    const roles: string[] = this.kc.tokenParsed?.['realm_access']?.['roles'] ?? [];
    const appRoles = ['executive', 'hr', 'manager', 'employee'];
    return roles.find((r) => appRoles.includes(r)) ?? '';
  }

  get isAdmin(): boolean {
    const roles: string[] = this.kc.tokenParsed?.['realm_access']?.['roles'] ?? [];
    return roles.includes('admin');
  }

  get token(): string {
    return this.kc.token ?? '';
  }

  // Refresh the token if it expires within the next 30 seconds
  async updateToken(): Promise<void> {
    await this.kc.updateToken(30);
  }

  logout(): void {
    if (this.kc.idToken) {
      this.kc.logout({ redirectUri: window.location.origin });
    } else {
      // idToken unavailable (e.g. after a silent refresh) — Keycloak 18+ rejects
      // an empty id_token_hint, so build the end-session URL with client_id instead.
      const base = `${environment.keycloak.url}/realms/${environment.keycloak.realm}/protocol/openid-connect/logout`;
      const params = new URLSearchParams({
        client_id: environment.keycloak.clientId,
        post_logout_redirect_uri: window.location.origin,
      });
      window.location.href = `${base}?${params}`;
    }
  }
}
