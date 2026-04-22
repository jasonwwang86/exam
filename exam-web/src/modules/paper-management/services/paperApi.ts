import axios from 'axios';
import type {
  AddPaperQuestionsPayload,
  PaperDetailRecord,
  PaperListParams,
  PaperListResponse,
  PaperPayload,
  PaperQuestionRecord,
  UpdatePaperQuestionPayload,
} from '../types';

const client = axios.create();
const TRACE_NO_HEADER = 'TraceNo';
const TRACE_NO_LENGTH = 32;

function createTraceNo() {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID().replace(/-/g, '');
  }

  return `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}${Math.random().toString(16).slice(2)}`
    .slice(0, TRACE_NO_LENGTH)
    .padEnd(TRACE_NO_LENGTH, '0');
}

function withAuthHeaders(token: string) {
  return {
    Authorization: `Bearer ${token}`,
    [TRACE_NO_HEADER]: createTraceNo(),
  };
}

export async function listPapers(token: string, params: PaperListParams) {
  const response = await client.get<PaperListResponse>('/api/admin/papers', {
    headers: withAuthHeaders(token),
    params,
  });
  return response.data;
}

export async function getPaper(token: string, id: number) {
  const response = await client.get<PaperDetailRecord>(`/api/admin/papers/${id}`, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function createPaper(token: string, payload: PaperPayload) {
  const response = await client.post<PaperDetailRecord>('/api/admin/papers', payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function updatePaper(token: string, id: number, payload: PaperPayload) {
  const response = await client.put<PaperDetailRecord>(`/api/admin/papers/${id}`, payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function deletePaper(token: string, id: number) {
  await client.delete(`/api/admin/papers/${id}`, {
    headers: withAuthHeaders(token),
  });
}

export async function listPaperQuestions(token: string, paperId: number) {
  const response = await client.get<PaperQuestionRecord[]>(`/api/admin/papers/${paperId}/questions`, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function addPaperQuestions(token: string, paperId: number, payload: AddPaperQuestionsPayload) {
  const response = await client.post<PaperQuestionRecord[]>(`/api/admin/papers/${paperId}/questions`, payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function updatePaperQuestion(token: string, paperId: number, paperQuestionId: number, payload: UpdatePaperQuestionPayload) {
  const response = await client.put<PaperQuestionRecord>(`/api/admin/papers/${paperId}/questions/${paperQuestionId}`, payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function deletePaperQuestion(token: string, paperId: number, paperQuestionId: number) {
  await client.delete(`/api/admin/papers/${paperId}/questions/${paperQuestionId}`, {
    headers: withAuthHeaders(token),
  });
}
