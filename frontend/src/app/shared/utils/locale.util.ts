export function normalizeLang(code: string): string {
  const [lang, region] = (code || '').trim().split('-', 2);
  const l = (lang || '').toLowerCase();
  const r = region ? region.toUpperCase() : '';
  return r ? `${l}-${r}` : l;
}
