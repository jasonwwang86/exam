import { Alert, Button, Form, Input, Typography } from 'antd';
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
      <section className={styles.shell} aria-label="管理员登录区域">
        <section className={styles.formCard}>
          <div className={styles.formIntro}>
            <Typography.Text className={styles.formLabel}>系统登录入口</Typography.Text>
            <Typography.Title level={1} className={styles.title}>
              管理员登录
            </Typography.Title>
            <Typography.Title level={2} className={styles.platformTitle}>
              企业管理台
            </Typography.Title>
            <Typography.Paragraph className={styles.formDescription}>
              输入管理员账号与密码后进入统一工作区，当前登录流程、鉴权逻辑与权限体系保持不变。
            </Typography.Paragraph>
          </div>

          <Form layout="vertical" className={styles.form}>
            <Form.Item label="用户名" htmlFor="username">
              <Input
                id="username"
                name="username"
                size="large"
                autoComplete="username"
                placeholder="请输入管理员用户名"
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
                placeholder="请输入密码"
                value={form.password}
                onChange={(event) => onPasswordChange(event.target.value)}
              />
            </Form.Item>
            {errorMessage ? <Alert type="error" showIcon role="alert" message={errorMessage} /> : null}
            <Button aria-label="登录" type="primary" size="large" block onClick={onSubmit} loading={submitting}>
              登录
            </Button>
          </Form>
        </section>
      </section>
    </main>
  );
}
