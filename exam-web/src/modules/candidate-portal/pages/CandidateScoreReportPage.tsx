import { Alert, Button, Checkbox, Divider, Input, Radio, Tag, Typography } from 'antd';
import type { CandidateQuestionOption, CandidateScoreReport, CandidateScoreReportItem } from '../types';
import styles from './CandidateScoreReportPage.module.css';

type CandidateScoreReportPageProps = {
  report: CandidateScoreReport;
  errorMessage: string;
  onBackToExamList: () => void;
  onLogout: () => void;
};

function formatJudgeStatus(judgeStatus: string) {
  switch (judgeStatus) {
    case 'CORRECT':
      return '判定正确';
    case 'WRONG':
      return '判定错误';
    case 'PARTIALLY_CORRECT':
      return '部分正确';
    case 'PENDING_REVIEW':
      return '待判定';
    default:
      return judgeStatus;
  }
}

function formatAnswerStatus(answerStatus: string) {
  return answerStatus === 'ANSWERED' ? '已作答' : '未作答';
}

function buildTrueFalseOptions(config: CandidateScoreReportItem['answerConfig']) {
  const configuredOptions = Array.isArray(config.options) ? config.options : [];
  if (configuredOptions.length > 0) {
    return configuredOptions;
  }
  return [
    { key: 'TRUE', content: '正确' },
    { key: 'FALSE', content: '错误' },
  ];
}

function renderReadonlyAnswer(item: CandidateScoreReportItem) {
  const savedAnswer = item.savedAnswer ?? {};

  if (item.answerMode === 'SINGLE_CHOICE') {
    return (
      <Radio.Group value={(savedAnswer.selectedOption as string | undefined) ?? undefined} disabled className={styles.choiceGroup}>
        <div className={styles.optionList}>
          {item.answerConfig.options?.map((option) => (
            <Radio key={option.key} value={option.key}>
              {option.content}
            </Radio>
          ))}
        </div>
      </Radio.Group>
    );
  }

  if (item.answerMode === 'MULTIPLE_CHOICE') {
    return (
      <Checkbox.Group
        value={Array.isArray(savedAnswer.selectedOptions) ? (savedAnswer.selectedOptions as string[]) : []}
        disabled
        className={styles.choiceGroup}
      >
        <div className={styles.optionList}>
          {item.answerConfig.options?.map((option) => (
            <Checkbox key={option.key} value={option.key}>
              {option.content}
            </Checkbox>
          ))}
        </div>
      </Checkbox.Group>
    );
  }

  if (item.answerMode === 'TRUE_FALSE') {
    return (
      <Radio.Group value={(savedAnswer.selectedOption as string | undefined) ?? undefined} disabled className={styles.choiceGroup}>
        <div className={styles.optionList}>
          {buildTrueFalseOptions(item.answerConfig).map((option: CandidateQuestionOption) => (
            <Radio key={option.key} value={option.key}>
              {option.content}
            </Radio>
          ))}
        </div>
      </Radio.Group>
    );
  }

  return (
    <Input.TextArea
      aria-label={`第 ${item.questionNo} 题作答内容`}
      rows={6}
      disabled
      value={(savedAnswer.textAnswer as string | undefined) ?? ''}
      className={styles.textAnswer}
    />
  );
}

export function CandidateScoreReportPage({ report, errorMessage, onBackToExamList, onLogout }: CandidateScoreReportPageProps) {
  return (
    <main className={styles.page}>
      <section className={styles.shell}>
        <header className={styles.header}>
          <div>
            <Typography.Text className={styles.eyebrow}>考试结果</Typography.Text>
            <Typography.Title level={1} className={styles.title}>
              成绩详情
            </Typography.Title>
            <Typography.Paragraph className={styles.description}>{report.name}</Typography.Paragraph>
          </div>
          <div className={styles.actions}>
            <Button onClick={onBackToExamList}>返回考试列表</Button>
            <Button onClick={onLogout}>退出登录</Button>
          </div>
        </header>

        {errorMessage ? <Alert type="error" showIcon role="alert" message={errorMessage} /> : null}

        <section className={styles.summaryGrid}>
          <article className={styles.summaryCard}>
            <span className={styles.label}>总分</span>
            <strong className={styles.score}>{`总分 ${report.totalScore}`}</strong>
            <span className={styles.value}>{report.scoreStatus === 'PUBLISHED' ? '已出分' : report.scoreStatus}</span>
          </article>
          <article className={styles.summaryCard}>
            <span className={styles.label}>试卷信息</span>
            <strong className={styles.value}>{report.paperName}</strong>
            <span className={styles.value}>{`考试时长 ${report.durationMinutes} 分钟`}</span>
          </article>
          <article className={styles.summaryCard}>
            <span className={styles.label}>作答摘要</span>
            <strong className={styles.value}>{`已答 ${report.answeredCount} / ${report.answeredCount + report.unansweredCount} 题`}</strong>
            <span className={styles.value}>{`提交时间 ${report.submittedAt}`}</span>
          </article>
        </section>

        <section className={styles.card}>
          <Typography.Title level={3} className={styles.sectionTitle}>
            作答摘要
          </Typography.Title>
          <div className={styles.itemList}>
            {report.items.map((item) => (
              <article key={item.paperQuestionId} className={styles.item}>
                <div className={styles.itemHeader}>
                  <strong>{`第 ${item.questionNo} 题`}</strong>
                  <span>{item.questionTypeName}</span>
                  <span>{`得分 ${item.awardedScore} / ${item.itemScore}`}</span>
                  <Tag color={item.judgeStatus === 'CORRECT' ? 'success' : item.judgeStatus === 'WRONG' ? 'error' : 'default'}>
                    {formatJudgeStatus(item.judgeStatus)}
                  </Tag>
                </div>
                <Typography.Paragraph className={styles.stem}>{item.questionStem}</Typography.Paragraph>
                <div className={styles.itemMeta}>
                  <span>{formatAnswerStatus(item.answerStatus)}</span>
                  <span>{item.answerSummary ?? '未作答'}</span>
                </div>
                <div className={styles.answerPanel}>{renderReadonlyAnswer(item)}</div>
                <Divider className={styles.divider} />
              </article>
            ))}
          </div>
        </section>
      </section>
    </main>
  );
}
