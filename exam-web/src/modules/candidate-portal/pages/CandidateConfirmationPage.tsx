import { Alert, Button, Typography } from 'antd';
import type { CandidateProfile } from '../types';
import styles from './CandidateConfirmationPage.module.css';

type CandidateConfirmationPageProps = {
  profile: CandidateProfile;
  submitting: boolean;
  errorMessage: string;
  onConfirm: () => void;
  onLogout: () => void;
};

export function CandidateConfirmationPage({ profile, submitting, errorMessage, onConfirm, onLogout }: CandidateConfirmationPageProps) {
  return (
    <main className={styles.page}>
      <section className={styles.card}>
        <header className={styles.header}>
          <Typography.Text className={styles.eyebrow}>待考确认</Typography.Text>
          <Typography.Title level={1} className={styles.title}>
            确认身份信息
          </Typography.Title>
          <Typography.Paragraph className={styles.description}>{profile.message}</Typography.Paragraph>
        </header>

        <section className={styles.grid} aria-label="考生身份信息">
          <div className={styles.item}>
            <span className={styles.itemLabel}>姓名</span>
            <span className={styles.itemValue}>{profile.name}</span>
          </div>
          <div className={styles.item}>
            <span className={styles.itemLabel}>考生编号</span>
            <span className={styles.itemValue}>{profile.examineeNo}</span>
          </div>
          <div className={styles.item}>
            <span className={styles.itemLabel}>身份证号</span>
            <span className={styles.itemValue}>{profile.maskedIdCardNo}</span>
          </div>
        </section>

        {errorMessage ? <Alert type="error" showIcon role="alert" message={errorMessage} /> : null}

        <div className={styles.actions}>
          <Button size="large" onClick={onLogout}>
            退出登录
          </Button>
          <Button type="primary" size="large" onClick={onConfirm} loading={submitting}>
            确认信息并查看考试
          </Button>
        </div>
      </section>
    </main>
  );
}
