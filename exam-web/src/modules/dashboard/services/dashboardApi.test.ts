import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockGet, mockRandomUuid } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockRandomUuid: vi.fn(),
}));

vi.mock('axios', () => ({
  default: {
    create: () => ({
      get: mockGet,
    }),
  },
}));

describe('dashboardApi TraceNo headers', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockRandomUuid.mockReset();
    mockRandomUuid.mockReturnValue('123e4567-e89b-12d3-a456-426614174000');
    vi.stubGlobal('crypto', {
      randomUUID: mockRandomUuid,
    });
  });

  it('sends TraceNo and Authorization headers when fetching dashboard summary', async () => {
    mockGet.mockResolvedValue({
      data: {
        monthlyNewExamineeCount: 3,
        monthlyNewQuestionCount: 3,
        monthlyNewPaperCount: 2,
        monthlyActiveExamPlanCount: 0,
      },
    });

    const { fetchDashboardSummary } = await import('./dashboardApi');

    await fetchDashboardSummary('token-123');

    expect(mockGet).toHaveBeenCalledWith('/api/admin/dashboard/summary', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });
});
