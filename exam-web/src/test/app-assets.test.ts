import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const currentFilePath = fileURLToPath(import.meta.url);
const currentDirPath = dirname(currentFilePath);
const indexHtmlPath = resolve(currentDirPath, '../../index.html');
const faviconPath = resolve(currentDirPath, '../../public/favicon.svg');

describe('app assets', () => {
  it('declares a favicon in index.html', () => {
    const indexHtml = readFileSync(indexHtmlPath, 'utf-8');

    expect(indexHtml).toContain('rel="icon"');
    expect(indexHtml).toContain('href="/favicon.svg"');
  });

  it('ships the favicon asset from the public directory', () => {
    expect(existsSync(faviconPath)).toBe(true);
  });
});
