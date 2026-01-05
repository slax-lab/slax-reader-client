import type { JestConfigWithTsJest } from 'ts-jest';

const config: JestConfigWithTsJest = {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/tests/setup.ts'],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  transform: {
    '^.+\\.tsx?$': ['ts-jest', {
      tsconfig: 'tsconfig.json',
    }],
    '^.+\\.js$': ['ts-jest', {
      tsconfig: {
        allowJs: true,
      },
      useESM: true,
    }],
  },
  transformIgnorePatterns: [
    'node_modules/(?!.*approx-string-match)',
  ],
};

export default config;
