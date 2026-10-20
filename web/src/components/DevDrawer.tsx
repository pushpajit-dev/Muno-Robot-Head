import React, { useState, useEffect } from 'react';
import type { RobotHeadShape, RobotHeadState } from '../engine';
import { SHAPE_LABELS, type Colors } from '../engine/constants';

export interface CustomBlock {
  id: string;
  name: string;
  type: 'event' | 'action' | 'setter';
  category: 'motion' | 'emotion' | 'color' | 'setup';
  colorCode: string;
  shape?: RobotHeadShape;
  state?: RobotHeadState;
  shellColor?: string;
  screenColor?: string;
  speed?: number;
  antenna?: boolean;
  poke?: boolean;
}

export interface SavedProject {
  id: string;
  name: string;
  updatedAt: string;
  shape: RobotHeadShape;
  state: RobotHeadState;
  colors: Colors;
  speed: number;
  antenna: boolean;
}

interface DevDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  shape: RobotHeadShape;
  state: RobotHeadState;
  colors: Colors;
  speed: number;
  antenna: boolean;
  onApplyConfig: (config: {
    shape?: RobotHeadShape;
    state?: RobotHeadState;
    colors?: Partial<Colors>;
    speed?: number;
    antenna?: boolean;
    poke?: boolean;
  }) => void;
  onPoke: () => void;
}

const DEFAULT_BLOCKS: CustomBlock[] = [
  {
    id: 'b-happy-dance',
    name: 'Happy Jump & Glow',
    type: 'action',
    category: 'emotion',
    colorCode: '#4CAF50',
    state: 'happy',
    speed: 1.5,
    poke: true,
  },
  {
    id: 'b-matrix-mode',
    name: 'Matrix Hacker State',
    type: 'setter',
    category: 'color',
    colorCode: '#00E676',
    shape: 'hexagon',
    state: 'searching',
    shellColor: '#3a3d44',
    screenColor: '#7dff9a',
  },
  {
    id: 'b-thinking-deep',
    name: 'Deep AI Thinking',
    type: 'action',
    category: 'emotion',
    colorCode: '#2196F3',
    state: 'thinking',
    speed: 1,
  },
  {
    id: 'b-cyberpunk-disco',
    name: 'Cyberpunk Neon Pulse',
    type: 'setter',
    category: 'color',
    colorCode: '#E91E63',
    shape: 'circle',
    state: 'speaking',
    shellColor: '#6a45b8',
    screenColor: '#ff8fd0',
  },
  {
    id: 'b-error-alert',
    name: 'System Error Alert',
    type: 'action',
    category: 'emotion',
    colorCode: '#F44336',
    state: 'error',
    speed: 2,
    poke: true,
  },
];

export const DevDrawer: React.FC<DevDrawerProps> = ({
  isOpen,
  onClose,
  shape,
  state,
  colors,
  speed,
  antenna,
  onApplyConfig,
  onPoke,
}) => {
  const [activeTab, setActiveTab] = useState<'blocks' | 'creator' | 'phase-json' | 'projects' | 'export'>('blocks');

  // Blocks management
  const [blocks, setBlocks] = useState<CustomBlock[]>(() => {
    try {
      const saved = localStorage.getItem('munorobo-custom-blocks');
      return saved ? JSON.parse(saved) : DEFAULT_BLOCKS;
    } catch {
      return DEFAULT_BLOCKS;
    }
  });

  // Flow block sequence for execution
  const [flowSequence, setFlowSequence] = useState<CustomBlock[]>([
    DEFAULT_BLOCKS[0],
    DEFAULT_BLOCKS[1],
  ]);
  const [isRunningFlow, setIsRunningFlow] = useState(false);

  // New Block Form state
  const [newBlockName, setNewBlockName] = useState('');
  const [newBlockType, setNewBlockType] = useState<'action' | 'setter'>('action');
  const [newBlockCategory, setNewBlockCategory] = useState<'motion' | 'emotion' | 'color' | 'setup'>('emotion');
  const [newBlockColor, setNewBlockColor] = useState('#2196F3');
  const [newBlockShape, setNewBlockShape] = useState<RobotHeadShape>(shape);
  const [newBlockState, setNewBlockState] = useState<RobotHeadState>(state);
  const [newBlockPoke, setNewBlockPoke] = useState(true);

  // Phase JSON editor
  const [jsonText, setJsonText] = useState('');
  const [jsonStatus, setJsonStatus] = useState<{ message: string; isError: boolean } | null>(null);

  // Projects & Sessions management
  const [projects, setProjects] = useState<SavedProject[]>(() => {
    try {
      const saved = localStorage.getItem('munorobo-saved-projects');
      if (saved) return JSON.parse(saved);
    } catch {
      // fallback
    }
    return [
      {
        id: 'p-1',
        name: 'Cyber Cobalt Bot',
        updatedAt: new Date().toLocaleDateString(),
        shape: 'rectangle',
        state: 'idle',
        colors: { color: '#2b49a3', trimColor: '#93a6c8', screenColor: '#e8f2ff' },
        speed: 1,
        antenna: true,
      },
      {
        id: 'p-2',
        name: 'Matrix Scout',
        updatedAt: new Date().toLocaleDateString(),
        shape: 'hexagon',
        state: 'searching',
        colors: { color: '#3a3d44', trimColor: '#93a6c8', screenColor: '#7dff9a' },
        speed: 1.5,
        antenna: true,
      },
      {
        id: 'p-3',
        name: 'Bubblegum Happy',
        updatedAt: new Date().toLocaleDateString(),
        shape: 'circle',
        state: 'happy',
        colors: { color: '#d9558f', trimColor: '#c4c9d2', screenColor: '#ff8fd0' },
        speed: 1.5,
        antenna: true,
      },
    ];
  });
  const [projectNameInput, setProjectNameInput] = useState('');
  const [projectMessage, setProjectMessage] = useState<string | null>(null);

  // Sync blocks to localStorage
  useEffect(() => {
    try {
      localStorage.setItem('munorobo-custom-blocks', JSON.stringify(blocks));
    } catch {}
  }, [blocks]);

  // Sync projects to localStorage
  useEffect(() => {
    try {
      localStorage.setItem('munorobo-saved-projects', JSON.stringify(projects));
    } catch {}
  }, [projects]);

  // Generate Phase JSON whenever current state changes
  useEffect(() => {
    const phaseJson = {
      $schema: 'https://munorobo.dev/schema/v1/component-phase.json',
      componentName: 'MunoRoboHead',
      version: '1.0.0',
      description: 'Animated 3D glossy TV-headed robot with LED matrix display for Sketchware and Android Studio',
      activeConfiguration: {
        shape,
        state,
        speed,
        antenna,
        colors: {
          shell: colors.color,
          trim: colors.trimColor,
          screen: colors.screenColor,
        },
      },
      supportedAssets: {
        shapes: ['rectangle', 'square', 'circle', 'hexagon'],
        emotions: ['idle', 'thinking', 'searching', 'listening', 'speaking', 'working', 'happy', 'error', 'sleeping'],
        ledDisplay: {
          resolution: '16x8',
          animated: true,
          type: 'canvas_shader',
        },
      },
      sketchwareComponentDefinition: {
        typeName: 'MunoRoboView',
        category: 'Custom Views & Agents',
        icon: 'robot_head_icon',
        events: ['onInit', 'onStateChange', 'onTouchPoke', 'onAnimationCycle'],
        methods: [
          { name: 'setShape', args: ['shapeName: String'] },
          { name: 'setEmotion', args: ['emotion: String'] },
          { name: 'setColors', args: ['shell: Int', 'trim: Int', 'screen: Int'] },
          { name: 'poke', args: [] },
          { name: 'setSpeed', args: ['speedMultiplier: Float'] },
          { name: 'toggleAntenna', args: ['enabled: Boolean'] },
        ],
      },
    };
    setJsonText(JSON.stringify(phaseJson, null, 2));
  }, [shape, state, colors, speed, antenna]);

  // Run Flow Sequence step by step
  const handleRunFlow = async () => {
    if (isRunningFlow || flowSequence.length === 0) return;
    setIsRunningFlow(true);

    for (let i = 0; i < flowSequence.length; i++) {
      const blk = flowSequence[i];
      onApplyConfig({
        shape: blk.shape,
        state: blk.state,
        colors: {
          ...(blk.shellColor ? { color: blk.shellColor } : {}),
          ...(blk.screenColor ? { screenColor: blk.screenColor } : {}),
        },
        speed: blk.speed,
        antenna: blk.antenna,
        poke: blk.poke,
      });
      // Pause 900ms between steps to show each visual state
      await new Promise((r) => setTimeout(r, 900));
    }

    setIsRunningFlow(false);
  };

  // Add block to Flow
  const handleAddToFlow = (block: CustomBlock) => {
    setFlowSequence((prev) => [...prev, block]);
  };

  const handleRemoveFromFlow = (index: number) => {
    setFlowSequence((prev) => prev.filter((_, i) => i !== index));
  };

  // Create new custom block
  const handleCreateBlock = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newBlockName.trim()) return;

    const created: CustomBlock = {
      id: 'blk-' + Date.now(),
      name: newBlockName.trim(),
      type: newBlockType,
      category: newBlockCategory,
      colorCode: newBlockColor,
      shape: newBlockShape,
      state: newBlockState,
      shellColor: colors.color,
      screenColor: colors.screenColor,
      speed,
      antenna,
      poke: newBlockPoke,
    };

    setBlocks((prev) => [created, ...prev]);
    setNewBlockName('');
    setActiveTab('blocks');
  };

  // Rename block
  const handleRenameBlock = (id: string, currentName: string) => {
    const updated = window.prompt('Rename block:', currentName);
    if (updated && updated.trim()) {
      setBlocks((prev) => prev.map((b) => (b.id === id ? { ...b, name: updated.trim() } : b)));
      setFlowSequence((prev) => prev.map((b) => (b.id === id ? { ...b, name: updated.trim() } : b)));
    }
  };

  // Remove block
  const handleRemoveBlock = (id: string) => {
    if (window.confirm('Delete this block?')) {
      setBlocks((prev) => prev.filter((b) => b.id !== id));
      setFlowSequence((prev) => prev.filter((b) => b.id !== id));
    }
  };

  // Share block
  const handleShareBlock = (block: CustomBlock) => {
    const blockJson = JSON.stringify(block, null, 2);
    if (navigator.share) {
      navigator.share({
        title: `Muno Robo Block: ${block.name}`,
        text: blockJson,
      }).catch(() => {});
    } else if (navigator.clipboard) {
      navigator.clipboard.writeText(blockJson);
      alert(`Block "${block.name}" JSON copied to clipboard!`);
    }
  };

  // Duplicate block
  const handleDuplicateBlock = (block: CustomBlock) => {
    const cloned: CustomBlock = {
      ...block,
      id: 'blk-' + Date.now(),
      name: `${block.name} (Copy)`,
    };
    setBlocks((prev) => [cloned, ...prev]);
  };

  // Apply JSON from Editor
  const handleApplyJson = () => {
    try {
      const parsed = JSON.parse(jsonText);
      const conf = parsed.activeConfiguration || parsed;
      onApplyConfig({
        shape: conf.shape,
        state: conf.state,
        colors: {
          color: conf.colors?.shell || conf.color,
          trimColor: conf.colors?.trim || conf.trimColor,
          screenColor: conf.colors?.screen || conf.screenColor,
        },
        speed: typeof conf.speed === 'number' ? conf.speed : undefined,
        antenna: typeof conf.antenna === 'boolean' ? conf.antenna : undefined,
      });
      setJsonStatus({ message: '✓ Phase JSON successfully applied to Muno Robo!', isError: false });
      setTimeout(() => setJsonStatus(null), 3000);
    } catch (err: any) {
      setJsonStatus({ message: `JSON Parse Error: ${err.message}`, isError: true });
    }
  };

  // Save current project
  const handleSaveProject = () => {
    const name = projectNameInput.trim() || `Project #${projects.length + 1}`;
    const newProj: SavedProject = {
      id: 'proj-' + Date.now(),
      name,
      updatedAt: new Date().toLocaleDateString(),
      shape,
      state,
      colors: { ...colors },
      speed,
      antenna,
    };
    setProjects([newProj, ...projects]);
    setProjectNameInput('');
    setProjectMessage(`Project "${name}" saved!`);
    setTimeout(() => setProjectMessage(null), 2500);
  };

  // Load project
  const handleLoadProject = (p: SavedProject) => {
    onApplyConfig({
      shape: p.shape,
      state: p.state,
      colors: p.colors,
      speed: p.speed,
      antenna: p.antenna,
    });
    setProjectMessage(`Loaded "${p.name}"`);
    setTimeout(() => setProjectMessage(null), 2500);
  };

  // Delete project
  const handleDeleteProject = (id: string) => {
    if (window.confirm('Delete this project?')) {
      setProjects((prev) => prev.filter((p) => p.id !== id));
    }
  };

  // Rename project
  const handleRenameProject = (id: string, currentName: string) => {
    const nextName = window.prompt('Rename project:', currentName);
    if (nextName && nextName.trim()) {
      setProjects((prev) => prev.map((p) => (p.id === id ? { ...p, name: nextName.trim() } : p)));
    }
  };

  if (!isOpen) return null;

  return (
    <div className="dev-drawer-backdrop" onClick={onClose}>
      <div className="dev-drawer" onClick={(e) => e.stopPropagation()}>
        {/* Drawer Header */}
        <div className="drawer-header">
          <div className="drawer-brand">
            <span className="drawer-badge">DEV STUDIO</span>
            <h2>Sketchware & Blocks</h2>
          </div>
          <button className="drawer-close-btn" onClick={onClose} aria-label="Close drawer">
            ✕
          </button>
        </div>

        {/* Drawer Tabs */}
        <div className="drawer-tabs">
          <button
            className={`tab-btn ${activeTab === 'blocks' ? 'active' : ''}`}
            onClick={() => setActiveTab('blocks')}
          >
            Blocks & Flow
          </button>
          <button
            className={`tab-btn ${activeTab === 'creator' ? 'active' : ''}`}
            onClick={() => setActiveTab('creator')}
          >
            + New Block
          </button>
          <button
            className={`tab-btn ${activeTab === 'phase-json' ? 'active' : ''}`}
            onClick={() => setActiveTab('phase-json')}
          >
            Phase JSON
          </button>
          <button
            className={`tab-btn ${activeTab === 'projects' ? 'active' : ''}`}
            onClick={() => setActiveTab('projects')}
          >
            Projects
          </button>
          <button
            className={`tab-btn ${activeTab === 'export' ? 'active' : ''}`}
            onClick={() => setActiveTab('export')}
          >
            Android Code
          </button>
        </div>

        {/* Tab 1: Blocks & Visual Flow */}
        {activeTab === 'blocks' && (
          <div className="drawer-tab-content">
            <div className="flow-workbench">
              <div className="workbench-head">
                <span className="workbench-title">⚡ Interactive Execution Flow</span>
                <button
                  className={`run-flow-btn ${isRunningFlow ? 'running' : ''}`}
                  onClick={handleRunFlow}
                  disabled={isRunningFlow || flowSequence.length === 0}
                >
                  {isRunningFlow ? '⏳ Executing...' : '▶ Run Sequence'}
                </button>
              </div>

              {/* Connected Sketchware-style Flow Sequence */}
              <div className="block-sequence-container">
                {/* Initial Event Hat Block */}
                <div className="sketchware-hat-block">
                  <div className="hat-arch" />
                  <div className="hat-body">
                    <span className="block-icon">🏁</span>
                    <span className="block-text">when MunoRobo [Triggered]</span>
                  </div>
                </div>

                {/* Snapped Sequence of Action Blocks */}
                {flowSequence.map((b, idx) => (
                  <div
                    key={`${b.id}-${idx}`}
                    className="sketchware-block"
                    style={{ borderColor: b.colorCode }}
                  >
                    <div className="puzzle-tab-top" style={{ backgroundColor: b.colorCode }} />
                    <div className="block-main" style={{ backgroundColor: `${b.colorCode}22` }}>
                      <span className="notch-bullet" style={{ backgroundColor: b.colorCode }} />
                      <div className="block-details">
                        <span className="block-name" style={{ color: b.colorCode }}>{b.name}</span>
                        <span className="block-params">
                          {b.shape && `shape=${b.shape} `}
                          {b.state && `state=${b.state} `}
                          {b.poke && `(hop & spin) `}
                          {b.screenColor && `led=${b.screenColor}`}
                        </span>
                      </div>
                      <button
                        className="block-seq-delete"
                        onClick={() => handleRemoveFromFlow(idx)}
                        title="Remove from flow"
                      >
                        ✕
                      </button>
                    </div>
                    <div className="puzzle-tab-bottom" style={{ backgroundColor: b.colorCode }} />
                  </div>
                ))}

                {flowSequence.length === 0 && (
                  <div className="empty-flow-hint">
                    Tap "+ Add to Flow" on any block below to construct your robot behavior flow!
                  </div>
                )}
              </div>
            </div>

            {/* Block Library */}
            <div className="block-library-section">
              <div className="library-head">
                <span className="library-title">📦 Block Library & Components</span>
                <span className="library-count">{blocks.length} blocks</span>
              </div>

              <div className="blocks-grid">
                {blocks.map((b) => (
                  <div key={b.id} className="library-block-card">
                    <div className="lib-block-header" style={{ borderLeftColor: b.colorCode }}>
                      <div className="lib-block-info">
                        <span className="lib-block-title">{b.name}</span>
                        <span className="lib-block-badge" style={{ backgroundColor: `${b.colorCode}26`, color: b.colorCode }}>
                          {b.category.toUpperCase()}
                        </span>
                      </div>
                      <div className="lib-block-actions">
                        <button
                          className="card-action-btn"
                          title="Run now"
                          onClick={() => {
                            onApplyConfig({
                              shape: b.shape,
                              state: b.state,
                              colors: {
                                ...(b.shellColor ? { color: b.shellColor } : {}),
                                ...(b.screenColor ? { screenColor: b.screenColor } : {}),
                              },
                              speed: b.speed,
                              antenna: b.antenna,
                              poke: b.poke,
                            });
                          }}
                        >
                          ▶
                        </button>
                        <button
                          className="card-action-btn"
                          title="Add to sequence"
                          onClick={() => handleAddToFlow(b)}
                        >
                          +
                        </button>
                        <button
                          className="card-action-btn"
                          title="Share"
                          onClick={() => handleShareBlock(b)}
                        >
                          ↗
                        </button>
                        <button
                          className="card-action-btn"
                          title="Rename"
                          onClick={() => handleRenameBlock(b.id, b.name)}
                        >
                          ✎
                        </button>
                        <button
                          className="card-action-btn"
                          title="Duplicate"
                          onClick={() => handleDuplicateBlock(b)}
                        >
                          ⧉
                        </button>
                        <button
                          className="card-action-btn danger"
                          title="Delete"
                          onClick={() => handleRemoveBlock(b.id)}
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: New Block Creator */}
        {activeTab === 'creator' && (
          <div className="drawer-tab-content">
            <form className="block-creator-form" onSubmit={handleCreateBlock}>
              <div className="form-head">
                <h3>Create Custom Sketchware Block</h3>
                <p>Define a reusable block component to animate and customize Muno Robo.</p>
              </div>

              <div className="form-group">
                <label>Block Title</label>
                <input
                  type="text"
                  placeholder="e.g. Turbo Spin Alert"
                  value={newBlockName}
                  onChange={(e) => setNewBlockName(e.target.value)}
                  required
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Category</label>
                  <select
                    value={newBlockCategory}
                    onChange={(e) => {
                      const cat = e.target.value as any;
                      setNewBlockCategory(cat);
                      if (cat === 'motion') setNewBlockColor('#2196F3');
                      if (cat === 'emotion') setNewBlockColor('#4CAF50');
                      if (cat === 'color') setNewBlockColor('#9C27B0');
                      if (cat === 'setup') setNewBlockColor('#FF9800');
                    }}
                  >
                    <option value="emotion">Emotion</option>
                    <option value="motion">Motion</option>
                    <option value="color">Color Palette</option>
                    <option value="setup">Component Setup</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Block Accent Color</label>
                  <div className="color-preview-input">
                    <input
                      type="color"
                      value={newBlockColor}
                      onChange={(e) => setNewBlockColor(e.target.value)}
                    />
                    <span>{newBlockColor}</span>
                  </div>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Target Shape</label>
                  <select
                    value={newBlockShape}
                    onChange={(e) => setNewBlockShape(e.target.value as RobotHeadShape)}
                  >
                    <option value="rectangle">Rectangle</option>
                    <option value="square">Square</option>
                    <option value="circle">Circle</option>
                    <option value="hexagon">Hexagon</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Target Emotion</label>
                  <select
                    value={newBlockState}
                    onChange={(e) => setNewBlockState(e.target.value as RobotHeadState)}
                  >
                    <option value="idle">Idle</option>
                    <option value="thinking">Thinking</option>
                    <option value="searching">Searching</option>
                    <option value="listening">Listening</option>
                    <option value="speaking">Speaking</option>
                    <option value="working">Working</option>
                    <option value="happy">Happy</option>
                    <option value="error">Error</option>
                    <option value="sleeping">Sleeping</option>
                  </select>
                </div>
              </div>

              <div className="form-group checkbox-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={newBlockPoke}
                    onChange={(e) => setNewBlockPoke(e.target.checked)}
                  />
                  <span>Trigger Hop & Spin Animation (Poke)</span>
                </label>
              </div>

              {/* Block Visual Preview */}
              <div className="creator-preview">
                <span className="preview-label">Visual Block Preview:</span>
                <div
                  className="sketchware-block"
                  style={{ borderColor: newBlockColor }}
                >
                  <div className="puzzle-tab-top" style={{ backgroundColor: newBlockColor }} />
                  <div className="block-main" style={{ backgroundColor: `${newBlockColor}22` }}>
                    <span className="notch-bullet" style={{ backgroundColor: newBlockColor }} />
                    <div className="block-details">
                      <span className="block-name" style={{ color: newBlockColor }}>
                        {newBlockName || 'My Custom Block'}
                      </span>
                      <span className="block-params">
                        shape={newBlockShape} state={newBlockState} {newBlockPoke ? 'poke=true' : ''}
                      </span>
                    </div>
                  </div>
                  <div className="puzzle-tab-bottom" style={{ backgroundColor: newBlockColor }} />
                </div>
              </div>

              <button type="submit" className="save-block-btn">
                Save Block to Library
              </button>
            </form>
          </div>
        )}

        {/* Tab 3: Component Phase JSON */}
        {activeTab === 'phase-json' && (
          <div className="drawer-tab-content">
            <div className="json-editor-container">
              <div className="json-head">
                <div className="json-title-group">
                  <h3>Component Asset Schema (Phase JSON)</h3>
                  <p>Live schema representing Muno Robo component assets, shapes, and LED states.</p>
                </div>
                <div className="json-actions">
                  <button className="json-btn apply-btn" onClick={handleApplyJson}>
                    ⚡ Apply JSON to Robot
                  </button>
                  <button
                    className="json-btn copy-btn"
                    onClick={() => {
                      navigator.clipboard?.writeText(jsonText);
                      alert('Phase JSON copied to clipboard!');
                    }}
                  >
                    Copy JSON
                  </button>
                </div>
              </div>

              {jsonStatus && (
                <div className={`json-alert ${jsonStatus.isError ? 'error' : 'success'}`}>
                  {jsonStatus.message}
                </div>
              )}

              <textarea
                className="json-textarea"
                value={jsonText}
                onChange={(e) => setJsonText(e.target.value)}
                spellCheck={false}
              />
            </div>
          </div>
        )}

        {/* Tab 4: Projects & Sessions */}
        {activeTab === 'projects' && (
          <div className="drawer-tab-content">
            <div className="projects-container">
              <div className="save-project-card">
                <h3>Save Current Session</h3>
                <p>Save your customized colors, shapes, and animations for future sessions.</p>
                <div className="save-proj-input-group">
                  <input
                    type="text"
                    placeholder="Project Name (e.g. Robot Beta v2)"
                    value={projectNameInput}
                    onChange={(e) => setProjectNameInput(e.target.value)}
                  />
                  <button className="save-proj-btn" onClick={handleSaveProject}>
                    Save Project
                  </button>
                </div>
                {projectMessage && <div className="proj-message">{projectMessage}</div>}
              </div>

              <div className="saved-projects-list">
                <h3>Saved Projects & Presets</h3>
                <div className="projects-grid">
                  {projects.map((p) => (
                    <div key={p.id} className="project-card">
                      <div className="project-card-header">
                        <div className="proj-badge">
                          <span className="proj-dot" style={{ background: p.colors.color }} />
                          <span className="proj-title">{p.name}</span>
                        </div>
                        <span className="proj-date">{p.updatedAt}</span>
                      </div>
                      <div className="project-card-body">
                        <span className="proj-tag">Shape: {SHAPE_LABELS[p.shape]}</span>
                        <span className="proj-tag">Emotion: {p.state}</span>
                        <span className="proj-tag">Speed: {p.speed}×</span>
                      </div>
                      <div className="project-card-actions">
                        <button className="proj-action load-btn" onClick={() => handleLoadProject(p)}>
                          Load Setup
                        </button>
                        <button className="proj-action" onClick={() => handleRenameProject(p.id, p.name)}>
                          Rename
                        </button>
                        <button className="proj-action delete-btn" onClick={() => handleDeleteProject(p.id)}>
                          Delete
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 5: Android Studio / Sketchware Code */}
        {activeTab === 'export' && (
          <div className="drawer-tab-content">
            <div className="export-code-container">
              <h3>Android Studio & Sketchware Integration</h3>
              <p>Ready-to-use code snippets to embed Muno Robo in your Android APK or Sketchware project.</p>

              <div className="code-card">
                <div className="code-card-head">
                  <span>1. Android Studio (Kotlin WebView Setup)</span>
                </div>
                <pre className="export-pre">
                  <code>{`// In your Android Activity or Fragment:
val webView: WebView = findViewById(R.id.munoRoboWebView)
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    loadWithOverviewMode = true
    useWideViewPort = true
}
webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")
webView.loadUrl("file:///android_asset/www/index.html")`}</code>
                </pre>
              </div>

              <div className="code-card">
                <div className="code-card-head">
                  <span>2. Sketchware Component Specification (component.json)</span>
                </div>
                <pre className="export-pre">
                  <code>{`{
  "name": "MunoRoboComponent",
  "package": "com.munorobo.component",
  "icon": "ic_muno_robo",
  "events": [
    { "name": "onEmotionChanged", "code": "munoRobo.onEmotionChanged()" },
    { "name": "onHeadTapped", "code": "munoRobo.poke()" }
  ],
  "blocks": [
    { "spec": "%m.muno set shape %s.shape", "type": "action" },
    { "spec": "%m.muno set emotion %s.emotion", "type": "action" },
    { "spec": "%m.muno set colors shell:%c.shell screen:%c.screen", "type": "action" }
  ]
}`}</code>
                </pre>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
