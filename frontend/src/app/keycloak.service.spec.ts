import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { KeycloakService } from './keycloak.service';

const mockKc = {
  token: 'mock-token' as string | undefined,
  tokenParsed: null as Record<string, unknown> | null | undefined,
  init: vi.fn().mockResolvedValue(true),
  updateToken: vi.fn().mockResolvedValue(true),
  logout: vi.fn(),
};

vi.mock('keycloak-js', () => ({
  default: vi.fn(() => mockKc),
}));

describe('KeycloakService', () => {
  let service: KeycloakService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(KeycloakService);
    // Reset mock state before each test
    mockKc.token = 'mock-token';
    mockKc.tokenParsed = null;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('username', () => {
    it('returns preferred_username from tokenParsed', () => {
      mockKc.tokenParsed = { preferred_username: 'alice', realm_access: { roles: [] } };
      expect(service.username).toBe('alice');
    });

    it('returns empty string when tokenParsed is null', () => {
      mockKc.tokenParsed = null;
      expect(service.username).toBe('');
    });
  });

  describe('role', () => {
    it('returns the first matching app role in priority order', () => {
      mockKc.tokenParsed = { realm_access: { roles: ['offline_access', 'executive', 'admin'] } };
      expect(service.role).toBe('executive');
    });

    it('returns lower-priority role when only that role is present', () => {
      mockKc.tokenParsed = { realm_access: { roles: ['employee'] } };
      expect(service.role).toBe('employee');
    });

    it('returns empty string when no app role is present', () => {
      mockKc.tokenParsed = { realm_access: { roles: ['offline_access', 'default-roles-rag'] } };
      expect(service.role).toBe('');
    });

    it('returns empty string when tokenParsed is null', () => {
      mockKc.tokenParsed = null;
      expect(service.role).toBe('');
    });
  });

  describe('isAdmin', () => {
    it('returns true when admin role is present', () => {
      mockKc.tokenParsed = { realm_access: { roles: ['executive', 'admin'] } };
      expect(service.isAdmin).toBe(true);
    });

    it('returns false when admin role is absent', () => {
      mockKc.tokenParsed = { realm_access: { roles: ['employee'] } };
      expect(service.isAdmin).toBe(false);
    });

    it('returns false when tokenParsed is null', () => {
      mockKc.tokenParsed = null;
      expect(service.isAdmin).toBe(false);
    });
  });

  describe('token', () => {
    it('returns the current JWT token string', () => {
      mockKc.token = 'eyJhbGciOiJSUzI1NiJ9.payload.signature';
      expect(service.token).toBe('eyJhbGciOiJSUzI1NiJ9.payload.signature');
    });

    it('returns empty string when token is undefined', () => {
      mockKc.token = undefined;
      expect(service.token).toBe('');
    });
  });
});
