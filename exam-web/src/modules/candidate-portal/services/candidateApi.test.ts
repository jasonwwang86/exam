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

describe('candidateApi TraceNo headers', () => {
  beforeEach(() => {
    mockPost.mockReset();
    mockGet.mockReset();
    mockRandomUuid.mockReset();
    mockRandomUuid.mockReturnValue('123e4567-e89b-12d3-a456-426614174000');
    vi.stubGlobal('crypto', {
      randomUUID: mockRandomUuid,
    });
  });

  it('sends TraceNo header when candidate logs in', async () => {
    mockPost.mockResolvedValue({
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
    });

    const { loginCandidate } = await import('./candidateApi');

    await loginCandidate({
      examineeNo: 'EX2026001',
      idCardNo: '110101199001010011',
    });

    expect(mockPost).toHaveBeenCalledWith(
      '/api/candidate/auth/login',
      {
        examineeNo: 'EX2026001',
        idCardNo: '110101199001010011',
      },
      {
        headers: {
          TraceNo: '123e4567e89b12d3a456426614174000',
        },
      },
    );
  });

  it('sends Authorization and TraceNo headers when loading candidate profile', async () => {
    mockGet.mockResolvedValue({
      data: {
        examineeId: 1,
        examineeNo: 'EX2026001',
        name: '张三',
        maskedIdCardNo: '110101********0011',
        profileConfirmed: false,
        message: '请先确认身份信息后查看可参加考试',
      },
    });

    const { fetchCandidateProfile } = await import('./candidateApi');

    await fetchCandidateProfile('candidate-token-1');

    expect(mockGet).toHaveBeenCalledWith('/api/candidate/profile', {
      headers: {
        Authorization: 'Bearer candidate-token-1',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });

  it('sends Authorization and TraceNo headers when confirming candidate profile', async () => {
    mockPost.mockResolvedValue({
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

    const { confirmCandidateProfile } = await import('./candidateApi');

    await confirmCandidateProfile('candidate-token-1');

    expect(mockPost).toHaveBeenCalledWith(
      '/api/candidate/profile/confirm',
      {},
      {
        headers: {
          Authorization: 'Bearer candidate-token-1',
          TraceNo: '123e4567e89b12d3a456426614174000',
        },
      },
    );
  });

  it('sends Authorization and TraceNo headers when listing candidate exams', async () => {
    mockGet.mockResolvedValue({
      data: [],
    });

    const { listCandidateExams } = await import('./candidateApi');

    await listCandidateExams('candidate-token-2');

    expect(mockGet).toHaveBeenCalledWith('/api/candidate/exams', {
      headers: {
        Authorization: 'Bearer candidate-token-2',
        TraceNo: '123e4567e89b12d3a456426614174000',
      },
    });
  });
});
