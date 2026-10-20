import React, { useState } from 'react';
import type { RobotHeadShape, RobotHeadState } from '../engine';
import { SWATCHES, ROW_LABELS, SPEEDS, DEFAULTS, type Colors } from '../engine/constants';

interface BottomSheetProps {
  isOpen: boolean;
  onToggle: () => void;
  colors: Colors;
  onColorChange: (key: keyof Colors, value: string) => void;
  antenna: boolean;
  onAntennaChange: (antenna: boolean) => void;
  speed: number;
  onSpeedChange: (speed: number) => void;
  paused: boolean;
  onPausedChange: (paused: boolean) => void;
  shape: RobotHeadShape;
  state: RobotHeadState;
  onReset: () => void;
  hasChanged: boolean;
}

export const BottomSheet: React.FC<BottomSheetProps> = ({
  isOpen,
  onToggle,
  colors,
  onColorChange,
  antenna,
  onAntennaChange,
  speed,
  onSpeedChange,
  paused,
  onPausedChange,
  shape,
  state,
  onReset,
  hasChanged,
}) => {
  const [copiedSnippet, setCopiedSnippet] = useState(false);

  // Generate code snippet
  const snippetLines = [
    `shape="${shape}"`,
    `state="${state}"`,
    colors.color !== DEFAULTS.color ? `color="${colors.color}"` : null,
    colors.trimColor !== DEFAULTS.trimColor ? `trimColor="${colors.trimColor}"` : null,
    colors.screenColor !== DEFAULTS.screenColor ? `screenColor="${colors.screenColor}"` : null,
    speed !== 1 ? `speed={${speed}}` : null,
    !antenna ? `antenna={false}` : null,
  ].filter(Boolean);

  const snippetCode = `<RobotHead\n  ${snippetLines.join('\n  ')}\n/>`;

  const handleCopy = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(snippetCode);
      setCopiedSnippet(true);
      setTimeout(() => setCopiedSnippet(false), 1600);
    }
  };

  const getKnownLabel = (name: keyof Colors, value: string) => {
    const item = SWATCHES[name].find(([, hex]) => hex.toLowerCase() === value.toLowerCase());
    return item ? item[0] : value;
  };

  return (
    <div className={`bottom-sheet-container ${isOpen ? 'expanded' : 'collapsed'}`}>
      {/* Backdrop when sheet is expanded */}
      {isOpen && <div className="bottom-sheet-backdrop" onClick={onToggle} />}

      <div className="bottom-sheet">
        {/* Grab Handle & Peek Summary Bar */}
        <div className="sheet-handle-bar" onClick={onToggle} role="button" tabIndex={0}>
          <div className="drag-indicator" />
          <div className="sheet-peek-summary">
            <div className="peek-title">
              <span className="peek-label">Robot Properties</span>
              <span className="peek-tag">{isOpen ? 'Tap to minimize' : 'Tap to customize'}</span>
            </div>
            <div className="peek-pills">
              <span className="color-dot-pill" title={`Shell: ${colors.color}`}>
                <span className="dot" style={{ background: colors.color }} />
                <span>{getKnownLabel('color', colors.color)}</span>
              </span>
              <span className="color-dot-pill" title={`Screen: ${colors.screenColor}`}>
                <span className="dot" style={{ background: colors.screenColor }} />
                <span>{getKnownLabel('screenColor', colors.screenColor)}</span>
              </span>
              <span className="stat-pill">{antenna ? 'Antenna ON' : 'Antenna OFF'}</span>
              <span className="stat-pill">{speed}×</span>
            </div>
            <button className="sheet-toggle-icon" aria-label={isOpen ? 'Collapse sheet' : 'Expand sheet'}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                <path d={isOpen ? "M6 9l6 6 6-6" : "M18 15l-6-6-6 6"} />
              </svg>
            </button>
          </div>
        </div>

        {/* Expanded Sheet Content */}
        <div className="sheet-content">
          <div className="sheet-header">
            <h3>Configuration & Swatches</h3>
            <div className="sheet-header-actions">
              {hasChanged && (
                <button className="sheet-btn reset-btn" onClick={onReset}>
                  Reset Defaults
                </button>
              )}
              <button className="sheet-btn icon-only" onClick={onToggle} aria-label="Close sheet">
                ✕
              </button>
            </div>
          </div>

          <div className="sheet-grid">
            {/* Color controls */}
            {(Object.keys(SWATCHES) as Array<keyof Colors>).map((key) => {
              const currentValue = colors[key];
              const known = SWATCHES[key].find(([, hex]) => hex.toLowerCase() === currentValue.toLowerCase());
              return (
                <div key={key} className="sheet-section">
                  <div className="section-head">
                    <span className="section-title">{ROW_LABELS[key]} Color</span>
                    <span className="section-badge">{known ? known[0] : currentValue}</span>
                  </div>
                  <div className="swatch-list">
                    {SWATCHES[key].map(([label, hex]) => {
                      const isSelected = hex.toLowerCase() === currentValue.toLowerCase();
                      return (
                        <button
                          key={hex}
                          className={`swatch-btn ${isSelected ? 'selected' : ''}`}
                          style={{ background: hex }}
                          title={label}
                          aria-label={`${ROW_LABELS[key]}: ${label}`}
                          aria-pressed={isSelected}
                          onClick={() => onColorChange(key, hex)}
                        />
                      );
                    })}
                    {/* Custom Color Input */}
                    <label
                      className={`swatch-btn swatch-custom-btn ${!known ? 'selected' : ''}`}
                      title="Custom Color"
                      style={known ? undefined : { background: currentValue }}
                    >
                      <input
                        type="color"
                        value={currentValue}
                        onChange={(e) => onColorChange(key, e.target.value)}
                        aria-label={`${ROW_LABELS[key]}: Custom color picker`}
                      />
                    </label>
                  </div>
                </div>
              );
            })}

            {/* Antenna & Animation Speed */}
            <div className="sheet-section-row">
              {/* Antenna */}
              <div className="sheet-section">
                <div className="section-head">
                  <span className="section-title">Antenna</span>
                </div>
                <div className="segment-group">
                  <button
                    className={`segment-btn ${antenna ? 'active' : ''}`}
                    onClick={() => onAntennaChange(true)}
                  >
                    On
                  </button>
                  <button
                    className={`segment-btn ${!antenna ? 'active' : ''}`}
                    onClick={() => onAntennaChange(false)}
                  >
                    Off
                  </button>
                </div>
              </div>

              {/* Speed & Pause */}
              <div className="sheet-section">
                <div className="section-head">
                  <span className="section-title">Speed</span>
                  <button
                    className="pause-text-btn"
                    onClick={() => onPausedChange(!paused)}
                  >
                    {paused ? '▶ Play' : '⏸ Pause'}
                  </button>
                </div>
                <div className="segment-group">
                  {SPEEDS.map((s) => (
                    <button
                      key={s}
                      className={`segment-btn ${speed === s ? 'active' : ''}`}
                      onClick={() => onSpeedChange(s)}
                    >
                      {s}×
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Code Snippet */}
            <div className="sheet-section snippet-section">
              <div className="section-head">
                <span className="section-title">React / Component Code</span>
                <button className="copy-code-btn" onClick={handleCopy}>
                  {copiedSnippet ? '✓ Copied!' : 'Copy Code'}
                </button>
              </div>
              <pre className="code-block">
                <code>{snippetCode}</code>
              </pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
