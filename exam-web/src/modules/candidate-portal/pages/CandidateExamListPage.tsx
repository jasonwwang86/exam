import { Alert, Button, Typography } from 'antd';
import type { CandidateExam } from '../types';
import styles from './CandidateExamListPage.module.css';

type CandidateExamListPageProps = {
  exams: CandidateExam[];
  submitting: boolean;
  errorMessage: string;
  onRefresh: () => void;
  onEnterAnswering: (planId: number) => void;
  onViewScoreReport: (planId: number) => void;
  onLogout: () => void;
};

function formatRemainingMinutes(remainingSeconds: number | null) {
  if (remainingSeconds == null) {
    return '待进入答题后开始计时';
  }
  return `剩余 ${Math.max(1, Math.ceil(remainingSeconds / 60))} 分钟`;
}

function resolveAnsweringStatusText(exam: CandidateExam) {
  if (exam.answeringStatus === 'IN_PROGRESS') {
    return formatRemainingMinutes(exam.remainingSeconds);
  }
  if (exam.answeringStatus === 'SUBMITTED') {
    return '已提交';
  }
  if (exam.answeringStatus === 'AUTO_SUBMITTED') {
    return '已自动提交';
  }
  if (exam.answeringStatus === 'TIME_EXPIRED') {
    return '答题时间已结束';
  }
  return '未进入答题';
}

function resolveScoreStatusText(exam: CandidateExam) {
  if (exam.scoreStatus === 'PUBLISHED') {
    return '已出分';
  }
  if (exam.answeringStatus === 'SUBMITTED' || exam.answeringStatus === 'AUTO_SUBMITTED') {
    return '待出分';
  }
  return null;
}

export function CandidateExamListPage({ exams, submitting, errorMessage, onRefresh, onEnterAnswering, onViewScoreReport, onLogout }: CandidateExamListPageProps) {
  return (
    <main className={styles.page}>
      <section className={styles.shell}>
        <header className={styles.header}>
          <Typography.Text className={styles.eyebrow}>待考试卷</Typography.Text>
          <Typography.Title level={1} className={styles.title}>
            可参加考试
          </Typography.Title>
          <Typography.Paragraph className={styles.description}>当前页面支持进入在线答题、展示交卷状态，并在成绩生成后查看成绩详情；仍不扩展到动态大屏、考试计划或监考能力。</Typography.Paragraph>
          <div className={styles.actions}>
            <Button size="large" onClick={onLogout}>
              退出登录
            </Button>
            <Button type="primary" size="large" onClick={onRefresh} loading={submitting}>
              刷新考试列表
            </Button>
          </div>
        </header>

        <section className={styles.card}>
          {errorMessage ? <Alert type="error" showIcon role="alert" message={errorMessage} /> : null}
          {exams.length === 0 ? (
            <div className={styles.empty}>当前暂无可参加的考试</div>
          ) : (
            <div className={styles.list}>
              {exams.map((exam) => (
                <article key={exam.planId} className={styles.item}>
                  <div className={styles.itemHeader}>
                    <h2 className={styles.itemTitle}>{exam.name}</h2>
                    <span className={styles.status}>{exam.displayStatus}</span>
                  </div>
                  <div className={styles.meta}>
                    <span>{exam.paperName}</span>
                    <span>考试时长 {exam.durationMinutes} 分钟</span>
                    <span>{resolveAnsweringStatusText(exam)}</span>
                    <span>
                      {exam.startTime} 至 {exam.endTime}
                    </span>
                    {exam.submittedAt ? <span>{`提交时间 ${exam.submittedAt}`}</span> : null}
                    {exam.totalScore != null ? <span>{`总分 ${exam.totalScore}`}</span> : null}
                    {resolveScoreStatusText(exam) ? <span>{resolveScoreStatusText(exam)}</span> : null}
                    {exam.remark ? <span>{exam.remark}</span> : null}
                  </div>
                  <div className={styles.itemActions}>
                    {exam.canEnterAnswering ? (
                      <Button type="primary" onClick={() => onEnterAnswering(exam.planId)}>
                        进入答题
                      </Button>
                    ) : exam.reportAvailable ? (
                      <Button type="primary" onClick={() => onViewScoreReport(exam.planId)}>
                        查看成绩
                      </Button>
                    ) : (
                      <span className={styles.readonlyHint}>当前不可进入答题</span>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </section>
    </main>
  );
}
