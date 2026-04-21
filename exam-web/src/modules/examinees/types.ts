export type ExamineeStatus = 'ENABLED' | 'DISABLED';
export type ExamineeGender = 'MALE' | 'FEMALE';

export type ExamineeRecord = {
  id: number;
  examineeNo: string;
  name: string;
  gender: ExamineeGender;
  idCardNo: string;
  phone: string;
  email: string;
  status: ExamineeStatus;
  remark: string;
  updatedAt: string;
};

export type ExamineeListParams = {
  keyword?: string;
  status?: string;
  page?: number;
  pageSize?: number;
};

export type ExamineeListResponse = {
  total: number;
  page: number;
  pageSize: number;
  records: ExamineeRecord[];
};

export type ExamineeFormPayload = {
  examineeNo: string;
  name: string;
  gender: ExamineeGender;
  idCardNo: string;
  phone: string;
  email: string;
  status: ExamineeStatus;
  remark: string;
};

export type UpdateExamineePayload = Omit<ExamineeFormPayload, 'examineeNo'>;

export type ImportExamineeResult = {
  successCount: number;
  failureCount: number;
  failures: Array<{
    rowNumber: number;
    message: string;
  }>;
};
