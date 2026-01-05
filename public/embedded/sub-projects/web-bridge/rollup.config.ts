import type { RollupOptions } from 'rollup';
import typescript from '@rollup/plugin-typescript';
import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import terser from '@rollup/plugin-terser';
import serve from 'rollup-plugin-serve';
import livereload from 'rollup-plugin-livereload';

const isServe = process.env.SERVE === 'true';
const isProduction = !process.env.ROLLUP_WATCH;

const config: RollupOptions = {
  input: 'src/index.ts',
  output: [
    {
      file: 'dist/slax-reader-web-bridge.js',
      format: 'iife',
      name: 'SlaxReaderWebBridgeExports',
      sourcemap: false,
      footer: 'window.SlaxWebViewBridge = new SlaxReaderWebBridgeExports.SlaxWebViewBridge();'
    }
  ],
  plugins: [
    resolve(),
    commonjs(),
    typescript({
      tsconfig: './tsconfig.json',
      declaration: false,
      declarationMap: false,
    }),
    isProduction && terser({
      format: {
        comments: false,
        beautify: false
      },
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    }),
    isServe && serve({
      open: true,
      openPage: '/demo/index.html',
      contentBase: '.',
      port: 10001
    }),
    isServe && livereload({
      watch: 'dist'
    })
  ]
};

export default config;
