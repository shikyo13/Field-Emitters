import React from 'react';
import {Backdrop, Eyebrow, Lineup, Reveal, palette} from '../Design';
import {ModelStage} from '../ModelStage';

export const Hardware: React.FC = () => <Backdrop section="THE HARDWARE" number="02">
  <Reveal style={{position: 'absolute', left: 104, top: 160, width: 760}}>
    <Eyebrow>FIELD TUNER</Eyebrow>
    <div style={{fontSize: 112, fontWeight: 600, lineHeight: 0.98}}>Every field.<br />In your hand.</div>
    <div style={{fontSize: 40, lineHeight: 1.3, color: '#b9c8d4', marginTop: 30}}>Open any post’s controls.<br />Reach every loaded field from anywhere.<br />Sample a mob for a filter. Cycle colors.</div>
    <div style={{marginTop: 44, height: 2, background: '#5b4f8f', width: 430}} />
    <div style={{fontSize: 32, marginTop: 20, color: palette.violet}}>Three pieces. Every emitter and rail takes Forge Energy.</div>
  </Reveal>
  <Lineup x={103} y={772} />
  <div style={{position: 'absolute', left: 940, top: 140, width: 880, height: 800}}><ModelStage kind="tuner" width={880} height={800} zoom={540} angle={-0.32} turn={0.0043} /></div>
</Backdrop>;
