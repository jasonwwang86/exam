const TRACE_NO_HEADER = 'TraceNo';
const TRACE_NO_LENGTH = 32;

function createTraceNo() {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID().replace(/-/g, '');
  }

  return `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}${Math.random().toString(16).slice(2)}`
    .slice(0, TRACE_NO_LENGTH)
    .padEnd(TRACE_NO_LENGTH, '0');
}

export function withTraceNo(headers: Record<string, string> = {}) {
  return {
    headers: {
      ...headers,
      [TRACE_NO_HEADER]: createTraceNo(),
    },
  };
}
