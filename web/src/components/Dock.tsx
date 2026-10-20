import React from 'react';
import { RobotHead, robotHeadShapes, type RobotHeadShape } from '../engine';
import { SHAPE_LABELS, type Colors } from '../engine/constants';

interface DockProps {
  shape: RobotHeadShape;
  onChange: (shape: RobotHeadShape) => void;
  colors: Colors;
  antenna: boolean;
  speed: number;
}

export const Dock: React.FC<DockProps> = ({ shape, onChange, colors, antenna, speed }) => {
  return (
    <nav className="dock" aria-label="Robot Shape Selector">
      <div className="dock-heading">Shape</div>
      {robotHeadShapes.map((s) => {
        const isSelected = s === shape;
        return (
          <button
            key={s}
            className={`dock-item ${isSelected ? 'selected' : ''}`}
            aria-pressed={isSelected}
            aria-label={`Shape: ${SHAPE_LABELS[s]}`}
            onClick={() => onChange(s)}
          >
            <span className="dock-icon">
              <RobotHead
                shape={s}
                size={52}
                speed={speed}
                interactive={false}
                floorShadow={false}
                antenna={antenna}
                aria-hidden
                {...colors}
              />
            </span>
            <span className="dock-tip">{SHAPE_LABELS[s]}</span>
          </button>
        );
      })}
    </nav>
  );
};
