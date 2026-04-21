import { Alert, Button, Card, Form, Input, Space, Typography } from 'antd';
import type { LoginFormState } from '../types';
import styles from './LoginPage.module.css';

type LoginPageProps = {
  form: LoginFormState;
  submitting: boolean;
  errorMessage: string;
  onUsernameChange: (value: string) => void;
  onPasswordChange: (value: string) => void;
  onSubmit: () => void;
};

export function LoginPage({
  form,
  submitting,
  errorMessage,
  onUsernameChange,
  onPasswordChange,
  onSubmit,
}: LoginPageProps) {
  return (
    <main className={styles.page}>
      <section className={styles.panel} aria-label="管理员登录区域">
        <Card variant="borderless" className={styles.formCard}>
          <Space direction="vertical" size={6} className={styles.formHeader}>
            <div className={styles.brand}>
              <div className={styles.brandMark}>E</div>
              <div>
                <Typography.Text className={styles.formEyebrow}>Exam Admin</Typography.Text>
                <Typography.Title level={1} className={styles.title}>
                  企业管理台
                </Typography.Title>
              </div>
            </div>
            <Typography.Title level={2} className={styles.formTitle}>
              管理员登录
            </Typography.Title>
            <Typography.Paragraph className={styles.formText}>
              输入管理员账号和密码，进入统一工作台。
            </Typography.Paragraph>
          </Space>

          <Form layout="vertical" className={styles.form}>
            <Form.Item label="用户名" htmlFor="username">
              <Input
                id="username"
                name="username"
                size="large"
                autoComplete="username"
                value={form.username}
                onChange={(event) => onUsernameChange(event.target.value)}
              />
            </Form.Item>
            <Form.Item label="密码" htmlFor="password">
              <Input.Password
                id="password"
                name="password"
                size="large"
                autoComplete="current-password"
                value={form.password}
                onChange={(event) => onPasswordChange(event.target.value)}
              />
            </Form.Item>
            {errorMessage ? <Alert type="error" showIcon role="alert" message={errorMessage} /> : null}
            <Button aria-label="登录" type="primary" size="large" block onClick={onSubmit} loading={submitting}>
              登录
            </Button>
          </Form>
        </Card>
      </section>
    </main>
  );
}
