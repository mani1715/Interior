import React from 'react';
import { act, cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { CinematicHero } from '../home/CinematicHero';
import { STORY_SCENES, STORY_DISTANCE, activeScene, sceneMotion } from '../home/story-scenes';

vi.mock('next/image', () => ({default: ({fill: _fill, ...props}: any) => <img {...props} />}));

function setup(reduced = false, desktop = true) {
  vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue({top:76, height:3240, width:1440, left:0, right:1440, bottom:3316, x:0, y:76, toJSON:()=>({})});
  const listeners: Array<() => void> = [];
  const media = { matches: reduced, addEventListener: (_: string, cb: () => void) => listeners.push(cb), removeEventListener: vi.fn() };
  vi.stubGlobal('matchMedia', (query: string) => query.includes('reduced') ? media : {...media, matches: desktop});
  vi.stubGlobal('IntersectionObserver', class { observe() {} disconnect() {} });
  return { media, listeners };
}
afterEach(() => {cleanup(); vi.unstubAllGlobals(); vi.restoreAllMocks();});

describe('Cinematic interior journey', () => {
  it('uses the five supplied images in the approved order and opens on bedroom storage', () => {
    setup(true);
    const {container} = render(<CinematicHero />);
    expect(Array.from(container.querySelectorAll('img')).map(image => image.getAttribute('src'))).toEqual([
      '/images/approved/wardrobe.png', '/images/approved/kitchen.png', '/images/approved/hall.png',
      '/images/approved/tv-unit.png', '/images/approved/feature.png',
    ]);
    expect(screen.getByRole('heading',{level:1}).textContent).toBe('Storage designed around the way you live.');
    expect(screen.getAllByText('AI Concept Visualization')).toHaveLength(5);
  });
  it('selects every story anchor and settles on the fifth scene at the release', () => {
    STORY_SCENES.forEach((_, index) => expect(activeScene(index / STORY_DISTANCE)).toBe(index));
    expect(activeScene(1)).toBe(4);
    expect(sceneMotion(1,4).opacity).toBe(1);
    expect(sceneMotion(1,4).reveal).toBe(0);
  });
  it('keeps a phone story unpinned even when reduced motion is not requested', () => {
    setup(false, false);
    const {container} = render(<CinematicHero />);
    expect(container.querySelector('[data-cinematic]')).toBeNull();
    expect(screen.getAllByRole('heading')).toHaveLength(5);
  });
  it('keeps the five approved interior stories readable without browser animation APIs', () => {
    vi.stubGlobal('matchMedia', undefined);
    const {container} = render(<CinematicHero />);
    expect(screen.getAllByRole('heading')).toHaveLength(5);
    expect(container.querySelectorAll('h1')).toHaveLength(1);
    expect(container.querySelectorAll('[inert]')).toHaveLength(0);
    expect(screen.getByRole('link', {name:'Explore wardrobes'}).getAttribute('href')).toBe('/categories/wardrobes');
  });
  it('offers all five scenes and lazy later images on phones and reduced motion', () => {
    setup(true);
    const {container} = render(<CinematicHero />);
    expect(container.querySelector('[data-cinematic]')).toBeNull();
    expect(screen.getAllByRole('heading')).toHaveLength(5);
    expect(container.querySelectorAll('img[loading="lazy"]')).toHaveLength(4);
  });
  it('only exposes the current desktop story and restores all stories when reduced motion changes', () => {
    const {media, listeners} = setup();
    const {container} = render(<CinematicHero />);
    expect(container.querySelector('[data-cinematic]')).not.toBeNull();
    expect(container.querySelectorAll('[aria-hidden="true"][data-scene]')).toHaveLength(4);
    expect(screen.getAllByRole('heading')).toHaveLength(1);
    expect(container.querySelectorAll('img[loading="eager"]')).toHaveLength(2);
    act(() => {media.matches = true; listeners[0]();});
    expect(container.querySelector('[data-cinematic]')).toBeNull();
    expect(screen.getAllByRole('heading')).toHaveLength(5);
    expect(container.querySelector('[aria-current]')).toBeNull();
  });
  it('never leaves the stage without a room through all four transitions', () => {
    for (let step = 0; step <= 1000; step++) {
      const frames = STORY_SCENES.map((_, index) => sceneMotion(step / 1000, index));
      expect(frames.some(frame => frame.visible && frame.reveal === 0)).toBe(true);
      expect(frames.reduce((sum, frame) => sum + frame.opacity, 0)).toBeCloseTo(1, 5);
      expect(frames.filter(frame => frame.textOpacity > 0).length).toBeLessThanOrEqual(1);
    }
  });
});
