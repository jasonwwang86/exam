export type CandidateLoginFormState = {
  examineeNo: string;
  idCardNo: string;
};

export type CandidateProfileSummary = {
  examineeId: number;
  examineeNo: string;
  name: string;
  maskedIdCardNo: string;
};

export type CandidateProfile = CandidateProfileSummary & {
  profileConfirmed: boolean;
  message: string;
};

export type CandidateExam = {
  planId: number;
  name: string;
  paperName: string;
  durationMinutes: number;
  startTime: string;
  endTime: string;
  displayStatus: string;
  remark: string | null;
};
