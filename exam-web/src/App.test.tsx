import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from './App';

const { mockPost, mockGet } = vi.hoisted(() => ({
  mockPost: vi.fn(),
  mockGet: vi.fn(),
}));

vi.mock('axios', () => ({
  default: {
    create: () => ({
      post: mockPost,
      get: mockGet,
    }),
  },
}));

describe('App', () => {
  beforeEach(() => {
    window.localStorage.clear();
    window.history.pushState({}, '', '/');
    mockPost.mockReset();
    mockGet.mockReset();
  });

  it('shows admin login form when there is no authenticated session', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: '管理员登录' })).toBeInTheDocument();
    expect(screen.getByLabelText('用户名')).toBeInTheDocument();
    expect(screen.getByLabelText('密码')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '登录' })).toBeInTheDocument();
  });

  it('stores token and enters dashboard after successful login', async () => {
    const user = userEvent.setup();
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
    mockGet.mockResolvedValue({
      data: {
        userId: 1,
        username: 'admin',
        displayName: '系统管理员',
        roles: ['SUPER_ADMIN'],
        permissions: ['dashboard:view', 'dashboard:read'],
        menus: [
          {
            code: 'dashboard:view',
            name: '管理首页',
            path: '/dashboard',
          },
        ],
      },
    });

    render(<App />);

    await user.type(screen.getByLabelText('用户名'), 'admin');
    await user.type(screen.getByLabelText('密码'), 'Admin@123456');
    await user.click(screen.getByRole('button', { name: '登录' }));

    await waitFor(() => {
      expect(window.localStorage.getItem('admin_token')).toBe('token-123');
    });
    expect(await screen.findByRole('heading', { name: '管理首页' })).toBeInTheDocument();
    expect(screen.getByText('系统管理员')).toBeInTheDocument();
  });

  it('validates required login fields before sending request', async () => {
    const user = userEvent.setup();

    render(<App />);

    await user.click(screen.getByRole('button', { name: '登录' }));

    expect(screen.getByRole('alert')).toHaveTextContent('请输入用户名和密码');
    expect(mockPost).not.toHaveBeenCalled();
  });

  it('restores current user from stored token on app start', async () => {
    window.localStorage.setItem('admin_token', 'token-restore');
    mockGet.mockResolvedValue({
      data: {
        userId: 1,
        username: 'admin',
        displayName: '系统管理员',
        roles: ['SUPER_ADMIN'],
        permissions: ['dashboard:view', 'dashboard:read'],
        menus: [
          {
            code: 'dashboard:view',
            name: '管理首页',
            path: '/dashboard',
          },
        ],
      },
    });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '管理首页' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '管理首页' })).toHaveAttribute('href', '/dashboard');
  });

  it('clears invalid stored token and returns to login page', async () => {
    window.localStorage.setItem('admin_token', 'expired-token');
    mockGet.mockRejectedValue(new Error('unauthorized'));

    render(<App />);

    expect(await screen.findByRole('heading', { name: '管理员登录' })).toBeInTheDocument();
    expect(window.localStorage.getItem('admin_token')).toBeNull();
  });

  it('shows no-permission page when authenticated user opens unauthorized route', async () => {
    window.localStorage.setItem('admin_token', 'token-limited');
    window.history.pushState({}, '', '/dashboard');
    mockGet.mockResolvedValue({
      data: {
        userId: 3,
        username: 'limited-admin',
        displayName: '受限管理员',
        roles: [],
        permissions: [],
        menus: [],
      },
    });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '暂无权限' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: '管理首页' })).not.toBeInTheDocument();
  });

  it('logs out and returns to login page', async () => {
    const user = userEvent.setup();
    mockPost
      .mockResolvedValueOnce({
        data: {
          token: 'token-logout',
          tokenType: 'Bearer',
          user: {
            userId: 1,
            username: 'admin',
            displayName: '系统管理员',
          },
        },
      })
      .mockResolvedValueOnce({ data: { success: true } });
    mockGet.mockResolvedValue({
      data: {
        userId: 1,
        username: 'admin',
        displayName: '系统管理员',
        roles: ['SUPER_ADMIN'],
        permissions: ['dashboard:view', 'dashboard:read'],
        menus: [
          {
            code: 'dashboard:view',
            name: '管理首页',
            path: '/dashboard',
          },
        ],
      },
    });

    render(<App />);

    await user.type(screen.getByLabelText('用户名'), 'admin');
    await user.type(screen.getByLabelText('密码'), 'Admin@123456');
    await user.click(screen.getByRole('button', { name: '登录' }));
    expect(await screen.findByRole('heading', { name: '管理首页' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '退出登录' }));

    expect(await screen.findByRole('heading', { name: '管理员登录' })).toBeInTheDocument();
    expect(window.localStorage.getItem('admin_token')).toBeNull();
  });
});
