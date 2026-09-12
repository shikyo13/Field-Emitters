import React from 'react';
import {AbsoluteFill, CanvasImage, Interactive, interpolate, staticFile, useCurrentFrame} from 'remotion';
import {loadFont as displayFont} from '@remotion/google-fonts/BarlowCondensed';
import {loadFont as monoFont} from '@remotion/google-fonts/IBMPlexMono';
import {ModelStage} from './ModelStage';

export const display = displayFont('normal', {weights: ['500', '600', '700'], subsets: ['latin']}).fontFamily;
export const mono = monoFont('normal', {weights: ['400', '500'], subsets: ['latin']}).fontFamily;

// The accent and eyebrow colors are the mod's first two field presets (0x52E5FF cyan, 0xB08CFF violet).
export const palette = {
  ink: '#eef1f4', accent: '#52e5ff', violet: '#b08cff', muted: '#b9c4d6', dim: '#8390a6', rule: '#2f3650', frame: '#4a5878',
  glow: 'radial-gradient(ellipse at 72% 48%, #1f2848 0%, #121a2c 32%, #0a0d14 70%)',
};

// Native captures from the 1.0.0 release build, 1708 × 960.
export const CAPTURE = {width: 1708, height: 960};

export const Backdrop: React.FC<{children?: React.ReactNode; section?: string; number?: string}> = ({children, section = 'TERRAIN-FOLLOWING FORCEFIELDS', number = '01'}) => <AbsoluteFill style={{background: '#0a0d14', color: palette.ink, fontFamily: display, overflow: 'hidden'}}>
  <AbsoluteFill style={{background: palette.glow}} />
  <div style={{position: 'absolute', top: 48, left: 72, right: 72, height: 1, background: palette.rule}} />
  <div style={{position: 'absolute', bottom: 48, left: 72, right: 72, height: 1, background: palette.rule}} />
  <div style={{position: 'absolute', left: 72, top: 66, fontFamily: mono, fontSize: 18, color: '#b8c3d6', letterSpacing: 3}}>FE / FIELD EMITTERS</div>
  <div style={{position: 'absolute', right: 72, top: 66, fontFamily: mono, fontSize: 18, color: palette.violet, letterSpacing: 3}}>{number} — {section}</div>
  {children}
  <div style={{position: 'absolute', left: 72, bottom: 68, fontFamily: mono, fontSize: 16, letterSpacing: 2, color: palette.dim}}>MINECRAFT 1.21.1 / NEOFORGE</div>
  <div style={{position: 'absolute', right: 180, bottom: 68, fontFamily: mono, fontSize: 14, color: palette.dim}}>FIELD EMITTERS 1.0.0</div>
  <div style={{position: 'absolute', right: 72, bottom: 68, width: 64, height: 5, background: palette.accent}} />
</AbsoluteFill>;

export const Eyebrow: React.FC<{children: React.ReactNode}> = ({children}) => <div style={{fontFamily: mono, fontSize: 22, color: palette.violet, letterSpacing: 4, marginBottom: 26}}>{children}</div>;

export const Reveal: React.FC<{children: React.ReactNode; style?: React.CSSProperties}> = ({children, style}) => {
  const frame = useCurrentFrame();
  return <Interactive.Div name="Scene copy" style={{...style, opacity: interpolate(frame, [6, 28], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}), translate: interpolate(frame, [6, 32], ['0px 20px', '0px 0px'], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'})}}>{children}</Interactive.Div>;
};

export const Capture: React.FC<{name: string; x: number; y: number; width: number; height: number; crop?: [number, number, number, number]; zoom?: boolean}> = ({name, x, y, width, height, crop = [0, 0, CAPTURE.width, CAPTURE.height], zoom = false}) => {
  const frame = useCurrentFrame();
  const scale = Math.max(width / crop[2], height / crop[3]);
  return <div style={{position: 'absolute', left: x, top: y, width, height, overflow: 'hidden', border: `1px solid ${palette.frame}`, boxShadow: '0 30px 80px #0009', background: '#0f1522'}}>
    <div style={{position: 'absolute', inset: 0, scale: zoom ? interpolate(frame, [0, 180], [1, 1.035], {extrapolateRight: 'clamp'}) : 1}}>
      <CanvasImage name={name} src={staticFile(`screenshots/${name}.png`)} width={CAPTURE.width * scale} height={CAPTURE.height * scale} style={{position: 'absolute', left: -crop[0] * scale, top: -crop[1] * scale}} />
    </div>
  </div>;
};

// The three shipped pieces side by side: a full emitter post, a rail panel and the handheld tuner.
export const Lineup: React.FC<{x: number; y: number}> = ({x, y}) => <div style={{position: 'absolute', left: x, top: y, display: 'flex', gap: 40, alignItems: 'flex-end'}}>
  {([['post', 'EMITTER', 150, 31, -0.5], ['rail', 'RAIL', 150, 100, 0.7], ['tuner', 'TUNER', 150, 82, 0.35]] as const).map(([kind, label, size, zoom, angle]) => <div key={kind} style={{width: size, textAlign: 'center'}}>
    <ModelStage kind={kind} width={size} height={165} zoom={zoom} angle={angle} turn={0.004} />
    <div style={{fontFamily: mono, fontSize: 16, color: '#a9b6cc', marginTop: 12, letterSpacing: 2}}>{label}</div>
  </div>)}
</div>;
