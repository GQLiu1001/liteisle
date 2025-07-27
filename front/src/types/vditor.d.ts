declare module 'vditor' {
  export default class Vditor {
    constructor(element: HTMLElement | string, options?: any)
    getValue(): string
    setValue(value: string): void
    destroy(): void
    focus(): void
    blur(): void
    disabled(): void
    enable(): void
    insertValue(value: string, render?: boolean): void
    clearStack(): void
    renderPreview(value?: string): void
    getCursorPosition(): { top: number; left: number }
    deleteValue(): void
    updateValue(value: string): void
    isUploading(): boolean
    clearCache(): void
    disabledCache(): void
    enableCache(): void
    html2md(value: string): string
    tip(text: string, time: number): void
    setPreviewMode(mode: 'both' | 'editor'): void
    setTheme(theme: 'dark' | 'classic', contentTheme?: string, codeTheme?: string, contentThemePath?: string): void
    getCurrentMode(): string
  }
}

declare module 'vditor/dist/method.min' {
  export function preview(element: HTMLElement, markdown: string, options?: any): void
  export function md2html(mdText: string, options?: any): Promise<string>
  export function previewImage(oldImgElement: HTMLImageElement, lang?: string, theme?: string): void
  export function mermaidRender(element: HTMLElement, cdn?: string, theme?: string): void
  export function codeRender(element: HTMLElement, option?: any): void
  export function chartRender(element?: HTMLElement | Document, cdn?: string, theme?: string): void
  export function mindmapRender(element?: HTMLElement | Document, cdn?: string, theme?: string): void
  export function plantumlRender(element?: HTMLElement | Document, cdn?: string): void
  export function abcRender(element?: HTMLElement | Document, cdn?: string): void
  export function highlightRender(hljsOption?: any, element?: HTMLElement | Document, cdn?: string): void
  export function mediaRender(element: HTMLElement): void
  export function mathRender(element: HTMLElement, options?: any): void
  export function speechRender(element: HTMLElement, lang?: string): void
  export function graphvizRender(element: HTMLElement, cdn?: string): void
  export function outlineRender(contentElement: HTMLElement, targetElement: Element): void
  export function lazyLoadImageRender(element?: HTMLElement | Document): void
  export function setCodeTheme(codeTheme: string, cdn?: string): void
  export function setContentTheme(contentTheme: string, path: string): void
}

declare module 'vditor/dist/index.css' 