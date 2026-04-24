import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockPost, mockGet, mockPut, mockRandomUuid } = vi.hoisted(() => ({
  mockPost: vi.fn(),
  mockGet: vi.fn(),
  mockPut: vi.fn(),
  mockRandomUuid: vi.fn(),
}));

vi.mock('axios', () => ({
  default: {
    create: () => ({
      post: mockPost,
      get: mockGet,
      put: mockPut,
    }),
  },
}));

describe('candidateApi TraceNo headers', () => {
  beforeEach(() => {
    mockPost.mockReset();
    mockGet.mockReset();
    mockPut.mockReset();
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

  it('sends Authorization and TraceNo headers when loading candidate answer session', async () => {
    mockPut.mockResolvedValue({
      data: {
        planId: 1,
        name: 'Java 在线答题场次',
        paperName: 'Java 基础试卷',
        durationMinutes: 120,
        sessionStatus: 'IN_PROGRESS',
        startedAt: '2026-05-01T09:10:00',
        deadlineAt: '2026-05-01T11:00:00',
        remainingSeconds: 6600,
        answeredCount: 0,
        totalQuestionCount: 2,
        questions: [],
      },
    });

    const { loadCandidateAnswerSession } = await import('./candidateApi');

    await loadCandidateAnswerSession('candidate-token-2', 1);

    expect(mockPut).toHaveBeenCalledWith(
      '/api/candidate/exams/1/answer-session',
      {},
      {
        headers: {
          Authorization: 'Bearer candidate-token-2',
          TraceNo: '123e4567e89b12d3a456426614174000',
        },
      },
    );
  });

  it('sends Authorization and TraceNo headers when saving candidate answer', async () => {
    mockPut.mockResolvedValue({
      data: {
        paperQuestionId: 1,
        answerStatus: 'ANSWERED',
        lastSavedAt: '2026-05-01T09:15:00',
        remainingSeconds: 6300,
        sessionStatus: 'IN_PROGRESS',
        answeredCount: 1,
      },
    });

    const { saveCandidateAnswer } = await import('./candidateApi');

    await saveCandidateAnswer('candidate-token-2', 1, 1, {
      selectedOption: 'A',
    });

    expect(mockPut).toHaveBeenCalledWith(
      '/api/candidate/exams/1/questions/1/answer',
      {
        answerContent: {
          selectedOption: 'A',
        },
      },
      {
        headers: {
          Authorization: 'Bearer candidate-token-2',
          TraceNo: '123e4567e89b12d3a456426614174000',
        },
      },
    );
  });

  it('sends Authorization and TraceNo headers when submitting candidate exam', async () => {
    mockPost.mockResolvedValue({
      data: {
        planId: 1,
        name: 'Java 在线答题场次',
        paperName: 'Java 基础试卷',
        sessionStatus: 'SUBMITTED',
        submissionMethod: 'MANUAL',
        submittedAt: '2026-05-01T09:45:00',
        answeredCount: 1,
        totalQuestionCount: 2,
      },
    });

    const api = await import('./candidateApi');

    await (api as any).submitCandidateExam('candidate-token-2', 1);

    expect(mockPost).toHaveBeenCalledWith(
      '/api/candidate/exams/1/submission',
      {},
      {
        headers: {
          Authorization: 'Bearer candidate-token-2',
          TraceNo: '123e4567e89b12d3a456426614174000',
        },
      },
    );
  });
});
