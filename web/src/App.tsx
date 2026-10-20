import { useState, useEffect, useRef } from 'react';
import { RobotHead, type RobotHeadShape, type RobotHeadState } from './engine';
import { DEFAULTS, STATE_HINTS, type Colors } from './engine/constants';
import { Masthead } from './components/Masthead';
import { Dock } from './components/Dock';
import { EmotionsBar } from './components/EmotionsBar';
import { BottomSheet } from './components/BottomSheet';
import { DevDrawer } from './components/DevDrawer';

type Theme = 'dark' | 'light';

function useTheme() {
  const [theme, setTheme] = useState<Theme>(() => {
    try {
      const saved = localStorage.getItem('munorobo-theme') || localStorage.getItem('robot-heads-theme');
      if (saved === 'light' || saved === 'dark') return saved;
      return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
    } catch {
      return 'dark';
    }
  });

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try {
      localStorage.setItem('munorobo-theme', theme);
    } catch {}
  }, [theme]);

  const toggle = () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'));
  return [theme, toggle] as const;
}

function useStageSize() {
  const measure = () => {
    const w = window.innerWidth;
    const h = window.innerHeight;
    if (w < 600) return Math.max(150, Math.min(w - 80, 210));
    if (w < 900) return Math.max(180, Math.min(w - 120, 260));
    const room = Math.min(w - 400, h - 320);
    return Math.round(Math.max(190, Math.min(room * 0.65, 330)));
  };

  const [size, setSize] = useState(measure);
  useEffect(() => {
    const on = () => setSize(measure());
    window.addEventListener('resize', on);
    return () => window.removeEventListener('resize', on);
  }, []);
  return size;
}

const title = (s: string) => s[0].toUpperCase() + s.slice(1);

export default function App() {
  const [theme, toggleTheme] = useTheme();
  const [shape, setShape] = useState<RobotHeadShape>('rectangle');
  const [state, setState] = useState<RobotHeadState>('idle');
  const [colors, setColors] = useState<Colors>(DEFAULTS);
  const [speed, setSpeed] = useState<number>(1);
  const [antenna, setAntenna] = useState<boolean>(true);
  const [paused, setPaused] = useState<boolean>(false);

  // Bottom Sheet open / closed state
  const [isBottomSheetOpen, setIsBottomSheetOpen] = useState<boolean>(false);

  // Right-side Development Drawer state
  const [isDrawerOpen, setIsDrawerOpen] = useState<boolean>(false);

  const stageSize = useStageSize();
  const robotHeadRef = useRef<HTMLCanvasElement | null>(null);

  // Auto-save active session to localStorage
  useEffect(() => {
    try {
      const session = { shape, state, colors, speed, antenna, paused };
      localStorage.setItem('munorobo-active-session', JSON.stringify(session));
    } catch {}
  }, [shape, state, colors, speed, antenna, paused]);

  // Restore session on mount
  useEffect(() => {
    try {
      const saved = localStorage.getItem('munorobo-active-session');
      if (saved) {
        const s = JSON.parse(saved);
        if (s.shape) setShape(s.shape);
        if (s.state) setState(s.state);
        if (s.colors) setColors(s.colors);
        if (typeof s.speed === 'number') setSpeed(s.speed);
        if (typeof s.antenna === 'boolean') setAntenna(s.antenna);
      }
    } catch {}
  }, []);

  const handleColorChange = (key: keyof Colors, value: string) => {
    setColors((prev) => ({ ...prev, [key]: value }));
  };

  const handleReset = () => {
    setColors(DEFAULTS);
    setSpeed(1);
    setAntenna(true);
    setPaused(false);
  };

  const hasChanged =
    colors.color !== DEFAULTS.color ||
    colors.trimColor !== DEFAULTS.trimColor ||
    colors.screenColor !== DEFAULTS.screenColor ||
    speed !== 1 ||
    paused ||
    !antenna;

  // Poke the robot to trigger bounce/spin
  const handlePoke = () => {
    if (robotHeadRef.current) {
      robotHeadRef.current.parentElement?.click();
    }
  };

  const handleApplyConfig = (config: {
    shape?: RobotHeadShape;
    state?: RobotHeadState;
    colors?: Partial<Colors>;
    speed?: number;
    antenna?: boolean;
    poke?: boolean;
  }) => {
    if (config.shape) setShape(config.shape);
    if (config.state) setState(config.state);
    if (config.colors) setColors((c) => ({ ...c, ...config.colors }));
    if (typeof config.speed === 'number') setSpeed(config.speed);
    if (typeof config.antenna === 'boolean') setAntenna(config.antenna);
    if (config.poke) {
      setTimeout(handlePoke, 50);
    }
  };

  return (
    <div className="app-container">
      {/* Masthead Header with Muno Robo branding */}
      <Masthead
        theme={theme}
        onToggleTheme={toggleTheme}
        onOpenDrawer={() => setIsDrawerOpen(true)}
        onToggleBottomSheet={() => setIsBottomSheetOpen((prev) => !prev)}
        isBottomSheetOpen={isBottomSheetOpen}
      />

      {/* Main View Area */}
      <div className="main-layout">
        {/* Left Side: Shapes Dock */}
        <aside className="dock-sidebar">
          <Dock
            shape={shape}
            onChange={setShape}
            colors={colors}
            antenna={antenna}
            speed={speed}
          />
        </aside>

        {/* Center: Stage with large interactive Robot Head */}
        <main className="stage-area" aria-label="Muno Robo Interactive Stage">
          <div className="stage-canvas-wrap">
            <RobotHead
              ref={robotHeadRef}
              shape={shape}
              state={state}
              size={stageSize}
              paused={paused}
              speed={speed}
              antenna={antenna}
              floorShadow={true}
              interactive={true}
              {...colors}
            />
          </div>

          <div className="stage-caption">
            <h1 className="caption-state-title">{title(state)}</h1>
            <p className="caption-hint">{STATE_HINTS[state]}</p>
          </div>
        </main>
      </div>

      {/* Below Stage: Animated Robot Emotions Bar */}
      <footer className="emotions-footer">
        <EmotionsBar
          shape={shape}
          state={state}
          onChange={setState}
          colors={colors}
          antenna={antenna}
          paused={paused}
          speed={speed}
        />
      </footer>

      {/* Bottom Sheet View containing all properties and controls */}
      <BottomSheet
        isOpen={isBottomSheetOpen}
        onToggle={() => setIsBottomSheetOpen((prev) => !prev)}
        colors={colors}
        onColorChange={handleColorChange}
        antenna={antenna}
        onAntennaChange={setAntenna}
        speed={speed}
        onSpeedChange={setSpeed}
        paused={paused}
        onPausedChange={setPaused}
        shape={shape}
        state={state}
        onReset={handleReset}
        hasChanged={hasChanged}
      />

      {/* Right Side: Development Implementation Drawer */}
      <DevDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        shape={shape}
        state={state}
        colors={colors}
        speed={speed}
        antenna={antenna}
        onApplyConfig={handleApplyConfig}
        onPoke={handlePoke}
      />
    </div>
  );
}
