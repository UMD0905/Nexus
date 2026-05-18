/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        canvas:  'var(--color-canvas)',
        surface: 'var(--color-surface)',
        overlay: 'var(--color-overlay)',
        accent:  {
          DEFAULT: '#6366f1',
          dim:     'rgba(99,102,241,0.15)',
          glow:    'rgba(99,102,241,0.35)',
        },
        fg: {
          DEFAULT: 'var(--color-fg)',
          muted:   'var(--color-fg-muted)',
          subtle:  'var(--color-fg-subtle)',
        },
        border:  'var(--color-border)',
        success: '#10b981',
        warning: '#f59e0b',
        danger:  '#ef4444',
        purple:  '#a78bfa',
      },
      fontFamily: { sans: ['Inter', 'system-ui', 'sans-serif'] },
      animation: {
        'fade-in':    'fadeIn 0.2s ease-out',
        'slide-in':   'slideIn 0.25s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4,0,0.6,1) infinite',
      },
      keyframes: {
        fadeIn:  { from: { opacity: '0', transform: 'translateY(6px)' }, to: { opacity: '1', transform: 'translateY(0)' } },
        slideIn: { from: { opacity: '0', transform: 'translateX(-10px)' }, to: { opacity: '1', transform: 'translateX(0)' } },
      },
      boxShadow: {
        'card':   '0 4px 24px rgba(0,0,0,0.25)',
        'glow':   '0 0 20px rgba(99,102,241,0.25)',
        'accent': '0 0 12px rgba(99,102,241,0.4)',
      },
    },
  },
  plugins: [],
}
