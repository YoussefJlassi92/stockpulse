/**
 * Global test setup for `npx vitest run`.
 *
 * Replicates what `@angular/build:unit-test` generates in its virtual
 * `init-testbed.js` entry point:
 *  - imports @angular/compiler so JIT compilation is available as a fallback
 *  - initialises Angular's TestBed with BrowserTestingModule once per suite
 *
 * TestBed teardown (destroyAfterEach: true by default in Angular 17+) handles
 * per-test cleanup automatically — no manual beforeEach/afterEach needed here.
 */

import '@angular/compiler';

import { getTestBed } from '@angular/core/testing';
import {
  BrowserTestingModule,
  platformBrowserTesting,
} from '@angular/platform-browser/testing';

const SETUP_KEY = Symbol.for('@angular/cli/testbed-setup');
if (!(globalThis as Record<symbol, unknown>)[SETUP_KEY]) {
  (globalThis as Record<symbol, unknown>)[SETUP_KEY] = true;

  getTestBed().initTestEnvironment(
    BrowserTestingModule,
    platformBrowserTesting(),
    {
      errorOnUnknownElements: true,
      errorOnUnknownProperties: true,
    }
  );
}
