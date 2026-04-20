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
      <section className={styles.card} aria-label="管理员登录区域">
        <p className={styles.eyebrow}>Exam Admin</p>
        <h1 className={styles.title}>管理员登录</h1>
        <p className={styles.subtitle}>输入管理员账号和密码，进入基础权限与登录控制台。</p>
        <form className={styles.form}>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="username">用户名</label>
            <input
              className={styles.input}
              id="username"
              name="username"
              type="text"
              autoComplete="username"
              value={form.username}
              onChange={(event) => onUsernameChange(event.target.value)}
            />
          </div>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="password">密码</label>
            <input
              className={styles.input}
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={form.password}
              onChange={(event) => onPasswordChange(event.target.value)}
            />
          </div>
          {errorMessage ? <p className={styles.error} role="alert">{errorMessage}</p> : null}
          <button className={styles.submit} type="button" onClick={onSubmit} disabled={submitting}>
            登录
          </button>
        </form>
      </section>
    </main>
  );
}
