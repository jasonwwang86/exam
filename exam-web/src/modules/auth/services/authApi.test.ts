import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockPost, mockGet, mockRandomUuid } = vi.hoisted(() => ({
  mockPost: vi.fn(),
  mockGet: vi.fn(),
  mockRandomUuid: vi.fn(),
}));

vi.mock('axios', () => ({
  default: {
    create: () => ({
      post: mockPost,
      get: mockGet,
    }),
  },
}));

describe('authApi TraceNo headers', () => {
  beforeEach(() => {
    mockPost.mockReset();
    mockGet.mockReset();
    mockRandomUuid.mockReset();
    mockRandomUuid.mockReturnValue('123e4567-e89b-12d3-a456-426614174000');
    vi.stubGlobal('crypto', {
      randomUUID: mockRandomUuid,
    });
  });

  it('sends TraceNo header when logging in', async () => {
    mockPost.mockResolvedValue({
      data: {
        token: 'token-123',
        tokenType: 'Bearer',
        user: {
          userId: 1,
          username: 'admin',
          displayName: '系统管理员',
        },
      },
    });

    const { login } = await import('./authApi');

    await login({
      username: 'admin',
      password: 'Admin@123456',
    });

    expect(mockPost).toHaveBeenCalledWith(
      '/api/admin/auth/login',
      {
        username: 'admin',
        password: 'Admin@123456',
      },
      {
        headers: {
          TraceNo: '123e4567e89b12d3a456426614174000',
        },
      },
    );
  });

  it('sends TraceNo and Authorization headers when fetching current user', async () => {
    mockGet.mockResolvedValue({
      data: {
        userId: 1,
        username: 'admin',
        displayName: '系统管理员',
        roles: [],
        permissions: [],
        menus: [],
      },
    });

    const { fetchCurrentUser } = await import('./authApi');

    await fetchCurrentUser('token-123');

    expect(mockGet).toHaveBeenCalledWith('/api/admin/auth/me', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });

  it('sends TraceNo and Authorization headers when logging out', async () => {
    mockPost.mockResolvedValue({ data: { success: true } });

    const { logout } = await import('./authApi');

    await logout('token-123');

    expect(mockPost).toHaveBeenCalledWith(
      '/api/admin/auth/logout',
      {},
      {
        headers: {
          Authorization: 'Bearer token-123',
          TraceNo: '123e4567e89b12d3a456426614174000',
        },
      },
    );
  });
});
