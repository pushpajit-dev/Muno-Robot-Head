import React from 'react';

interface MastheadProps {
  theme: 'dark' | 'light';
  onToggleTheme: () => void;
  onOpenDrawer: () => void;
  onToggleBottomSheet: () => void;
  isBottomSheetOpen: boolean;
}

export const Masthead: React.FC<MastheadProps> = ({
  theme,
  onToggleTheme,
  onOpenDrawer,
  onToggleBottomSheet,
  isBottomSheetOpen,
}) => {
  const isLight = theme === 'light';

  return (
    <header className="masthead">
      <div className="masthead-brand">
        <div className="muno-logo-wrap">
          <svg className="muno-logo-icon" viewBox="0 0 32 32" fill="none">
            <rect x="5" y="8" width="22" height="18" rx="6" fill="currentColor" fillOpacity="0.18" stroke="currentColor" strokeWidth="2" />
            <circle cx="16" cy="4" r="2" fill="currentColor" />
            <path d="M16 6V8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            <circle cx="11" cy="16" r="2" fill="currentColor" />
            <circle cx="21" cy="16" r="2" fill="currentColor" />
            <path d="M12 21C13.2 22.2 15 22.6 16 22.6C17 22.6 18.8 22.2 20 21" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            <rect x="2" y="14" width="3" height="6" rx="1.5" fill="currentColor" />
            <rect x="27" y="14" width="3" height="6" rx="1.5" fill="currentColor" />
          </svg>
          <div className="muno-title-group">
            <span className="muno-title">Muno Robo</span>
            <span className="muno-badge">STUDIO</span>
          </div>
        </div>
      </div>

      <div className="masthead-actions">
        {/* Toggle Bottom Sheet button */}
        <button
          className={`action-pill ${isBottomSheetOpen ? 'active' : ''}`}
          onClick={onToggleBottomSheet}
          title={isBottomSheetOpen ? "Collapse Properties" : "Expand Properties"}
          aria-label="Toggle Properties Bottom Sheet"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <rect x="3" y="3" width="18" height="18" rx="4" />
            <path d="M3 15H21" />
            <path d={isBottomSheetOpen ? "M10 19L12 17L14 19" : "M10 17L12 19L14 17"} />
          </svg>
          <span className="pill-text">Properties</span>
        </button>

        {/* Development Implementation Drawer (Sketchware / Blocks / JSON) */}
        <button
          className="action-pill action-pill-primary"
          onClick={onOpenDrawer}
          title="Open Sketchware Components & Blocks Drawer"
          aria-label="Open Development Implementation Drawer"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <path d="M10 13l-2 2 2 2" />
            <path d="M14 17l2-2-2-2" />
          </svg>
          <span className="pill-text">Dev Blocks & JSON</span>
        </button>

        {/* Theme Toggle (Dark / Light) */}
        <button
          className="theme-btn"
          onClick={onToggleTheme}
          aria-label={isLight ? 'Switch to dark mode' : 'Switch to light mode'}
          title={isLight ? 'Dark mode' : 'Light mode'}
        >
          {isLight ? (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <path d="M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5a8.5 8.5 0 1 0 11 11Z" />
            </svg>
          ) : (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
              <circle cx="12" cy="12" r="4" />
              <path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4" />
            </svg>
          )}
        </button>
      </div>
    </header>
  );
};
