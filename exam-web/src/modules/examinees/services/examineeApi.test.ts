import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockGet, mockPost, mockPut, mockDelete, mockPatch, mockRandomUuid } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockPut: vi.fn(),
  mockDelete: vi.fn(),
  mockPatch: vi.fn(),
  mockRandomUuid: vi.fn(),
}));

vi.mock('axios', () => ({
  default: {
    create: () => ({
      get: mockGet,
      post: mockPost,
      put: mockPut,
      delete: mockDelete,
      patch: mockPatch,
    }),
  },
}));

describe('examineeApi TraceNo headers', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPost.mockReset();
    mockPut.mockReset();
    mockDelete.mockReset();
    mockPatch.mockReset();
    mockRandomUuid.mockReset();
    mockRandomUuid.mockReturnValue('123e4567-e89b-12d3-a456-426614174000');
    vi.stubGlobal('crypto', {
      randomUUID: mockRandomUuid,
    });
  });

  it('sends query params and TraceNo when listing examinees', async () => {
    mockGet.mockResolvedValue({
      data: {
        total: 1,
        page: 1,
        pageSize: 10,
        records: [],
      },
    });

    const { listExaminees } = await import('./examineeApi');

    await listExaminees('token-123', {
      keyword: '张',
      status: 'ENABLED',
      page: 1,
      pageSize: 10,
    });

    expect(mockGet).toHaveBeenCalledWith('/api/admin/examinees', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
      params: {
        keyword: '张',
        status: 'ENABLED',
        page: 1,
        pageSize: 10,
      },
    });
  });

  it('sends TraceNo when creating an examinee', async () => {
    mockPost.mockResolvedValue({
      data: {
        id: 1,
      },
    });

    const { createExaminee } = await import('./examineeApi');

    await createExaminee('token-123', {
      examineeNo: 'EX2026099',
      name: '赵六',
      gender: 'FEMALE',
      idCardNo: '310101199901011234',
      phone: '13900000099',
      email: 'zhaoliu@example.com',
      status: 'ENABLED',
      remark: '新导入考生',
    });

    expect(mockPost).toHaveBeenCalledWith(
      '/api/admin/examinees',
      {
        examineeNo: 'EX2026099',
        name: '赵六',
        gender: 'FEMALE',
        idCardNo: '310101199901011234',
        phone: '13900000099',
        email: 'zhaoliu@example.com',
        status: 'ENABLED',
        remark: '新导入考生',
      },
      {
        headers: {
          Authorization: 'Bearer token-123',
          TraceNo: '123e4567e89b12d3a456426614174000',
        },
      },
    );
  });

  it('sends TraceNo when updating examinee status', async () => {
    mockPatch.mockResolvedValue({ data: { id: 1, status: 'DISABLED' } });

    const { updateExamineeStatus } = await import('./examineeApi');

    await updateExamineeStatus('token-123', 1, 'DISABLED');

    expect(mockPatch).toHaveBeenCalledWith(
      '/api/admin/examinees/1/status',
      {
        status: 'DISABLED',
      },
      {
        headers: {
          Authorization: 'Bearer token-123',
          TraceNo: '123e4567e89b12d3a456426614174000',
        },
      },
    );
  });

  it('requests blob response when exporting examinees', async () => {
    mockGet.mockResolvedValue({ data: new Blob() });

    const { exportExaminees } = await import('./examineeApi');

    await exportExaminees('token-123', {
      status: 'ENABLED',
    });

    expect(mockGet).toHaveBeenCalledWith('/api/admin/examinees/export', {
      headers: {
        Authorization: 'Bearer token-123',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
      params: {
        status: 'ENABLED',
      },
      responseType: 'blob',
    });
  });
});
