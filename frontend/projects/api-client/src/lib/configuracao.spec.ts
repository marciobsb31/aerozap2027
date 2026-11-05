import { describe, expect, it } from 'vitest';

import { semBarrasNoFim } from './configuracao';

describe('semBarrasNoFim', () => {
  it('tira todas as barras do fim e só as do fim', () => {
    expect(semBarrasNoFim('https://api.exemplo.test/v1///')).toBe('https://api.exemplo.test/v1');
    expect(semBarrasNoFim('https://api.exemplo.test/v1')).toBe('https://api.exemplo.test/v1');
    expect(semBarrasNoFim('///')).toBe('');
    expect(semBarrasNoFim('')).toBe('');
  });
});
