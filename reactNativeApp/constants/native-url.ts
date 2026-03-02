import * as Linking from 'expo-linking';

type NativeEntry = {
  pathname: string;
  params: Record<string, string>;
};

const DEFAULT_PATHNAME = '/feedback';

function normalizePath(path: string): string {
  const cleaned = path.replace(/^\/+|\/+$/g, '');
  if (!cleaned) return DEFAULT_PATHNAME;
  return `/${cleaned}`;
}

function normalizeQueryParams(
  queryParams: ReturnType<typeof Linking.parse>['queryParams'],
): Record<string, string> {
  if (!queryParams) return {};
  return Object.entries(queryParams).reduce<Record<string, string>>((acc, [key, value]) => {
    if (value == null) return acc;
    acc[key] = Array.isArray(value) ? value.join(',') : String(value);
    return acc;
  }, {});
}

export function resolveNativeEntry(url?: string): NativeEntry {
  if (!url) {
    return { pathname: DEFAULT_PATHNAME, params: {} };
  }

  try {
    const parsed = Linking.parse(url);
    const pathParts = [parsed.hostname, parsed.path].filter(
      (part): part is string => typeof part === 'string' && part.length > 0,
    );
    const pathname = normalizePath(pathParts.join('/'));
    const params = normalizeQueryParams(parsed.queryParams);
    return { pathname, params };
  } catch {
    return { pathname: DEFAULT_PATHNAME, params: {} };
  }
}
