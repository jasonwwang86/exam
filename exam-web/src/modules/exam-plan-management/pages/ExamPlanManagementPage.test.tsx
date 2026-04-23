import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const {
  mockListExamPlans,
  mockGetExamPlan,
  mockCreateExamPlan,
  mockUpdateExamPlan,
  mockListExamPlanExaminees,
  mockUpdateExamPlanExaminees,
  mockUpdateExamPlanStatus,
  mockListPapers,
  mockListExaminees,
} = vi.hoisted(() => ({
  mockListExamPlans: vi.fn(),
  mockGetExamPlan: vi.fn(),
  mockCreateExamPlan: vi.fn(),
  mockUpdateExamPlan: vi.fn(),
  mockListExamPlanExaminees: vi.fn(),
  mockUpdateExamPlanExaminees: vi.fn(),
  mockUpdateExamPlanStatus: vi.fn(),
  mockListPapers: vi.fn(),
  mockListExaminees: vi.fn(),
}));

vi.mock('../services/examPlanApi', () => ({
  listExamPlans: mockListExamPlans,
  getExamPlan: mockGetExamPlan,
  createExamPlan: mockCreateExamPlan,
  updateExamPlan: mockUpdateExamPlan,
  listExamPlanExaminees: mockListExamPlanExaminees,
  updateExamPlanExaminees: mockUpdateExamPlanExaminees,
  updateExamPlanStatus: mockUpdateExamPlanStatus,
}));

vi.mock('../../paper-management/services/paperApi', () => ({
  listPapers: mockListPapers,
}));

vi.mock('../../examinees/services/examineeApi', () => ({
  listExaminees: mockListExaminees,
}));

describe('ExamPlanManagementPage', () => {
  beforeEach(() => {
    mockListExamPlans.mockReset();
    mockGetExamPlan.mockReset();
    mockCreateExamPlan.mockReset();
    mockUpdateExamPlan.mockReset();
    mockListExamPlanExaminees.mockReset();
    mockUpdateExamPlanExaminees.mockReset();
    mockUpdateExamPlanStatus.mockReset();
    mockListPapers.mockReset();
    mockListExaminees.mockReset();

    mockListExamPlans.mockResolvedValue({
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
          status: 'DRAFT',
          updatedAt: '2026-04-22T10:00:00',
        },
      ],
    });
    mockGetExamPlan.mockResolvedValue({
      id: 1,
      name: 'Java 基础考试-上午场',
      paperId: 1,
      paperName: 'Java 基础试卷',
      paperDurationMinutes: 120,
      startTime: '2026-05-01T09:00:00',
      endTime: '2026-05-01T12:00:00',
      status: 'DRAFT',
      remark: '首场安排',
      effectiveExamineeCount: 2,
      invalidExamineeCount: 0,
      updatedAt: '2026-04-22T10:00:00',
    });
    mockListExamPlanExaminees.mockResolvedValue([
      { id: 1, examineeNo: 'EX2026001', name: '张三', status: 'ENABLED' },
    ]);
    mockListPapers.mockResolvedValue({
      total: 2,
      page: 1,
      pageSize: 10,
      records: [
        { id: 1, name: 'Java 基础试卷', totalScore: 11, durationMinutes: 120, questionCount: 2, updatedAt: '2026-04-22T10:00:00' },
        { id: 2, name: '空白练习卷', totalScore: 0, durationMinutes: 90, questionCount: 0, updatedAt: '2026-04-22T11:00:00' },
      ],
    });
    mockListExaminees.mockResolvedValue({
      total: 2,
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
          updatedAt: '2026-04-22T10:00:00',
        },
        {
          id: 3,
          examineeNo: 'EX2026003',
          name: '王五',
          gender: 'MALE',
          idCardNo: '110101199303030033',
          phone: '13800000003',
          email: 'wangwu@example.com',
          status: 'ENABLED',
          remark: '正式考生',
          updatedAt: '2026-04-22T10:00:00',
        },
      ],
    });
    mockCreateExamPlan.mockResolvedValue({});
    mockUpdateExamPlan.mockResolvedValue({});
    mockUpdateExamPlanExaminees.mockResolvedValue({ planId: 1, effectiveExamineeCount: 2 });
    mockUpdateExamPlanStatus.mockResolvedValue({});
  });

  it('loads exam plans and supports querying by keyword and status', async () => {
    const { ExamPlanManagementPage } = await import('./ExamPlanManagementPage');
    const user = userEvent.setup();

    render(<ExamPlanManagementPage token="token-123" permissions={['exam-plan:read']} />);

    expect(await screen.findByRole('heading', { name: '考试计划' })).toBeInTheDocument();
    expect(screen.getByText('Java 基础考试-上午场')).toBeInTheDocument();
    expect(screen.getByText('2026-05-01 09:00')).toBeInTheDocument();
    expect(screen.getByText('2026-05-01 12:00')).toBeInTheDocument();

    await user.type(screen.getByLabelText('关键字'), 'Java');
    await user.selectOptions(screen.getByLabelText('状态'), 'PUBLISHED');
    await user.click(screen.getByRole('button', { name: /查\s*询/ }));

    await waitFor(() => {
      expect(mockListExamPlans).toHaveBeenLastCalledWith('token-123', {
        keyword: 'Java',
        status: 'PUBLISHED',
        page: 1,
        pageSize: 10,
      });
    });
  });

  it('validates required fields before creating an exam plan', async () => {
    const { ExamPlanManagementPage } = await import('./ExamPlanManagementPage');
    const user = userEvent.setup();

    render(<ExamPlanManagementPage token="token-123" permissions={['exam-plan:read', 'exam-plan:create']} />);

    await screen.findByRole('heading', { name: '考试计划' });
    await user.click(screen.getByRole('button', { name: '新增考试' }));
    await user.click(screen.getByRole('button', { name: '保存计划' }));

    expect(screen.getByRole('alert')).toHaveTextContent('请填写计划名称、试卷和考试时间');
    expect(mockCreateExamPlan).not.toHaveBeenCalled();
  });

  it('opens edit dialog, loads detail and updates exam plan', async () => {
    const { ExamPlanManagementPage } = await import('./ExamPlanManagementPage');
    const user = userEvent.setup();

    render(<ExamPlanManagementPage token="token-123" permissions={['exam-plan:read', 'exam-plan:update']} />);

    await screen.findByText('Java 基础考试-上午场');
    await user.click(screen.getByRole('button', { name: '编辑计划' }));

    expect(mockGetExamPlan).toHaveBeenCalledWith('token-123', 1);
    expect(await screen.findByRole('dialog', { name: '编辑考试计划' })).toBeInTheDocument();

    await user.clear(screen.getByLabelText('计划名称'));
    await user.type(screen.getByLabelText('计划名称'), 'Java 基础考试-上午场-更新');
    await user.click(screen.getByRole('button', { name: '保存计划' }));

    await waitFor(() => {
      expect(mockUpdateExamPlan).toHaveBeenCalledWith('token-123', 1, {
        name: 'Java 基础考试-上午场-更新',
        paperId: 1,
        startTime: '2026-05-01T09:00:00',
        endTime: '2026-05-01T12:00:00',
        remark: '首场安排',
      });
    });
  });

  it('shows backend error message when saving exam plan fails', async () => {
    const { ExamPlanManagementPage } = await import('./ExamPlanManagementPage');
    const user = userEvent.setup();

    mockUpdateExamPlan.mockRejectedValueOnce({
      response: {
        data: {
          message: '考试时间窗口不能短于试卷时长',
        },
      },
    });

    render(<ExamPlanManagementPage token="token-123" permissions={['exam-plan:read', 'exam-plan:update']} />);

    await screen.findByText('Java 基础考试-上午场');
    await user.click(screen.getByRole('button', { name: '编辑计划' }));
    await screen.findByRole('dialog', { name: '编辑考试计划' });
    await user.click(screen.getByRole('button', { name: '保存计划' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('考试时间窗口不能短于试卷时长');
  });

  it('supports replacing examinees and publishing an exam plan', async () => {
    const { ExamPlanManagementPage } = await import('./ExamPlanManagementPage');
    const user = userEvent.setup();

    render(
      <ExamPlanManagementPage
        token="token-123"
        permissions={['exam-plan:read', 'exam-plan:range', 'exam-plan:status']}
      />,
    );

    await screen.findByText('Java 基础考试-上午场');
    await user.click(screen.getByRole('button', { name: '配置范围' }));

    expect(await screen.findByRole('dialog', { name: '配置考试范围' })).toBeInTheDocument();
    expect(mockListExamPlanExaminees).toHaveBeenCalledWith('token-123', 1);
    expect(mockListExaminees).toHaveBeenCalledWith('token-123', {
      keyword: '',
      status: 'ENABLED',
      page: 1,
      pageSize: 50,
    });
    expect(screen.getByRole('checkbox', { name: '选择考生 1' })).toBeChecked();
    expect(screen.getByText('已配置 1 人')).toBeInTheDocument();
    expect(screen.getAllByText('张三（EX2026001）').length).toBeGreaterThan(0);

    await user.click(screen.getByRole('checkbox', { name: '选择考生 3' }));
    await user.click(screen.getByRole('button', { name: '保存范围' }));

    await waitFor(() => {
      expect(mockUpdateExamPlanExaminees).toHaveBeenCalledWith('token-123', 1, {
        examineeIds: [1, 3],
      });
    });

    await user.click(screen.getByRole('button', { name: '发布考试' }));
    await waitFor(() => {
      expect(mockUpdateExamPlanStatus).toHaveBeenCalledWith('token-123', 1, 'PUBLISHED');
    });
  });

  it('shows close and cancel actions for published exam plans', async () => {
    const { ExamPlanManagementPage } = await import('./ExamPlanManagementPage');
    const user = userEvent.setup();

    mockListExamPlans.mockResolvedValueOnce({
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
    });

    render(<ExamPlanManagementPage token="token-123" permissions={['exam-plan:read', 'exam-plan:status']} />);

    await screen.findByText('Java 基础考试-上午场');
    expect(screen.getByRole('button', { name: '关闭考试' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '取消考试' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '关闭考试' }));
    await user.click(screen.getByRole('button', { name: '取消考试' }));

    await waitFor(() => {
      expect(mockUpdateExamPlanStatus).toHaveBeenNthCalledWith(1, 'token-123', 1, 'CLOSED');
      expect(mockUpdateExamPlanStatus).toHaveBeenNthCalledWith(2, 'token-123', 1, 'CANCELLED');
    });
  });
});
