import axios from 'axios';
import { withTraceNo } from '../../../shared/utils/trace';
import type { CandidateExam, CandidateProfile, CandidateProfileSummary } from '../types';

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
