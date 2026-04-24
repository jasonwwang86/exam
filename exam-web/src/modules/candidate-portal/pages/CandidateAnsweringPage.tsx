import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Input, Modal, Radio, Typography } from 'antd';
import type { CandidateAnswerQuestion, CandidateAnswerSession, CandidateSaveAnswerResult } from '../types';
import { useAnswerCountdown } from '../hooks/useAnswerCountdown';
import styles from './CandidateAnsweringPage.module.css';

type CandidateAnsweringPageProps = {
  session: CandidateAnswerSession;
  submitting: boolean;
  errorMessage: string;
  onSaveAnswer: (paperQuestionId: number, answerContent: Record<string, unknown> | null) => Promise<CandidateSaveAnswerResult>;
  onSubmitPaper?: () => Promise<void>;
  onBackToExamList: () => void;
  onLogout: () => void;
};

function formatCountdown(remainingSeconds: number) {
  const hours = String(Math.floor(remainingSeconds / 3600)).padStart(2, '0');
  const minutes = String(Math.floor((remainingSeconds % 3600) / 60)).padStart(2, '0');
  const seconds = String(remainingSeconds % 60).padStart(2, '0');
  return `${hours}:${minutes}:${seconds}`;
}

function buildDrafts(questions: CandidateAnswerQuestion[]) {
  return questions.reduce<Record<number, Record<string, unknown> | null>>((drafts, question) => {
    drafts[question.paperQuestionId] = question.savedAnswer ?? null;
    return drafts;
  }, {});
}

function buildQuestionState(questions: CandidateAnswerQuestion[]) {
  return questions.map((question) => ({ ...question }));
}

export function CandidateAnsweringPage({
  session,
  submitting,
  errorMessage,
  onSaveAnswer,
  onSubmitPaper,
  onBackToExamList,
  onLogout,
}: CandidateAnsweringPageProps) {
  const [questions, setQuestions] = useState(() => buildQuestionState(session.questions));
  const [drafts, setDrafts] = useState(() => buildDrafts(session.questions));
  const [currentIndex, setCurrentIndex] = useState(0);
  const [forcedExpired, setForcedExpired] = useState(session.sessionStatus === 'TIME_EXPIRED');
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [autoSubmitTriggered, setAutoSubmitTriggered] = useState(session.sessionStatus === 'AUTO_SUBMITTED');
  const { remainingSeconds, expired } = useAnswerCountdown(session.deadlineAt, session.sessionStatus);
  const finalSubmitted = session.sessionStatus === 'SUBMITTED' || session.sessionStatus === 'AUTO_SUBMITTED';

  useEffect(() => {
    setQuestions(buildQuestionState(session.questions));
    setDrafts(buildDrafts(session.questions));
    setCurrentIndex(0);
    setForcedExpired(session.sessionStatus === 'TIME_EXPIRED');
    setConfirmOpen(false);
    setAutoSubmitTriggered(session.sessionStatus === 'AUTO_SUBMITTED');
  }, [session]);

  useEffect(() => {
    if (!expired || finalSubmitted || autoSubmitTriggered || !onSubmitPaper) {
      return;
    }
    setAutoSubmitTriggered(true);
    void onSubmitPaper();
  }, [autoSubmitTriggered, expired, finalSubmitted, onSubmitPaper]);

  const currentQuestion = questions[currentIndex];
  const answeredCount = useMemo(
    () => questions.filter((question) => question.answerStatus === 'ANSWERED').length,
    [questions],
  );
  const readOnly = finalSubmitted || expired || forcedExpired;

  async function persistQuestion(questionIndex: number) {
    const question = questions[questionIndex];
    if (!question || readOnly) {
      return true;
    }

    try {
      const draft = drafts[question.paperQuestionId] ?? null;
      const result = await onSaveAnswer(question.paperQuestionId, draft);
      setQuestions((current) =>
        current.map((item) =>
          item.paperQuestionId === question.paperQuestionId
            ? {
                ...item,
                savedAnswer: draft,
                answerStatus: result.answerStatus,
              }
            : item,
        ),
      );
      if (result.sessionStatus === 'TIME_EXPIRED') {
        setForcedExpired(true);
      }
      return true;
    } catch {
      return false;
    }
  }

  async function handleSwitchQuestion(nextIndex: number) {
    if (nextIndex === currentIndex) {
      return;
    }
    const saved = await persistQuestion(currentIndex);
    if (!saved) {
      return;
    }
    setCurrentIndex(nextIndex);
  }

  async function handleSaveCurrent() {
    await persistQuestion(currentIndex);
  }

  async function handleConfirmSubmit() {
    if (!onSubmitPaper) {
      return;
    }
    await onSubmitPaper();
    setConfirmOpen(false);
  }

  if (!currentQuestion) {
    return null;
  }

  return (
    <main className={styles.page}>
      <section className={styles.shell}>
        <header className={styles.header}>
          <div>
            <Typography.Text className={styles.eyebrow}>在线答题</Typography.Text>
            <Typography.Title level={1} className={styles.title}>
              {session.name}
            </Typography.Title>
            <Typography.Paragraph className={styles.description}>{session.paperName}</Typography.Paragraph>
          </div>
          <div className={styles.headerActions}>
            <Button onClick={onBackToExamList}>返回考试列表</Button>
            <Button onClick={onLogout}>退出登录</Button>
          </div>
        </header>

        <section className={styles.layout}>
          <aside className={styles.sidebar}>
            <div className={styles.countdownCard}>
              <span className={styles.countdownLabel}>{readOnly ? '答题时间已结束' : '剩余时间'}</span>
              <strong className={styles.countdownValue}>{formatCountdown(Math.max(0, remainingSeconds))}</strong>
            </div>
            <div className={styles.summaryCard}>
              <strong className={styles.summaryTitle}>当前已答 {answeredCount} / {questions.length} 题</strong>
              <div className={styles.navigator}>
                {questions.map((question, index) => (
                  <Button
                    key={question.paperQuestionId}
                    type={index === currentIndex ? 'primary' : 'default'}
                    className={styles.navigatorButton}
                    onClick={() => void handleSwitchQuestion(index)}
                  >
                    第 {question.questionNo} 题 {question.answerStatus === 'ANSWERED' ? '已答' : '未答'}
                  </Button>
                ))}
              </div>
            </div>
          </aside>

          <section className={styles.content}>
            {errorMessage ? <Alert type="error" showIcon role="alert" message={errorMessage} /> : null}
            {readOnly ? <Alert type="warning" showIcon message="答题时间已结束" /> : null}
            {finalSubmitted ? (
              <article className={styles.questionCard}>
                <Typography.Title level={3} className={styles.questionTitle}>
                  {session.sessionStatus === 'AUTO_SUBMITTED' ? '试卷已自动提交' : '试卷已提交'}
                </Typography.Title>
                <Typography.Paragraph className={styles.questionStem}>
                  {session.sessionStatus === 'AUTO_SUBMITTED'
                    ? '作答时间结束后系统已完成自动交卷。'
                    : '你已完成本场考试交卷，当前页面仅展示提交结果。'}
                </Typography.Paragraph>
                {session.submittedAt ? <Typography.Paragraph>{`提交时间 ${session.submittedAt}`}</Typography.Paragraph> : null}
                <Typography.Paragraph>{`已答 ${session.answeredCount} / ${session.totalQuestionCount} 题`}</Typography.Paragraph>
                <div className={styles.questionActions}>
                  <Button type="primary" onClick={onBackToExamList}>
                    返回考试列表
                  </Button>
                </div>
              </article>
            ) : (
              <article className={styles.questionCard}>
                <div className={styles.questionHeader}>
                  <Typography.Text className={styles.questionType}>{currentQuestion.questionTypeName}</Typography.Text>
                  <Typography.Title level={3} className={styles.questionTitle}>
                    第 {currentQuestion.questionNo} 题
                  </Typography.Title>
                </div>
                <Typography.Paragraph className={styles.questionStem}>{currentQuestion.stem}</Typography.Paragraph>

                {currentQuestion.answerMode === 'SINGLE_CHOICE' ? (
                  <Radio.Group
                    value={(drafts[currentQuestion.paperQuestionId]?.selectedOption as string | undefined) ?? undefined}
                    disabled={readOnly}
                    onChange={(event) =>
                      setDrafts((current) => ({
                        ...current,
                        [currentQuestion.paperQuestionId]: {
                          selectedOption: event.target.value,
                        },
                      }))
                    }
                  >
                    <div className={styles.optionList}>
                      {currentQuestion.answerConfig.options?.map((option) => (
                        <Radio key={option.key} value={option.key}>
                          {option.content}
                        </Radio>
                      ))}
                    </div>
                  </Radio.Group>
                ) : (
                  <Input.TextArea
                    aria-label="简答题答案"
                    rows={8}
                    disabled={readOnly}
                    value={(drafts[currentQuestion.paperQuestionId]?.textAnswer as string | undefined) ?? ''}
                    onChange={(event) =>
                      setDrafts((current) => ({
                        ...current,
                        [currentQuestion.paperQuestionId]: {
                          textAnswer: event.target.value,
                        },
                      }))
                    }
                  />
                )}

                <div className={styles.questionActions}>
                  <Button onClick={() => void handleSwitchQuestion(Math.max(0, currentIndex - 1))} disabled={currentIndex === 0}>
                    上一题
                  </Button>
                  <Button type="primary" loading={submitting} disabled={readOnly} onClick={() => void handleSaveCurrent()}>
                    保存当前答案
                  </Button>
                  <Button danger disabled={submitting} onClick={() => setConfirmOpen(true)}>
                    提交试卷
                  </Button>
                  <Button
                    onClick={() => void handleSwitchQuestion(Math.min(questions.length - 1, currentIndex + 1))}
                    disabled={currentIndex === questions.length - 1}
                  >
                    下一题
                  </Button>
                </div>
              </article>
            )}
          </section>
        </section>
      </section>
      <Modal
        open={confirmOpen}
        title="确认提交试卷"
        okText="确认提交"
        cancelText="继续作答"
        confirmLoading={submitting}
        onOk={() => void handleConfirmSubmit()}
        onCancel={() => setConfirmOpen(false)}
      >
        <Typography.Paragraph>确认提交试卷后将不能继续作答。</Typography.Paragraph>
      </Modal>
    </main>
  );
}
