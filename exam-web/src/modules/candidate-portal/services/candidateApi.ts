import axios from 'axios';
import { withTraceNo } from '../../../shared/utils/trace';
import type {
  CandidateAnswerSession,
  CandidateExam,
  CandidateExamSubmissionResult,
  CandidateProfile,
  CandidateProfileSummary,
  CandidateScoreReport,
  CandidateSaveAnswerResult,
} from '../types';

const client = axios.create();

export type CandidateLoginPayload = {
  examineeNo: string;
  idCardNo: string;
};

export type CandidateLoginResponse = {
  token: string;
  tokenType: string;
  expiresAt?: string;
  profileConfirmed: boolean;
  profile: CandidateProfileSummary;
};

export type CandidateConfirmResponse = CandidateLoginResponse;

export async function loginCandidate(payload: CandidateLoginPayload) {
  const response = await client.post<CandidateLoginResponse>('/api/candidate/auth/login', payload, withTraceNo());
  return response.data;
}

export async function fetchCandidateProfile(token: string) {
  const response = await client.get<CandidateProfile>(
    '/api/candidate/profile',
    withTraceNo({
      Authorization: `Bearer ${token}`,
    }),
  );
  return response.data;
}

export async function confirmCandidateProfile(token: string) {
  const response = await client.post<CandidateConfirmResponse>(
    '/api/candidate/profile/confirm',
    {},
    withTraceNo({
      Authorization: `Bearer ${token}`,
    }),
  );
  return response.data;
}

export async function listCandidateExams(token: string) {
  const response = await client.get<CandidateExam[]>(
    '/api/candidate/exams',
    withTraceNo({
      Authorization: `Bearer ${token}`,
    }),
  );
  return response.data;
}

export async function loadCandidateAnswerSession(token: string, planId: number) {
  const response = await client.put<CandidateAnswerSession>(
    `/api/candidate/exams/${planId}/answer-session`,
    {},
    withTraceNo({
      Authorization: `Bearer ${token}`,
    }),
  );
  return response.data;
}

export async function saveCandidateAnswer(
  token: string,
  planId: number,
  paperQuestionId: number,
  answerContent: Record<string, unknown> | null,
) {
  const response = await client.put<CandidateSaveAnswerResult>(
    `/api/candidate/exams/${planId}/questions/${paperQuestionId}/answer`,
    {
      answerContent,
    },
    withTraceNo({
      Authorization: `Bearer ${token}`,
    }),
  );
  return response.data;
}

export async function submitCandidateExam(token: string, planId: number) {
  const response = await client.post<CandidateExamSubmissionResult>(
    `/api/candidate/exams/${planId}/submission`,
    {},
    withTraceNo({
      Authorization: `Bearer ${token}`,
    }),
  );
  return response.data;
}

export async function fetchCandidateScoreReport(token: string, planId: number) {
  const response = await client.get<CandidateScoreReport>(
    `/api/candidate/exams/${planId}/score-report`,
    withTraceNo({
      Authorization: `Bearer ${token}`,
    }),
  );
  return response.data;
}
