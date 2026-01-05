declare module 'dom-anchor-text-position' {
    export function fromRange(root: Node, range: Range): { start: number, end: number };
    export function toRange(root: Node, selector: { start: number, end: number }): Range;
}
