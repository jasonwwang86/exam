import type { ExamineeRecord } from '../examinees/types';

export type ExamPlanStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'CANCELLED';

export type ExamPlanListRecord = {
  id: number;
  name: string;
  paperId: number;
  paperName: string;
  startTime: string;
  endTime: string;
  effectiveExamineeCount: number;
  status: ExamPlanStatus;
  updatedAt: string;
};

export type ExamPlanDetailRecord = ExamPlanListRecord & {
  paperDurationMinutes: number;
  remark: string;
  invalidExamineeCount: number;
};

export type ExamPlanListParams = {
  keyword?: string;
  status?: ExamPlanStatus | '';
  page?: number;
  pageSize?: number;
};

export type ExamPlanListResponse = {
  total: number;
  page: number;
  pageSize: number;
  records: ExamPlanListRecord[];
};

export type ExamPlanPayload = {
  name: string;
  paperId: number;
  startTime: string;
  endTime: string;
  remark: string;
};

export type UpdateExamPlanExamineesPayload = {
  examineeIds: number[];
};

export type UpdateExamPlanExamineesResult = {
  planId: number;
  effectiveExamineeCount: number;
};

export type ExamPlanExamineeRecord = Pick<ExamineeRecord, 'id' | 'examineeNo' | 'name' | 'status'>;
