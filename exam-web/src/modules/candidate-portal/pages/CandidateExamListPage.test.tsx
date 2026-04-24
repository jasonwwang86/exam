import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CandidateExamListPage } from './CandidateExamListPage';

describe('CandidateExamListPage', () => {
  it('shows submitted summary and hides answer entry for submitted exam', () => {
    render(
      <CandidateExamListPage
        exams={
          [
            {
              planId: 1,
              name: 'Java 在线答题场次',
              paperName: 'Java 基础试卷',
              durationMinutes: 120,
              startTime: '2026-05-01T09:00:00',
              endTime: '2026-05-01T12:00:00',
              displayStatus: '进行中',
              remark: '已交卷',
              canEnterAnswering: false,
              answeringStatus: 'SUBMITTED',
              remainingSeconds: 0,
              submittedAt: '2026-05-01T09:40:00',
              submissionMethod: 'MANUAL',
            },
          ] as any
        }
        submitting={false}
        errorMessage=""
        onRefresh={vi.fn()}
        onEnterAnswering={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    expect(screen.getByText('已提交')).toBeInTheDocument();
    expect(screen.getByText('提交时间 2026-05-01T09:40:00')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '进入答题' })).not.toBeInTheDocument();
  });
});
