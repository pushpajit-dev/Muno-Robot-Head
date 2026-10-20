import React from 'react';
import { RobotHead, robotHeadStates, type RobotHeadShape, type RobotHeadState } from '../engine';
import type { Colors } from '../engine/constants';

interface EmotionsBarProps {
  shape: RobotHeadShape;
  state: RobotHeadState;
  onChange: (state: RobotHeadState) => void;
  colors: Colors;
  antenna: boolean;
  paused: boolean;
  speed: number;
}

const title = (s: string) => s[0].toUpperCase() + s.slice(1);

export const EmotionsBar: React.FC<EmotionsBarProps> = ({
  shape,
  state,
  onChange,
  colors,
  antenna,
  paused,
  speed,
}) => {
  return (
    <nav className="states-bar" aria-label="Robot Emotions and States">
      <div className="states-scroll">
        {robotHeadStates.map((s) => {
          const isSelected = s === state;
          return (
            <button
              key={s}
              className={`state-item ${isSelected ? 'selected' : ''}`}
              aria-pressed={isSelected}
              onClick={() => onChange(s)}
            >
              <span className="state-head">
                <RobotHead
                  shape={shape}
                  state={s}
                  size={64}
                  paused={paused}
                  speed={speed}
                  antenna={antenna}
                  interactive={false}
                  floorShadow={false}
                  aria-hidden
                  {...colors}
                />
              </span>
              <span className="state-name">{title(s)}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
