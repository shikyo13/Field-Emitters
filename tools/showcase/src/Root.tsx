import './index.css';
import React from 'react';
import {Composition, Folder} from 'remotion';
import {Showcase} from './Showcase';
import {Intro} from './scenes/Intro';
import {Hardware} from './scenes/Hardware';
import {Perimeter} from './scenes/Perimeter';
import {Rails} from './scenes/Rails';
import {Filters} from './scenes/Filters';
import {Detection} from './scenes/Detection';
import {Finale} from './scenes/Finale';
import {ProjectIcon} from './ProjectIcon';

export const RemotionRoot: React.FC = () => <>
  <Composition id="FieldEmitters" component={Showcase} durationInFrames={900} fps={30} width={1920} height={1080} />
  <Composition id="ProjectIcon" component={ProjectIcon} durationInFrames={1} fps={30} width={1024} height={1024} />
  <Folder name="Scenes">
    <Composition id="Hero" component={Intro} durationInFrames={140} fps={30} width={1920} height={1080} />
    <Composition id="Hardware" component={Hardware} durationInFrames={150} fps={30} width={1920} height={1080} />
    <Composition id="Perimeter" component={Perimeter} durationInFrames={170} fps={30} width={1920} height={1080} />
    <Composition id="Rails" component={Rails} durationInFrames={130} fps={30} width={1920} height={1080} />
    <Composition id="Filters" component={Filters} durationInFrames={150} fps={30} width={1920} height={1080} />
    <Composition id="Detection" component={Detection} durationInFrames={132} fps={30} width={1920} height={1080} />
    <Composition id="Finale" component={Finale} durationInFrames={100} fps={30} width={1920} height={1080} />
  </Folder>
</>;
