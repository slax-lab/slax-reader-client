/**
 * 应用必要的 Polyfills 确保兼容性
 */
export function applyPolyfills() {
    if (!window.CSS || !window.CSS.escape) {
        window.CSS = window.CSS || {} as any;
        window.CSS.escape = function(value: any) {
            if (arguments.length === 0) {
                throw new TypeError('`CSS.escape` requires an argument.');
            }
            var string = String(value);
            var length = string.length;
            var index = -1;
            var codeUnit;
            var result = '';
            while (++index < length) {
                codeUnit = string.charCodeAt(index);
                // 注意：处理代理对等
                if (codeUnit === 0x0000) {
                    result += '\uFFFD';
                    continue;
                }
                if (
                    // 如果字符是 [0-9A-Za-z_-]
                    (codeUnit >= 0x0030 && codeUnit <= 0x0039) ||
                    (codeUnit >= 0x0041 && codeUnit <= 0x005A) ||
                    (codeUnit >= 0x0061 && codeUnit <= 0x007A) ||
                    codeUnit === 0x005F ||
                    codeUnit === 0x002D
                ) {
                    result += string.charAt(index);
                    continue;
                }
                // 转义其他字符
                result += '\\' + string.charAt(index);
            }
            return result;
        };
    }
}
