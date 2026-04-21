import axios from 'axios';
import type {
  ExamineeFormPayload,
  ExamineeListParams,
  ExamineeListResponse,
  ExamineeRecord,
  ExamineeStatus,
  ImportExamineeResult,
  UpdateExamineePayload,
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

export async function listExaminees(token: string, params: ExamineeListParams) {
  const response = await client.get<ExamineeListResponse>('/api/admin/examinees', {
    headers: withAuthHeaders(token),
    params,
  });
  return response.data;
}

export async function createExaminee(token: string, payload: ExamineeFormPayload) {
  const response = await client.post<ExamineeRecord>('/api/admin/examinees', payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function updateExaminee(token: string, id: number, payload: UpdateExamineePayload) {
  const response = await client.put<ExamineeRecord>(`/api/admin/examinees/${id}`, payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function deleteExaminee(token: string, id: number) {
  await client.delete(`/api/admin/examinees/${id}`, {
    headers: withAuthHeaders(token),
  });
}

export async function updateExamineeStatus(token: string, id: number, status: ExamineeStatus) {
  const response = await client.patch<ExamineeRecord>(
    `/api/admin/examinees/${id}/status`,
    { status },
    {
      headers: withAuthHeaders(token),
    },
  );
  return response.data;
}

export async function importExaminees(token: string, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  const response = await client.post<ImportExamineeResult>('/api/admin/examinees/import', formData, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function exportExaminees(token: string, params: ExamineeListParams) {
  const response = await client.get<Blob>('/api/admin/examinees/export', {
    headers: withAuthHeaders(token),
    params,
    responseType: 'blob',
  });
  return response.data;
}
