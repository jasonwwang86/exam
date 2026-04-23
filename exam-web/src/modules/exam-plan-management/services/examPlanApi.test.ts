import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockGet, mockPost, mockPut, mockPatch, mockRandomUuid } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockPut: vi.fn(),
  mockPatch: vi.fn(),
  mockRandomUuid: vi.fn(),
}));

vi.mock('axios', () => ({
  default: {
    create: () => ({
      get: mockGet,
      post: mockPost,
      put: mockPut,
      patch: mockPatch,
    }),
  },
}));

describe('examPlanApi TraceNo headers', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPost.mockReset();
    mockPut.mockReset();
    mockPatch.mockReset();
    mockRandomUuid.mockReset();
    mockRandomUuid.mockReturnValue('123e4567-e89b-12d3-a456-426614174000');
    vi.stubGlobal('crypto', {
      randomUUID: mockRandomUuid,
    });
  });

  it('sends query params and TraceNo when listing exam plans', async () => {
    mockGet.mockResolvedValue({
      data: {
        total: 1,
        page: 1,
        pageSize: 10,
        records: [],
      },
    });

    const { listExamPlans } = await import('./examPlanApi');

    await listExamPlans('token-123', {
      keyword: 'Java',
      status: 'PUBLISHED',
      page: 1,
      pageSize: 10,
    });

    expect(mockGet).toHaveBeenCalledWith('/api/admin/exam-plans', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
      params: {
        keyword: 'Java',
        status: 'PUBLISHED',
        page: 1,
        pageSize: 10,
      },
    });
  });

  it('sends TraceNo when loading plan detail and selected examinees', async () => {
    mockGet.mockResolvedValue({ data: {} });

    const { getExamPlan, listExamPlanExaminees } = await import('./examPlanApi');

    await getExamPlan('token-123', 1);
    await listExamPlanExaminees('token-123', 1);

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/admin/exam-plans/1', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/admin/exam-plans/1/examinees', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });

  it('sends TraceNo when creating, updating and managing range/status', async () => {
    mockPost.mockResolvedValue({ data: { id: 1 } });
    mockPut.mockResolvedValue({ data: { id: 1 } });
    mockPatch.mockResolvedValue({ data: { id: 1 } });

    const {
      createExamPlan,
      updateExamPlan,
      updateExamPlanExaminees,
      updateExamPlanStatus,
    } = await import('./examPlanApi');

    const payload = {
      name: 'Java 基础考试-上午场',
      paperId: 1,
      startTime: '2026-05-01T09:00:00',
      endTime: '2026-05-01T12:00:00',
      remark: '首场安排',
    } as const;

    await createExamPlan('token-123', payload);
    await updateExamPlan('token-123', 1, payload);
    await updateExamPlanExaminees('token-123', 1, {
      examineeIds: [1, 3],
    });
    await updateExamPlanStatus('token-123', 1, 'PUBLISHED');

    expect(mockPost).toHaveBeenCalledWith('/api/admin/exam-plans', payload, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockPut).toHaveBeenNthCalledWith(1, '/api/admin/exam-plans/1', payload, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockPut).toHaveBeenNthCalledWith(2, '/api/admin/exam-plans/1/examinees', {
      examineeIds: [1, 3],
    }, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockPatch).toHaveBeenCalledWith('/api/admin/exam-plans/1/status', {
      status: 'PUBLISHED',
    }, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });
});
