import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockGet, mockPost, mockPut, mockDelete, mockRandomUuid } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockPut: vi.fn(),
  mockDelete: vi.fn(),
  mockRandomUuid: vi.fn(),
}));

vi.mock('axios', () => ({
  default: {
    create: () => ({
      get: mockGet,
      post: mockPost,
      put: mockPut,
      delete: mockDelete,
    }),
  },
}));

describe('paperApi TraceNo headers', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPost.mockReset();
    mockPut.mockReset();
    mockDelete.mockReset();
    mockRandomUuid.mockReset();
    mockRandomUuid.mockReturnValue('123e4567-e89b-12d3-a456-426614174000');
    vi.stubGlobal('crypto', {
      randomUUID: mockRandomUuid,
    });
  });

  it('sends query params and TraceNo when listing papers', async () => {
    mockGet.mockResolvedValue({
      data: {
        total: 1,
        page: 1,
        pageSize: 10,
        records: [],
      },
    });

    const { listPapers } = await import('./paperApi');

    await listPapers('token-123', {
      keyword: 'Java',
      page: 1,
      pageSize: 10,
    });

    expect(mockGet).toHaveBeenCalledWith('/api/admin/papers', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
      params: {
        keyword: 'Java',
        page: 1,
        pageSize: 10,
      },
    });
  });

  it('sends TraceNo when loading paper detail and questions', async () => {
    mockGet.mockResolvedValue({ data: {} });

    const { getPaper, listPaperQuestions } = await import('./paperApi');

    await getPaper('token-123', 1);
    await listPaperQuestions('token-123', 1);

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/admin/papers/1', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/admin/papers/1/questions', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });

  it('sends TraceNo when creating, updating and deleting papers', async () => {
    mockPost.mockResolvedValue({ data: { id: 1 } });
    mockPut.mockResolvedValue({ data: { id: 1 } });
    mockDelete.mockResolvedValue({ data: { success: true } });

    const { createPaper, updatePaper, deletePaper } = await import('./paperApi');
    const payload = {
      name: 'Java 基础试卷',
      description: '覆盖 Java 基础知识',
      durationMinutes: 120,
      remark: '首套试卷',
    };

    await createPaper('token-123', payload);
    await updatePaper('token-123', 1, payload);
    await deletePaper('token-123', 1);

    expect(mockPost).toHaveBeenCalledWith('/api/admin/papers', payload, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockPut).toHaveBeenCalledWith('/api/admin/papers/1', payload, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockDelete).toHaveBeenCalledWith('/api/admin/papers/1', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });

  it('sends TraceNo when managing paper questions', async () => {
    mockPost.mockResolvedValue({ data: [] });
    mockPut.mockResolvedValue({ data: {} });
    mockDelete.mockResolvedValue({ data: { success: true } });

    const { addPaperQuestions, updatePaperQuestion, deletePaperQuestion } = await import('./paperApi');

    await addPaperQuestions('token-123', 1, { questionIds: [2, 3] });
    await updatePaperQuestion('token-123', 1, 9, {
      itemScore: 10,
      displayOrder: 1,
    });
    await deletePaperQuestion('token-123', 1, 9);

    expect(mockPost).toHaveBeenCalledWith('/api/admin/papers/1/questions', {
      questionIds: [2, 3],
    }, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockPut).toHaveBeenCalledWith('/api/admin/papers/1/questions/9', {
      itemScore: 10,
      displayOrder: 1,
    }, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockDelete).toHaveBeenCalledWith('/api/admin/papers/1/questions/9', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });
});
