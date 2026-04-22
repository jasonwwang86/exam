import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from './App';

const { mockPost, mockGet, mockPut, mockDelete, mockPatch } = vi.hoisted(() => ({
  mockPost: vi.fn(),
  mockGet: vi.fn(),
  mockPut: vi.fn(),
  mockDelete: vi.fn(),
  mockPatch: vi.fn(),
}));

vi.mock('axios', () => ({
  default: {
    create: () => ({
      post: mockPost,
      get: mockGet,
      put: mockPut,
      delete: mockDelete,
      patch: mockPatch,
    }),
  },
}));

describe('App', () => {
  beforeEach(() => {
    window.localStorage.clear();
    window.history.pushState({}, '', '/');
    mockPost.mockReset();
    mockGet.mockReset();
    mockPut.mockReset();
    mockDelete.mockReset();
    mockPatch.mockReset();
  });

  it('shows admin login form when there is no authenticated session', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: '管理员登录' })).toBeInTheDocument();
    expect(screen.getByText('Exam Admin')).toBeInTheDocument();
    expect(screen.getByText('输入管理员账号和密码，进入统一工作台。')).toBeInTheDocument();
    expect(screen.getByLabelText('用户名')).toBeInTheDocument();
    expect(screen.getByLabelText('密码')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '登录' })).toBeInTheDocument();
    expect(screen.queryByText('业务导航')).not.toBeInTheDocument();
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
    expect(screen.getByRole('link', { name: '管理首页' })).toHaveAttribute('aria-current', 'page');
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

  it('renders examinee module entry on the unified main page and can open the route', async () => {
    window.localStorage.setItem('admin_token', 'token-examinee');
    window.history.pushState({}, '', '/examinees');
    mockGet
      .mockResolvedValueOnce({
        data: {
          userId: 1,
          username: 'admin',
          displayName: '系统管理员',
          roles: ['SUPER_ADMIN'],
          permissions: [
            'dashboard:view',
            'dashboard:read',
            'examinee:view',
            'examinee:read',
            'examinee:create',
            'examinee:update',
            'examinee:delete',
            'examinee:status',
            'examinee:import',
            'examinee:export',
          ],
          menus: [
            {
              code: 'dashboard:view',
              name: '管理首页',
              path: '/dashboard',
            },
            {
              code: 'examinee:view',
              name: '考生管理',
              path: '/examinees',
            },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          total: 1,
          page: 1,
          pageSize: 10,
          records: [
            {
              id: 1,
              examineeNo: 'EX2026001',
              name: '张三',
              gender: 'MALE',
              idCardNo: '110101199001010011',
              phone: '13800000001',
              email: 'zhangsan@example.com',
              status: 'ENABLED',
              remark: '首批考生',
              updatedAt: '2026-04-21T10:00:00',
            },
          ],
        },
      });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '考生管理' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '管理首页' })).toHaveAttribute('href', '/dashboard');
    expect(screen.getByRole('link', { name: '考生管理' })).toHaveAttribute('href', '/examinees');
    expect(screen.getByRole('link', { name: '考生管理' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByText('张三')).toBeInTheDocument();
  });

  it('renders question bank module entry on the unified main page and can open the route', async () => {
    window.localStorage.setItem('admin_token', 'token-question-bank');
    window.history.pushState({}, '', '/question-bank');
    mockGet
      .mockResolvedValueOnce({
        data: {
          userId: 1,
          username: 'admin',
          displayName: '系统管理员',
          roles: ['SUPER_ADMIN'],
          permissions: [
            'dashboard:view',
            'dashboard:read',
            'question-bank:view',
            'question:read',
            'question:create',
            'question:update',
            'question:delete',
            'question-type:read',
            'question-type:create',
            'question-type:update',
            'question-type:delete',
          ],
          menus: [
            {
              code: 'dashboard:view',
              name: '管理首页',
              path: '/dashboard',
            },
            {
              code: 'question-bank:view',
              name: '题库管理',
              path: '/question-bank',
            },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: [
          { id: 1, name: '单选题', answerMode: 'SINGLE_CHOICE', sort: 10, remark: '唯一正确答案' },
        ],
      })
      .mockResolvedValueOnce({
        data: {
          total: 1,
          page: 1,
          pageSize: 10,
          records: [
            {
              id: 1,
              stem: 'Java 的入口方法是什么？',
              questionTypeId: 1,
              questionTypeName: '单选题',
              difficulty: 'EASY',
              score: 5,
              updatedAt: '2026-04-22T10:00:00',
            },
          ],
        },
      });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '题库管理' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '题库管理' })).toHaveAttribute('href', '/question-bank');
    expect(screen.getByRole('link', { name: '题库管理' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByText('Java 的入口方法是什么？')).toBeInTheDocument();
  });

  it('renders paper management module entry on the unified main page and can open the route', async () => {
    window.localStorage.setItem('admin_token', 'token-paper');
    window.history.pushState({}, '', '/papers');
    mockGet
      .mockResolvedValueOnce({
        data: {
          userId: 1,
          username: 'admin',
          displayName: '系统管理员',
          roles: ['SUPER_ADMIN'],
          permissions: [
            'dashboard:view',
            'dashboard:read',
            'paper-management:view',
            'paper:read',
            'paper:create',
            'paper:update',
            'paper:delete',
            'paper-question:read',
            'paper-question:create',
            'paper-question:update',
            'paper-question:delete',
          ],
          menus: [
            {
              code: 'dashboard:view',
              name: '管理首页',
              path: '/dashboard',
            },
            {
              code: 'paper-management:view',
              name: '试卷管理',
              path: '/papers',
            },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          total: 1,
          page: 1,
          pageSize: 10,
          records: [
            {
              id: 1,
              name: 'Java 基础试卷',
              totalScore: 11,
              durationMinutes: 120,
              questionCount: 2,
              updatedAt: '2026-04-22T10:00:00',
            },
          ],
        },
      });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '试卷管理' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '试卷管理' })).toHaveAttribute('href', '/papers');
    expect(screen.getByRole('link', { name: '试卷管理' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByText('Java 基础试卷')).toBeInTheDocument();
  });
});
