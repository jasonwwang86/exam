import axios from 'axios';
import type {
  ExamPlanDetailRecord,
  ExamPlanExamineeRecord,
  ExamPlanListParams,
  ExamPlanListResponse,
  ExamPlanPayload,
  ExamPlanStatus,
  UpdateExamPlanExamineesPayload,
  UpdateExamPlanExamineesResult,
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

export async function listExamPlans(token: string, params: ExamPlanListParams) {
  const response = await client.get<ExamPlanListResponse>('/api/admin/exam-plans', {
    headers: withAuthHeaders(token),
    params,
  });
  return response.data;
}

export async function getExamPlan(token: string, id: number) {
  const response = await client.get<ExamPlanDetailRecord>(`/api/admin/exam-plans/${id}`, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function createExamPlan(token: string, payload: ExamPlanPayload) {
  const response = await client.post<ExamPlanDetailRecord>('/api/admin/exam-plans', payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function updateExamPlan(token: string, id: number, payload: ExamPlanPayload) {
  const response = await client.put<ExamPlanDetailRecord>(`/api/admin/exam-plans/${id}`, payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function listExamPlanExaminees(token: string, id: number) {
  const response = await client.get<ExamPlanExamineeRecord[]>(`/api/admin/exam-plans/${id}/examinees`, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function updateExamPlanExaminees(token: string, id: number, payload: UpdateExamPlanExamineesPayload) {
  const response = await client.put<UpdateExamPlanExamineesResult>(`/api/admin/exam-plans/${id}/examinees`, payload, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}

export async function updateExamPlanStatus(token: string, id: number, status: ExamPlanStatus) {
  const response = await client.patch<ExamPlanDetailRecord>(`/api/admin/exam-plans/${id}/status`, {
    status,
  }, {
    headers: withAuthHeaders(token),
  });
  return response.data;
}
