export const extractJsonFromResponse = (text) => {
  const trimmed = text.trim();
  if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
    return trimmed;
  }

  const fenceMatch = trimmed.match(/```(?:json)?\s*([\s\S]*?)```/i);
  if (fenceMatch) {
    return fenceMatch[1].trim();
  }
  return trimmed;
};

export const ensureArrayLength = (arr, min = 1, max = Number.MAX_SAFE_INTEGER) => {
  if (!Array.isArray(arr)) {
    throw new Error("AI response did not return an array of questions");
  }
  if (arr.length < min || arr.length > max) {
    throw new Error(`Expected between ${min} and ${max} questions, received ${arr.length}`);
  }
  return arr;
};
