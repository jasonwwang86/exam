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
    expect(screen.getByText('系统登录入口')).toBeInTheDocument();
    expect(screen.getByText('输入管理员账号与密码后进入统一工作区，当前登录流程、鉴权逻辑与权限体系保持不变。')).toBeInTheDocument();
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
    mockGet.mockResolvedValueOnce({
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
    mockGet.mockResolvedValueOnce({
      data: {
        monthlyNewExamineeCount: 3,
        monthlyNewQuestionCount: 3,
        monthlyNewPaperCount: 2,
        monthlyActiveExamPlanCount: 0,
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
    expect(screen.getAllByText('系统管理员')).toHaveLength(2);
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
    mockGet
      .mockResolvedValueOnce({
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
      })
      .mockResolvedValueOnce({
        data: {
          monthlyNewExamineeCount: 3,
          monthlyNewQuestionCount: 3,
          monthlyNewPaperCount: 2,
          monthlyActiveExamPlanCount: 0,
        },
      });

    render(<App />);

    expect(await screen.findByRole('link', { name: '管理首页' })).toHaveAttribute('href', '/dashboard');
    expect(screen.queryByRole('heading', { name: '管理员登录' })).not.toBeInTheDocument();
    expect(screen.getAllByText('系统管理员').length).toBeGreaterThan(0);
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
    mockGet.mockResolvedValueOnce({
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
    mockGet.mockResolvedValueOnce({
      data: {
        monthlyNewExamineeCount: 3,
        monthlyNewQuestionCount: 3,
        monthlyNewPaperCount: 2,
        monthlyActiveExamPlanCount: 0,
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

  it('renders exam plan module entry on the unified main page and can open the route', async () => {
    window.localStorage.setItem('admin_token', 'token-exam-plan');
    window.history.pushState({}, '', '/exam-plans');
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
            'exam-plan-management:view',
            'exam-plan:read',
            'exam-plan:create',
            'exam-plan:update',
            'exam-plan:range',
            'exam-plan:status',
          ],
          menus: [
            {
              code: 'dashboard:view',
              name: '管理首页',
              path: '/dashboard',
            },
            {
              code: 'exam-plan-management:view',
              name: '考试计划',
              path: '/exam-plans',
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
              name: 'Java 基础考试-上午场',
              paperId: 1,
              paperName: 'Java 基础试卷',
              startTime: '2026-05-01T09:00:00',
              endTime: '2026-05-01T12:00:00',
              effectiveExamineeCount: 2,
              status: 'PUBLISHED',
              updatedAt: '2026-04-22T10:00:00',
            },
          ],
        },
      });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '考试计划' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '考试计划' })).toHaveAttribute('href', '/exam-plans');
    expect(screen.getByRole('link', { name: '考试计划' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByText('Java 基础考试-上午场')).toBeInTheDocument();
  });

  it('shows candidate login form on candidate route when there is no authenticated session', () => {
    window.history.pushState({}, '', '/candidate/login');

    render(<App />);

    expect(screen.getByRole('heading', { name: '考生登录' })).toBeInTheDocument();
    expect(screen.getByText('考生登录入口')).toBeInTheDocument();
    expect(screen.getByLabelText('考生编号')).toBeInTheDocument();
    expect(screen.getByLabelText('身份证号')).toBeInTheDocument();
    expect(screen.queryByText('业务导航')).not.toBeInTheDocument();
  });

  it('validates required candidate login fields before sending request', async () => {
    const user = userEvent.setup();
    window.history.pushState({}, '', '/candidate/login');

    render(<App />);

    await user.click(screen.getByRole('button', { name: '登录并进入待考页' }));

    expect(screen.getByRole('alert')).toHaveTextContent('请输入考生编号和身份证号');
    expect(mockPost).not.toHaveBeenCalled();
  });

  it('logs in candidate, confirms profile, and then shows available exams', async () => {
    const user = userEvent.setup();
    window.history.pushState({}, '', '/candidate/login');
    mockPost
      .mockResolvedValueOnce({
        data: {
          token: 'candidate-token-1',
          tokenType: 'Bearer',
          profileConfirmed: false,
          profile: {
            examineeId: 1,
            examineeNo: 'EX2026001',
            name: '张三',
            maskedIdCardNo: '110101********0011',
          },
        },
      })
      .mockResolvedValueOnce({
        data: {
          token: 'candidate-token-2',
          tokenType: 'Bearer',
          profileConfirmed: true,
          profile: {
            examineeId: 1,
            examineeNo: 'EX2026001',
            name: '张三',
            maskedIdCardNo: '110101********0011',
          },
        },
      });
    mockGet.mockResolvedValueOnce({
      data: [
        {
          planId: 1,
          name: 'Java 基础考试-上午场',
          paperName: 'Java 基础试卷',
          durationMinutes: 120,
          startTime: '2026-05-01T09:00:00',
          endTime: '2026-05-01T12:00:00',
          displayStatus: '待开始',
          remark: '首场安排',
        },
      ],
    });

    render(<App />);

    await user.type(screen.getByLabelText('考生编号'), 'EX2026001');
    await user.type(screen.getByLabelText('身份证号'), '110101199001010011');
    await user.click(screen.getByRole('button', { name: '登录并进入待考页' }));

    expect(await screen.findByRole('heading', { name: '确认身份信息' })).toBeInTheDocument();
    expect(screen.getByText('张三')).toBeInTheDocument();
    expect(screen.getByText('110101********0011')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '确认信息并查看考试' }));

    expect(await screen.findByRole('heading', { name: '可参加考试' })).toBeInTheDocument();
    expect(window.localStorage.getItem('candidate_token')).toBe('candidate-token-2');
    expect(screen.getByText('Java 基础考试-上午场')).toBeInTheDocument();
    expect(screen.getByText('Java 基础试卷')).toBeInTheDocument();
  });

  it('redirects restored unconfirmed candidate session back to confirmation page', async () => {
    window.localStorage.setItem('candidate_token', 'candidate-token-restore');
    window.history.pushState({}, '', '/candidate/exams');
    mockGet.mockResolvedValueOnce({
      data: {
        examineeId: 1,
        examineeNo: 'EX2026001',
        name: '张三',
        maskedIdCardNo: '110101********0011',
        profileConfirmed: false,
        message: '请先确认身份信息后查看可参加考试',
      },
    });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '确认身份信息' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: '可参加考试' })).not.toBeInTheDocument();
  });

  it('shows empty state when confirmed candidate has no available exams', async () => {
    window.localStorage.setItem('candidate_token', 'candidate-token-restore');
    window.history.pushState({}, '', '/candidate/exams');
    mockGet
      .mockResolvedValueOnce({
        data: {
          examineeId: 1,
          examineeNo: 'EX2026001',
          name: '张三',
          maskedIdCardNo: '110101********0011',
          profileConfirmed: true,
          message: '身份信息已确认，可查看可参加考试',
        },
      })
      .mockResolvedValueOnce({
        data: [],
      });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '可参加考试' })).toBeInTheDocument();
    expect(screen.getByText('当前暂无可参加的考试')).toBeInTheDocument();
  });

  it('allows candidate to log out from confirmation page', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem('candidate_token', 'candidate-token-confirm');
    window.localStorage.setItem(
      'candidate_profile',
      JSON.stringify({
        examineeId: 1,
        examineeNo: 'EX2026001',
        name: '张三',
        maskedIdCardNo: '110101********0011',
        profileConfirmed: false,
        message: '请先确认身份信息后查看可参加考试',
      }),
    );
    window.history.pushState({}, '', '/candidate/confirm');

    render(<App />);

    expect(await screen.findByRole('heading', { name: '确认身份信息' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '退出登录' }));

    expect(await screen.findByRole('heading', { name: '考生登录' })).toBeInTheDocument();
    expect(window.localStorage.getItem('candidate_token')).toBeNull();
    expect(window.localStorage.getItem('candidate_profile')).toBeNull();
    expect(window.localStorage.getItem('candidate_exams')).toBeNull();
  });

  it('refreshes candidate exams and overwrites cached exam list', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem('candidate_token', 'candidate-token-exams');
    window.localStorage.setItem(
      'candidate_profile',
      JSON.stringify({
        examineeId: 1,
        examineeNo: 'EX2026001',
        name: '张三',
        maskedIdCardNo: '110101********0011',
        profileConfirmed: true,
        message: '身份信息已确认，可查看可参加考试',
      }),
    );
    window.localStorage.setItem(
      'candidate_exams',
      JSON.stringify([
        {
          planId: 1,
          name: '缓存中的旧考试',
          paperName: '旧试卷',
          durationMinutes: 90,
          startTime: '2026-05-01T09:00:00',
          endTime: '2026-05-01T10:30:00',
          displayStatus: '待开始',
          remark: '旧数据',
        },
      ]),
    );
    window.history.pushState({}, '', '/candidate/exams');
    mockGet.mockResolvedValueOnce({
      data: [
        {
          planId: 2,
          name: 'Java 进阶考试-下午场',
          paperName: 'Java 进阶试卷',
          durationMinutes: 120,
          startTime: '2026-05-02T14:00:00',
          endTime: '2026-05-02T16:00:00',
          displayStatus: '待开始',
          remark: '最新安排',
        },
      ],
    });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '可参加考试' })).toBeInTheDocument();
    expect(screen.getByText('缓存中的旧考试')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '刷新考试列表' }));

    expect(await screen.findByText('Java 进阶考试-下午场')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByText('缓存中的旧考试')).not.toBeInTheDocument();
    });
    expect(window.localStorage.getItem('candidate_exams')).toContain('Java 进阶考试-下午场');
  });

  it('allows candidate to log out from exam list page', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem('candidate_token', 'candidate-token-exams');
    window.localStorage.setItem(
      'candidate_profile',
      JSON.stringify({
        examineeId: 1,
        examineeNo: 'EX2026001',
        name: '张三',
        maskedIdCardNo: '110101********0011',
        profileConfirmed: true,
        message: '身份信息已确认，可查看可参加考试',
      }),
    );
    window.localStorage.setItem(
      'candidate_exams',
      JSON.stringify([
        {
          planId: 1,
          name: 'Java 基础考试-上午场',
          paperName: 'Java 基础试卷',
          durationMinutes: 120,
          startTime: '2026-05-01T09:00:00',
          endTime: '2026-05-01T12:00:00',
          displayStatus: '待开始',
          remark: '首场安排',
        },
      ]),
    );
    window.history.pushState({}, '', '/candidate/exams');

    render(<App />);

    expect(await screen.findByRole('heading', { name: '可参加考试' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '退出登录' }));

    expect(await screen.findByRole('heading', { name: '考生登录' })).toBeInTheDocument();
    expect(window.localStorage.getItem('candidate_token')).toBeNull();
    expect(window.localStorage.getItem('candidate_profile')).toBeNull();
    expect(window.localStorage.getItem('candidate_exams')).toBeNull();
  });

  it('shows enter-answer button only for answerable exams and navigates into answering page', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem('candidate_token', 'candidate-token-exams');
    window.localStorage.setItem(
      'candidate_profile',
      JSON.stringify({
        examineeId: 1,
        examineeNo: 'EX2026001',
        name: '张三',
        maskedIdCardNo: '110101********0011',
        profileConfirmed: true,
        message: '身份信息已确认，可查看可参加考试',
      }),
    );
    window.localStorage.setItem(
      'candidate_exams',
      JSON.stringify([
        {
          planId: 1,
          name: 'Java 在线答题场次',
          paperName: 'Java 基础试卷',
          durationMinutes: 120,
          startTime: '2026-05-01T09:00:00',
          endTime: '2026-05-01T12:00:00',
          displayStatus: '进行中',
          remark: '可作答',
          canEnterAnswering: true,
          answeringStatus: 'IN_PROGRESS',
          remainingSeconds: 2400,
        },
        {
          planId: 2,
          name: '尚未开始考试',
          paperName: '等待试卷',
          durationMinutes: 90,
          startTime: '2026-05-02T09:00:00',
          endTime: '2026-05-02T10:30:00',
          displayStatus: '待开始',
          remark: '不可作答',
          canEnterAnswering: false,
          answeringStatus: 'NOT_STARTED',
          remainingSeconds: null,
        },
      ]),
    );
    window.history.pushState({}, '', '/candidate/exams');
    mockPut.mockResolvedValueOnce({
      data: {
        planId: 1,
        name: 'Java 在线答题场次',
        paperName: 'Java 基础试卷',
        durationMinutes: 120,
        sessionStatus: 'IN_PROGRESS',
        startedAt: '2026-05-01T09:10:00',
        deadlineAt: '2099-05-01T10:00:00',
        remainingSeconds: 2400,
        answeredCount: 0,
        totalQuestionCount: 2,
        questions: [
          {
            paperQuestionId: 1,
            questionId: 1,
            questionNo: 1,
            stem: 'Java 的入口方法是什么？',
            questionTypeName: '单选题',
            answerMode: 'SINGLE_CHOICE',
            answerConfig: {
              options: [
                { key: 'A', content: 'main' },
                { key: 'B', content: 'run' },
              ],
            },
            savedAnswer: null,
            answerStatus: 'UNANSWERED',
          },
          {
            paperQuestionId: 2,
            questionId: 2,
            questionNo: 2,
            stem: '请写出 JVM 的英文全称。',
            questionTypeName: '简答题',
            answerMode: 'TEXT',
            answerConfig: {},
            savedAnswer: null,
            answerStatus: 'UNANSWERED',
          },
        ],
      },
    });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '可参加考试' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '进入答题' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '进入答题', hidden: false })).toBeInTheDocument();
    expect(screen.queryByText('尚未开始考试')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '进入答题' }));

    expect(await screen.findByRole('heading', { name: 'Java 在线答题场次' })).toBeInTheDocument();
    expect(screen.getByText('Java 的入口方法是什么？')).toBeInTheDocument();
    expect(screen.queryAllByRole('button', { name: '进入答题' })).toHaveLength(0);
  });

  it('restores candidate answer session when opening answer route directly', async () => {
    window.localStorage.setItem('candidate_token', 'candidate-token-answer');
    window.localStorage.setItem(
      'candidate_profile',
      JSON.stringify({
        examineeId: 1,
        examineeNo: 'EX2026001',
        name: '张三',
        maskedIdCardNo: '110101********0011',
        profileConfirmed: true,
        message: '身份信息已确认，可查看可参加考试',
      }),
    );
    window.localStorage.setItem(
      'candidate_exams',
      JSON.stringify([
        {
          planId: 1,
          name: 'Java 在线答题场次',
          paperName: 'Java 基础试卷',
          durationMinutes: 120,
          startTime: '2026-05-01T09:00:00',
          endTime: '2026-05-01T12:00:00',
          displayStatus: '进行中',
          remark: '可作答',
          canEnterAnswering: true,
          answeringStatus: 'IN_PROGRESS',
          remainingSeconds: 2400,
        },
      ]),
    );
    window.history.pushState({}, '', '/candidate/exams/1/answer');
    mockPut.mockResolvedValueOnce({
      data: {
        planId: 1,
        name: 'Java 在线答题场次',
        paperName: 'Java 基础试卷',
        durationMinutes: 120,
        sessionStatus: 'IN_PROGRESS',
        startedAt: '2026-05-01T09:10:00',
        deadlineAt: '2099-05-01T10:00:00',
        remainingSeconds: 2100,
        answeredCount: 1,
        totalQuestionCount: 2,
        questions: [
          {
            paperQuestionId: 1,
            questionId: 1,
            questionNo: 1,
            stem: 'Java 的入口方法是什么？',
            questionTypeName: '单选题',
            answerMode: 'SINGLE_CHOICE',
            answerConfig: {
              options: [
                { key: 'A', content: 'main' },
                { key: 'B', content: 'run' },
              ],
            },
            savedAnswer: {
              selectedOption: 'A',
            },
            answerStatus: 'ANSWERED',
          },
          {
            paperQuestionId: 2,
            questionId: 2,
            questionNo: 2,
            stem: '请写出 JVM 的英文全称。',
            questionTypeName: '简答题',
            answerMode: 'TEXT',
            answerConfig: {},
            savedAnswer: null,
            answerStatus: 'UNANSWERED',
          },
        ],
      },
    });

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Java 在线答题场次' })).toBeInTheDocument();
    expect(screen.getByText('当前已答 1 / 2 题')).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'main' })).toBeChecked();
  });

  it('submits exam after candidate confirmation and shows submission result', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem('candidate_token', 'candidate-token-submit');
    window.localStorage.setItem(
      'candidate_profile',
      JSON.stringify({
        examineeId: 1,
        examineeNo: 'EX2026001',
        name: '张三',
        maskedIdCardNo: '110101********0011',
        profileConfirmed: true,
        message: '身份信息已确认，可查看可参加考试',
      }),
    );
    window.localStorage.setItem(
      'candidate_exams',
      JSON.stringify([
        {
          planId: 1,
          name: 'Java 在线答题场次',
          paperName: 'Java 基础试卷',
          durationMinutes: 120,
          startTime: '2026-05-01T09:00:00',
          endTime: '2026-05-01T12:00:00',
          displayStatus: '进行中',
          remark: '可作答',
          canEnterAnswering: true,
          answeringStatus: 'IN_PROGRESS',
          remainingSeconds: 2400,
        },
      ]),
    );
    window.history.pushState({}, '', '/candidate/exams');
    mockPut.mockResolvedValueOnce({
      data: {
        planId: 1,
        name: 'Java 在线答题场次',
        paperName: 'Java 基础试卷',
        durationMinutes: 120,
        sessionStatus: 'IN_PROGRESS',
        startedAt: '2026-05-01T09:10:00',
        deadlineAt: '2099-05-01T10:00:00',
        remainingSeconds: 2400,
        answeredCount: 1,
        totalQuestionCount: 2,
        questions: [
          {
            paperQuestionId: 1,
            questionId: 1,
            questionNo: 1,
            stem: 'Java 的入口方法是什么？',
            questionTypeName: '单选题',
            answerMode: 'SINGLE_CHOICE',
            answerConfig: {
              options: [
                { key: 'A', content: 'main' },
                { key: 'B', content: 'run' },
              ],
            },
            savedAnswer: {
              selectedOption: 'A',
            },
            answerStatus: 'ANSWERED',
          },
          {
            paperQuestionId: 2,
            questionId: 2,
            questionNo: 2,
            stem: '请写出 JVM 的英文全称。',
            questionTypeName: '简答题',
            answerMode: 'TEXT',
            answerConfig: {},
            savedAnswer: null,
            answerStatus: 'UNANSWERED',
          },
        ],
      },
    });
    mockPost.mockResolvedValueOnce({
      data: {
        planId: 1,
        name: 'Java 在线答题场次',
        paperName: 'Java 基础试卷',
        sessionStatus: 'SUBMITTED',
        submissionMethod: 'MANUAL',
        submittedAt: '2026-05-01T09:40:00',
        answeredCount: 1,
        totalQuestionCount: 2,
      },
    });

    render(<App />);

    await user.click(await screen.findByRole('button', { name: '进入答题' }));
    expect(await screen.findByRole('heading', { name: 'Java 在线答题场次' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '提交试卷' }));
    expect(screen.getByText('确认提交试卷后将不能继续作答。')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '确认提交' }));

    expect(await screen.findByText('试卷已提交')).toBeInTheDocument();
    expect(mockPost).toHaveBeenCalledWith(
      '/api/candidate/exams/1/submission',
      {},
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer candidate-token-submit',
        }),
      }),
    );
  });

  it('refreshes candidate exam list after submission so generated score summary becomes visible', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem('candidate_token', 'candidate-token-submit-refresh');
    window.localStorage.setItem(
      'candidate_profile',
      JSON.stringify({
        examineeId: 1,
        examineeNo: 'EX2026001',
        name: '张三',
        maskedIdCardNo: '110101********0011',
        profileConfirmed: true,
        message: '身份信息已确认，可查看可参加考试',
      }),
    );
    window.localStorage.setItem(
      'candidate_exams',
      JSON.stringify([
        {
          planId: 1,
          name: 'Java 在线答题场次',
          paperName: 'Java 基础试卷',
          durationMinutes: 120,
          startTime: '2026-05-01T09:00:00',
          endTime: '2026-05-01T12:00:00',
          displayStatus: '进行中',
          remark: '可作答',
          canEnterAnswering: true,
          answeringStatus: 'IN_PROGRESS',
          remainingSeconds: 2400,
        },
      ]),
    );
    window.history.pushState({}, '', '/candidate/exams');
    mockPut.mockResolvedValueOnce({
      data: {
        planId: 1,
        name: 'Java 在线答题场次',
        paperName: 'Java 基础试卷',
        durationMinutes: 120,
        sessionStatus: 'IN_PROGRESS',
        startedAt: '2026-05-01T09:10:00',
        deadlineAt: '2099-05-01T10:00:00',
        remainingSeconds: 2400,
        answeredCount: 2,
        totalQuestionCount: 2,
        questions: [
          {
            paperQuestionId: 1,
            questionId: 1,
            questionNo: 1,
            stem: 'Java 的入口方法是什么？',
            questionTypeName: '单选题',
            answerMode: 'SINGLE_CHOICE',
            answerConfig: {
              options: [
                { key: 'A', content: 'main' },
                { key: 'B', content: 'run' },
              ],
            },
            savedAnswer: {
              selectedOption: 'A',
            },
            answerStatus: 'ANSWERED',
          },
          {
            paperQuestionId: 2,
            questionId: 2,
            questionNo: 2,
            stem: '请写出 JVM 的英文全称。',
            questionTypeName: '简答题',
            answerMode: 'TEXT',
            answerConfig: {},
            savedAnswer: {
              textAnswer: 'Java Virtual Machine',
            },
            answerStatus: 'ANSWERED',
          },
        ],
      },
    });
    mockPost.mockResolvedValueOnce({
      data: {
        planId: 1,
        name: 'Java 在线答题场次',
        paperName: 'Java 基础试卷',
        sessionStatus: 'SUBMITTED',
        submissionMethod: 'MANUAL',
        submittedAt: '2026-05-01T09:40:00',
        answeredCount: 2,
        totalQuestionCount: 2,
      },
    });
    mockGet.mockResolvedValueOnce({
      data: [
        {
          planId: 1,
          name: 'Java 在线答题场次',
          paperName: 'Java 基础试卷',
          durationMinutes: 120,
          startTime: '2026-05-01T09:00:00',
          endTime: '2026-05-01T12:00:00',
          displayStatus: '已结束',
          remark: '可查看成绩',
          canEnterAnswering: false,
          answeringStatus: 'SUBMITTED',
          remainingSeconds: 0,
          submittedAt: '2026-05-01T09:40:00',
          submissionMethod: 'MANUAL',
          scoreStatus: 'PUBLISHED',
          reportAvailable: true,
          totalScore: 11,
          resultGeneratedAt: '2026-05-01T09:40:05',
        },
      ],
    });

    render(<App />);

    await user.click(await screen.findByRole('button', { name: '进入答题' }));
    expect(await screen.findByRole('heading', { name: 'Java 在线答题场次' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '提交试卷' }));
    await user.click(screen.getByRole('button', { name: '确认提交' }));

    expect(await screen.findByText('试卷已提交')).toBeInTheDocument();

    await user.click(screen.getAllByRole('button', { name: '返回考试列表' })[0]);

    expect(await screen.findByRole('heading', { name: '可参加考试' })).toBeInTheDocument();
    expect(await screen.findByText('总分 11')).toBeInTheDocument();
    expect(screen.getByText('已出分')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '查看成绩' })).toBeInTheDocument();
    expect(mockGet).toHaveBeenCalledWith(
      '/api/candidate/exams',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer candidate-token-submit-refresh',
        }),
      }),
    );
    expect(window.localStorage.getItem('candidate_exams')).toContain('"scoreStatus":"PUBLISHED"');
  });

  it('opens score report from candidate exam list and renders score details', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem('candidate_token', 'candidate-token-report');
    window.localStorage.setItem(
      'candidate_profile',
      JSON.stringify({
        examineeId: 1,
        examineeNo: 'EX2026001',
        name: '张三',
        maskedIdCardNo: '110101********0011',
        profileConfirmed: true,
        message: '身份信息已确认，可查看可参加考试',
      }),
    );
    window.localStorage.setItem(
      'candidate_exams',
      JSON.stringify([
        {
          planId: 1,
          name: 'Java 成绩单场次',
          paperName: 'Java 基础试卷',
          durationMinutes: 120,
          startTime: '2026-05-01T09:00:00',
          endTime: '2026-05-01T12:00:00',
          displayStatus: '已结束',
          remark: '可查看成绩',
          canEnterAnswering: false,
          answeringStatus: 'SUBMITTED',
          remainingSeconds: 0,
          submittedAt: '2026-05-01T09:40:00',
          submissionMethod: 'MANUAL',
          scoreStatus: 'PUBLISHED',
          reportAvailable: true,
          totalScore: 92.5,
          resultGeneratedAt: '2026-05-01T10:00:00',
        },
      ]),
    );
    window.history.pushState({}, '', '/candidate/exams');
    mockGet.mockResolvedValueOnce({
      data: {
        planId: 1,
        name: 'Java 成绩单场次',
        paperName: 'Java 基础试卷',
        durationMinutes: 120,
        remark: '可查看成绩',
        scoreStatus: 'PUBLISHED',
        totalScore: 92.5,
        objectiveScore: 86.5,
        subjectiveScore: 6,
        answeredCount: 2,
        unansweredCount: 0,
        submittedAt: '2026-05-01T09:40:00',
        generatedAt: '2026-05-01T10:00:00',
        publishedAt: '2026-05-01T10:01:00',
        submissionMethod: 'MANUAL',
        items: [
          {
            paperQuestionId: 1,
            questionId: 1,
            questionNo: 1,
            questionStem: 'Java 的入口方法是什么？',
            questionTypeName: '单选题',
            itemScore: 5,
            awardedScore: 5,
            answerStatus: 'ANSWERED',
            answerSummary: '选择 A',
            judgeStatus: 'CORRECT',
          },
        ],
      },
    });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '可参加考试' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '查看成绩' }));

    expect(await screen.findByRole('heading', { name: '成绩详情' })).toBeInTheDocument();
    expect(screen.getByText('Java 成绩单场次')).toBeInTheDocument();
    expect(screen.getByText('总分 92.5')).toBeInTheDocument();
    expect(screen.getByText('Java 的入口方法是什么？')).toBeInTheDocument();
  });

  it('returns to candidate exam list and shows error when score report loading fails', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem('candidate_token', 'candidate-token-report-error');
    window.localStorage.setItem(
      'candidate_profile',
      JSON.stringify({
        examineeId: 1,
        examineeNo: 'EX2026001',
        name: '张三',
        maskedIdCardNo: '110101********0011',
        profileConfirmed: true,
        message: '身份信息已确认，可查看可参加考试',
      }),
    );
    window.localStorage.setItem(
      'candidate_exams',
      JSON.stringify([
        {
          planId: 1,
          name: 'Java 成绩单场次',
          paperName: 'Java 基础试卷',
          durationMinutes: 120,
          startTime: '2026-05-01T09:00:00',
          endTime: '2026-05-01T12:00:00',
          displayStatus: '已结束',
          remark: '可查看成绩',
          canEnterAnswering: false,
          answeringStatus: 'SUBMITTED',
          remainingSeconds: 0,
          submittedAt: '2026-05-01T09:40:00',
          submissionMethod: 'MANUAL',
          scoreStatus: 'PUBLISHED',
          reportAvailable: true,
          totalScore: 92.5,
          resultGeneratedAt: '2026-05-01T10:00:00',
        },
      ]),
    );
    window.history.pushState({}, '', '/candidate/exams');
    mockGet.mockRejectedValueOnce({
      response: {
        data: {
          message: '成绩详情加载失败',
        },
      },
    });

    render(<App />);

    expect(await screen.findByRole('heading', { name: '可参加考试' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '查看成绩' }));

    expect(await screen.findByRole('heading', { name: '可参加考试' })).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('成绩详情加载失败');
  });
});
