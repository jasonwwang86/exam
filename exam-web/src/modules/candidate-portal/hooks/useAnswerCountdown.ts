import { useEffect, useState } from 'react';

export function useAnswerCountdown(deadlineAt: string, sessionStatus: string) {
  const calculateRemainingSeconds = () => {
    if (sessionStatus === 'TIME_EXPIRED') {
      return 0;
    }
    return Math.max(0, Math.floor((new Date(deadlineAt).getTime() - Date.now()) / 1000));
  };

  const [remainingSeconds, setRemainingSeconds] = useState(calculateRemainingSeconds);

  useEffect(() => {
    setRemainingSeconds(calculateRemainingSeconds());
    const timerId = window.setInterval(() => {
      setRemainingSeconds(calculateRemainingSeconds());
    }, 1000);

    return () => {
      window.clearInterval(timerId);
    };
  }, [deadlineAt, sessionStatus]);

  return {
    remainingSeconds,
    expired: remainingSeconds <= 0 || sessionStatus === 'TIME_EXPIRED',
  };
}
