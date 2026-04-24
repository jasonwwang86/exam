import { Alert, Button, Typography } from 'antd';
import type { CandidateExam } from '../types';
import styles from './CandidateExamListPage.module.css';

type CandidateExamListPageProps = {
  exams: CandidateExam[];
  submitting: boolean;
  errorMessage: string;
  onRefresh: () => void;
  onLogout: () => void;
};

export function CandidateExamListPage({ exams, submitting, errorMessage, onRefresh, onLogout }: CandidateExamListPageProps) {
  return (
    <main className={styles.page}>
      <section className={styles.shell}>
        <header className={styles.header}>
          <Typography.Text className={styles.eyebrow}>待考试卷</Typography.Text>
          <Typography.Title level={1} className={styles.title}>
            可参加考试
          </Typography.Title>
          <Typography.Paragraph className={styles.description}>当前页面只展示本次范围内的待考信息，不提供在线答题、交卷或成绩查看入口。</Typography.Paragraph>
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
                    <span>
                      {exam.startTime} 至 {exam.endTime}
                    </span>
                    {exam.remark ? <span>{exam.remark}</span> : null}
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
