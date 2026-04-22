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

describe('questionApi TraceNo headers', () => {
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

  it('sends query params and TraceNo when listing questions', async () => {
    mockGet.mockResolvedValue({
      data: {
        total: 1,
        page: 1,
        pageSize: 10,
        records: [],
      },
    });

    const { listQuestions } = await import('./questionApi');

    await listQuestions('token-123', {
      keyword: '入口',
      questionTypeId: 1,
      difficulty: 'EASY',
      page: 1,
      pageSize: 10,
    });

    expect(mockGet).toHaveBeenCalledWith('/api/admin/questions', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
      params: {
        keyword: '入口',
        questionTypeId: 1,
        difficulty: 'EASY',
        page: 1,
        pageSize: 10,
      },
    });
  });

  it('sends TraceNo when loading question detail and types', async () => {
    mockGet.mockResolvedValue({ data: {} });

    const { getQuestion, listQuestionTypes } = await import('./questionApi');

    await getQuestion('token-123', 1);
    await listQuestionTypes('token-123');

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/admin/questions/1', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/admin/question-types', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });

  it('sends TraceNo when creating and updating a question', async () => {
    mockPost.mockResolvedValue({ data: { id: 1 } });
    mockPut.mockResolvedValue({ data: { id: 1 } });

    const { createQuestion, updateQuestion } = await import('./questionApi');
    const payload = {
      stem: 'HTTP 默认端口是什么？',
      questionTypeId: 1,
      difficulty: 'EASY' as const,
      score: 3,
      answerConfig: {
        options: [
          { key: 'A', content: '80' },
          { key: 'B', content: '443' },
        ],
        correctOption: 'A',
      },
    };

    await createQuestion('token-123', payload);
    await updateQuestion('token-123', 1, payload);

    expect(mockPost).toHaveBeenCalledWith('/api/admin/questions', payload, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockPut).toHaveBeenCalledWith('/api/admin/questions/1', payload, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });

  it('sends TraceNo when deleting a question and managing question types', async () => {
    mockDelete.mockResolvedValue({ data: { success: true } });
    mockPost.mockResolvedValue({ data: { id: 1 } });
    mockPut.mockResolvedValue({ data: { id: 1 } });

    const { deleteQuestion, createQuestionType, updateQuestionType, deleteQuestionType } = await import('./questionApi');

    await deleteQuestion('token-123', 1);
    await createQuestionType('token-123', {
      name: '填空题',
      answerMode: 'TEXT',
      sort: 50,
      remark: '文本填空',
    });
    await updateQuestionType('token-123', 2, {
      name: '问答题',
      answerMode: 'TEXT',
      sort: 60,
      remark: '人工评分',
    });
    await deleteQuestionType('token-123', 2);

    expect(mockDelete).toHaveBeenNthCalledWith(1, '/api/admin/questions/1', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockPost).toHaveBeenCalledWith('/api/admin/question-types', {
      name: '填空题',
      answerMode: 'TEXT',
      sort: 50,
      remark: '文本填空',
    }, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockPut).toHaveBeenCalledWith('/api/admin/question-types/2', {
      name: '问答题',
      answerMode: 'TEXT',
      sort: 60,
      remark: '人工评分',
    }, {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
    expect(mockDelete).toHaveBeenNthCalledWith(2, '/api/admin/question-types/2', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });
});
