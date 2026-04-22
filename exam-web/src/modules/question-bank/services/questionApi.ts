import axios from 'axios';
import type {
  QuestionDetailRecord,
  QuestionListParams,
  QuestionListResponse,
  QuestionPayload,
  QuestionTypePayload,
  QuestionTypeRecord,
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

export async function listQuestions(token: string, params: QuestionListParams) {
  const response = await client.get<QuestionListResponse>('/api/admin/questions', {
    headers: withAuthHeaders(token),
    params,
  });
  return response.data;
}

export async function getQuestion(token: string, id: number) {
  const response = await client.get<QuestionDetailRecord>(`/api/admin/questions/${id}`, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function createQuestion(token: string, payload: QuestionPayload) {
  const response = await client.post<QuestionDetailRecord>('/api/admin/questions', payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function updateQuestion(token: string, id: number, payload: QuestionPayload) {
  const response = await client.put<QuestionDetailRecord>(`/api/admin/questions/${id}`, payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function deleteQuestion(token: string, id: number) {
  await client.delete(`/api/admin/questions/${id}`, {
    headers: withAuthHeaders(token),
  });
}

export async function listQuestionTypes(token: string) {
  const response = await client.get<QuestionTypeRecord[]>('/api/admin/question-types', {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function createQuestionType(token: string, payload: QuestionTypePayload) {
  const response = await client.post<QuestionTypeRecord>('/api/admin/question-types', payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function updateQuestionType(token: string, id: number, payload: QuestionTypePayload) {
  const response = await client.put<QuestionTypeRecord>(`/api/admin/question-types/${id}`, payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function deleteQuestionType(token: string, id: number) {
  await client.delete(`/api/admin/question-types/${id}`, {
    headers: withAuthHeaders(token),
  });
}
