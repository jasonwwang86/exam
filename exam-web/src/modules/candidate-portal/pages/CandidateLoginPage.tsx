import { Alert, Button, Form, Input, Typography } from 'antd';
import type { CandidateLoginFormState } from '../types';
import styles from './CandidateLoginPage.module.css';

type CandidateLoginPageProps = {
  form: CandidateLoginFormState;
  submitting: boolean;
  errorMessage: string;
  onExamineeNoChange: (value: string) => void;
  onIdCardNoChange: (value: string) => void;
  onSubmit: () => void;
};

export function CandidateLoginPage({
  form,
  submitting,
  errorMessage,
  onExamineeNoChange,
  onIdCardNoChange,
  onSubmit,
}: CandidateLoginPageProps) {
  return (
    <main className={styles.page}>
      <section className={styles.shell} aria-label="考生登录区域">
        <section className={styles.card}>
          <div className={styles.intro}>
            <Typography.Text className={styles.label}>考生登录入口</Typography.Text>
            <Typography.Title level={1} className={styles.title}>
              考生登录
            </Typography.Title>
            <Typography.Paragraph className={styles.description}>
              使用考生编号和身份证号进入待考流程，先确认身份信息，再查看当前可参加的考试。
            </Typography.Paragraph>
          </div>

          <Form layout="vertical" className={styles.form}>
            <Form.Item label="考生编号" htmlFor="candidate-examinee-no">
              <Input
                id="candidate-examinee-no"
                name="candidate-examinee-no"
                size="large"
                placeholder="请输入考生编号"
                value={form.examineeNo}
                onChange={(event) => onExamineeNoChange(event.target.value)}
              />
            </Form.Item>
            <Form.Item label="身份证号" htmlFor="candidate-id-card-no">
              <Input
                id="candidate-id-card-no"
                name="candidate-id-card-no"
                size="large"
                placeholder="请输入身份证号"
                value={form.idCardNo}
                onChange={(event) => onIdCardNoChange(event.target.value)}
              />
            </Form.Item>
            {errorMessage ? <Alert type="error" showIcon role="alert" message={errorMessage} /> : null}
            <Button aria-label="登录并进入待考页" type="primary" size="large" block onClick={onSubmit} loading={submitting}>
              登录并进入待考页
            </Button>
          </Form>
        </section>
      </section>
    </main>
  );
}
