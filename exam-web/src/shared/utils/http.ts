type ErrorResponseData = {
  message?: unknown;
  error?: unknown;
  detail?: unknown;
};

type ErrorWithResponse = {
  message?: unknown;
  response?: {
    data?: unknown;
  };
};

function pickMessage(candidate: unknown) {
  return typeof candidate === 'string' && candidate.trim() ? candidate.trim() : '';
}

export function extractErrorMessage(error: unknown, fallback: string) {
  if (typeof error === 'string') {
    return pickMessage(error) || fallback;
  }

  if (!error || typeof error !== 'object') {
    return fallback;
  }

  const errorLike = error as ErrorWithResponse;
  const responseData = errorLike.response?.data;

  if (typeof responseData === 'string') {
    return pickMessage(responseData) || fallback;
  }

  if (responseData && typeof responseData === 'object') {
    const responseBody = responseData as ErrorResponseData;
    return pickMessage(responseBody.message) || pickMessage(responseBody.error) || pickMessage(responseBody.detail) || fallback;
  }

  return pickMessage(errorLike.message) || fallback;
}
